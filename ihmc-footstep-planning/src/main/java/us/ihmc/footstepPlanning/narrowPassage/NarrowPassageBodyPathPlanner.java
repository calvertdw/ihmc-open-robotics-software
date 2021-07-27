package us.ihmc.footstepPlanning.narrowPassage;

import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.shape.collision.EuclidShape3DCollisionResult;
import us.ihmc.euclid.shape.collision.epa.ExpandingPolytopeAlgorithm;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.footstepPlanning.BodyPathPlanningResult;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.*;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.simulationconstructionset.util.TickAndUpdatable;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NarrowPassageBodyPathPlanner
{
   BodyPathPlanningResult bodyPathPlanningResult;

   private static int numberOfWaypoints;
   private static int maxNumberOfWaypoints = 80;
   private static int maxOrientationAdjustmentIterations = 20;
   private static int maxPoseAdjustmentIterations = 20;
   private static int yawShiftLimit = 120;
   private static int yawWiggleLimit = 25;
   private static int maxTotalIterations = 7;
   private static int currentIteration = 0;
   private static double distancePerCollisionPoint = 0.15;

   private boolean rotationDirectionDetermined = false;
   private int rotationDirection = 0;

   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());
   private final YoBoolean collisionFound = new YoBoolean("collisionFound", registry);
   private final boolean visualize;

   private PlanarRegionsList planarRegionsList;

   private FramePose3D startPose = new FramePose3D();
   private FramePose3D endPose = new FramePose3D();
   private FramePose3D[] waypoints;
   private final RecyclingArrayList<BodyCollisionPoint> bodyCollisionPoints;

   private final ExpandingPolytopeAlgorithm collisionDetector = new ExpandingPolytopeAlgorithm();
   private static final double collisionGradientScale = 0.5;
   private final List<Double> totalYawsShifted = new ArrayList<>();
   private final List<Integer> shiftSigns = new ArrayList<>();
   private final List<Vector3D> collisionGradients = new ArrayList<>();
   private final List<Vector3D> convolvedGradients = new ArrayList<>();
   private final List<Vector3D> yaws = new ArrayList<>();
   private final List<Vector3D> convolvedYaws = new ArrayList<>();
   private final TDoubleArrayList convolutionWeights = new TDoubleArrayList();
   private final TDoubleArrayList convolutionYawWeights = new TDoubleArrayList();

   // for visualization only
   private TickAndUpdatable tickAndUpdatable;
   private final BagOfBalls waypointPointGraphic;

   public NarrowPassageBodyPathPlanner(FootstepPlannerParametersReadOnly footstepPlannerParameters, YoRegistry parentRegistry)
   {
      this(footstepPlannerParameters, null, null, parentRegistry);
   }

   public NarrowPassageBodyPathPlanner(FootstepPlannerParametersReadOnly footstepPlannerParameters,
                                       TickAndUpdatable tickAndUpdatable,
                                       YoGraphicsListRegistry graphicsListRegistry,
                                       YoRegistry parentRegistry)
   {
      this.tickAndUpdatable = tickAndUpdatable;
      this.bodyCollisionPoints = new RecyclingArrayList<>(maxNumberOfWaypoints, BodyCollisionPoint.class);
      this.bodyCollisionPoints.clear();

      for (int i = 0; i < maxNumberOfWaypoints; i++)
      {
         BodyCollisionPoint bodyCollisionPoint = bodyCollisionPoints.add();
         bodyCollisionPoint.set(i, footstepPlannerParameters, graphicsListRegistry, registry);
      }

      visualize = graphicsListRegistry != null;
      if (parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }

      if (visualize)
         waypointPointGraphic = new BagOfBalls(maxNumberOfWaypoints, 0.015, YoAppearance.White(), registry, graphicsListRegistry);
      else
         waypointPointGraphic = null;
   }

   public void setPlanarRegionsList(PlanarRegionsList planarRegionsList)
   {
      this.planarRegionsList = planarRegionsList;
   }

   public List<Pose3DReadOnly> makeAdjustments()
   {
      initializeBodyPoints();
      while (currentIteration < maxTotalIterations)
      {
         adjustWaypoints();
         if (!collisionFound.getValue())
            break;
      }

      if (collisionFound.getValue())
      {
         bodyPathPlanningResult = BodyPathPlanningResult.NO_PATH_EXISTS;
      }
      else
      {
         recomputeWaypoints();
         bodyPathPlanningResult = BodyPathPlanningResult.FOUND_SOLUTION;
      }
      LogTools.info(bodyPathPlanningResult);

      List<Pose3DReadOnly> bodyPath = new ArrayList<>();
      bodyPath.add(startPose);
      bodyPath.addAll(Arrays.asList(waypoints));
      bodyPath.add(endPose);

      return bodyPath;
   }

   private void initializeBodyPoints()
   {
      Vector3D vectorToNextWaypoint = new Vector3D();
      for (int i = 0; i < numberOfWaypoints; i++)
      {
         collisionGradients.add(new Vector3D());
         convolvedGradients.add(new Vector3D());
         shiftSigns.add(0);
         yaws.add(new Vector3D());
         convolvedYaws.add(new Vector3D());
         convolutionWeights.add(exp(0.65, i));
         convolutionYawWeights.add(exp(0.5, i));
         if (i < numberOfWaypoints - 1)
            vectorToNextWaypoint.set(waypoints[i + 1].getX() - waypoints[i].getX(),
                                     waypoints[i + 1].getY() - waypoints[i].getY(),
                                     waypoints[i + 1].getZ() - waypoints[i].getZ());
         bodyCollisionPoints.get(i).initializeWaypointAdjustmentFrame(vectorToNextWaypoint);
         bodyCollisionPoints.get(i).initialize(waypoints[i]);
         totalYawsShifted.add(0.0);
      }

      if (visualize)
      {
         /* Show one collision box at a time */
         for (int boxToShow = 0; boxToShow < numberOfWaypoints; boxToShow++)
         {
            bodyCollisionPoints.forEach(cp -> cp.updateGraphics(false));
            bodyCollisionPoints.get(boxToShow).updateGraphics(true);
            tickAndUpdatable.tickAndUpdate();
         }
      }
   }

   private void adjustBodyPointPositions()
   {
      for (int i = 0; i < maxPoseAdjustmentIterations; i++)
      {
         collisionFound.set(false);
         double maxShiftDistance = 0.0;

         for (int j = 0; j < numberOfWaypoints; j++)
         {
            BodyCollisionPoint bodyCollisionPoint = bodyCollisionPoints.get(j);

            boolean collisionDetected = bodyCollisionPoint.doCollisionCheck(collisionDetector, planarRegionsList);
            if (collisionDetected)
            {
               collisionFound.set(true);

               EuclidShape3DCollisionResult collisionResult = bodyCollisionPoint.getCollisionResult();
               collisionGradients.get(j).sub(collisionResult.getPointOnB(), collisionResult.getPointOnA());
               collisionGradients.get(j).scale(collisionGradientScale);
               int shiftSign = bodyCollisionPoints.get(j).project(collisionGradients.get(j), shiftSigns.get(j));
               shiftSigns.set(j, shiftSign);
            }
            else
            {
               collisionGradients.get(j).setToZero();
               continue;
            }

            maxShiftDistance = Math.max(maxShiftDistance, collisionGradients.get(j).length());
         }

         for (int j = 0; j < numberOfWaypoints; j++)
         {
            convolvedGradients.get(j).setToZero();
            for (int k = 0; k < numberOfWaypoints; k++)
            {
               int indexDifference = Math.abs(j - k);
               double scale = convolutionWeights.get(indexDifference);
               scaleAdd(convolvedGradients.get(j), scale, collisionGradients.get(k));
            }

            bodyCollisionPoints.get(j).project(convolvedGradients.get(j));
            bodyCollisionPoints.get(j).shiftWaypoint(convolvedGradients.get(j));

            if (visualize)
            {
               bodyCollisionPoints.get(j).updateGraphics(bodyCollisionPoints.get(j).getCollisionResult().areShapesColliding());
            }
         }

         if (visualize)
            tickAndUpdatable.tickAndUpdate();

         double shiftDistanceEpsilon = 0.002;
         if (!collisionFound.getValue() || maxShiftDistance < shiftDistanceEpsilon)
         {
            break;
         }
      }
   }

   private void adjustBodyPointOrientations()
   {
      for (int i = 0; i < maxOrientationAdjustmentIterations; i++)
      {
         collisionFound.set(false);
         for (int j = 0; j < numberOfWaypoints; j++)
         {
            BodyCollisionPoint bodyCollisionPoint = bodyCollisionPoints.get(j);

            boolean collisionDetected = bodyCollisionPoint.doCollisionCheck(collisionDetector, planarRegionsList);
            if (collisionDetected)
            {
               collisionFound.set(true);

               EuclidShape3DCollisionResult collisionResult = bodyCollisionPoint.getCollisionResult();
               Vector3D collisionVector = new Vector3D();
               collisionVector.sub(collisionResult.getPointOnB(), collisionResult.getPointOnA());

               // Rotation direction initially determined by first collision vector
               if (!rotationDirectionDetermined)
               {
                  rotationDirection = bodyCollisionPoint.getRotationDirection(collisionVector);
                  rotationDirectionDetermined = true;
               }
               yaws.set(j, collisionVector);
            }
            else
            {
               yaws.get(j).setToZero();
            }

            if (visualize)
               bodyCollisionPoints.get(j).updateGraphics(bodyCollisionPoints.get(j).getCollisionResult().areShapesColliding());
         }

         if (!collisionFound.getValue())
         {
            if (visualize) tickAndUpdatable.tickAndUpdate();
            break;
         }

         // Smooth out orientations along path (convolve yaws)
         for (int j = 0; j < numberOfWaypoints; j++)
         {
            convolvedYaws.get(j).setToZero();
            for (int k = 0; k < numberOfWaypoints; k++)
            {
               int indexDifference = Math.abs(j - k);
               double scale = convolutionYawWeights.get(indexDifference);
               scaleAdd(convolvedYaws.get(j), scale, yaws.get(k));
            }

            double totalYawShifted = totalYawsShifted.get(j);
            double yawShift = bodyCollisionPoints.get(j).getYawShift(convolvedYaws.get(j));
            double limit = currentIteration < 1 ? yawShiftLimit : yawWiggleLimit;
            if (Math.abs(totalYawShifted + (rotationDirection * yawShift)) < limit)
            {
               bodyCollisionPoints.get(j).adjustOrientation(rotationDirection, yawShift);
               double previousTotalYawShifted = totalYawsShifted.get(j);
               totalYawsShifted.set(j, previousTotalYawShifted + (rotationDirection * yawShift));
            }

            if (visualize)
               bodyCollisionPoints.get(j).updateGraphics(bodyCollisionPoints.get(j).getCollisionResult().areShapesColliding());
         }

         if (visualize)
            tickAndUpdatable.tickAndUpdate();
      }
   }

   private void adjustWaypoints()
   {
      if (currentIteration > 0)
         rotationDirection = (rotationDirection < 0) ? 1 : -1;
      resetShiftAlphas();
      adjustBodyPointPositions();
      if (collisionFound.getValue())
      {
         resetYawsShifted();
         adjustBodyPointOrientations();
      }
      currentIteration++;
   }

   private void resetYawsShifted()
   {
      for (int i = 0; i < numberOfWaypoints; i++)
      {
         totalYawsShifted.set(i, 0.0);
      }
   }

   private void resetShiftAlphas()
   {
      for (int i = 0; i < numberOfWaypoints; i++)
      {
         bodyCollisionPoints.get(i).resetShiftAlpha();
      }
   }

   private void recomputeWaypoints()
   {
      for (int i = 0; i < numberOfWaypoints; i++)
      {
         waypoints[i].set(bodyCollisionPoints.get(i).getOptimizedWaypoint());
         waypoints[i].getOrientation().set(bodyCollisionPoints.get(i).getOptimizedWaypoint().getOrientation());
         if (visualize)
            bodyCollisionPoints.get(i).updateGraphics(true);
      }
      updateWaypointPointGraphics();
      if (visualize)
      {
         bodyCollisionPoints.forEach(cp -> cp.updateGraphics(false));
         tickAndUpdatable.tickAndUpdate();
      }
   }

   private void updateWaypointPointGraphics()
   {
      if (!visualize)
      {
         return;
      }

      waypointPointGraphic.reset();
      for (int i = 0; i < waypoints.length; i++)
      {
         waypointPointGraphic.setBall(waypoints[i].getPosition());
      }

      tickAndUpdatable.tickAndUpdate();
   }

   public void setWaypointsFromStartAndEndPoses(Pose3DReadOnly startPose, Pose3DReadOnly endPose)
   {
      this.startPose.set(startPose);
      this.endPose.set(endPose);
      numberOfWaypoints = (int) (startPose.getPosition().distance(endPose.getPosition()) / distancePerCollisionPoint);
      waypoints = new FramePose3D[numberOfWaypoints];

      if (numberOfWaypoints > maxNumberOfWaypoints)
      {
         LogTools.error("Not enough space in recycling array for waypoints, increase maxNumberOfWaypoints.");
      }

      for (int i = 0; i < numberOfWaypoints; i++)
      {
         waypoints[i] = new FramePose3D();
         double alpha = (i + 1) / (double) (numberOfWaypoints + 1);
         waypoints[i].interpolate(this.startPose, this.endPose, alpha);
      }

      resetAdjuster();
      updateWaypointPointGraphics();
   }

   public void setWaypoints(List<FramePose3D> waypoints)
   {
      this.startPose.set(waypoints.get(0));
      this.endPose.set(waypoints.get(waypoints.size() - 1));

      // Add extra waypoints based on path length
      int numOfWaypoints = waypoints.size();
      List<FramePose3D> increasedWaypoints = new ArrayList<>();

      for (int i = 0; i < numOfWaypoints - 1; i++)
      {
         increasedWaypoints.add(waypoints.get(i));
         int numOfExtraWaypoints = (int) (waypoints.get(i).getPosition().distance(waypoints.get(i + 1).getPosition()) / distancePerCollisionPoint);

         for (int j = 0; j < numOfExtraWaypoints; j++)
         {
            double alpha = (j + 1) / (double) (numOfExtraWaypoints + 1);
            FramePose3D interpolatedWaypoint = new FramePose3D();
            interpolatedWaypoint.interpolate(waypoints.get(i), waypoints.get(i + 1), alpha);
            increasedWaypoints.add(interpolatedWaypoint);
         }
      }

      FramePose3D[] waypointsArray = new FramePose3D[increasedWaypoints.size()];
      increasedWaypoints.toArray(waypointsArray);

      if (waypointsArray.length > maxNumberOfWaypoints)
      {
         LogTools.error("Not enough space in recycling array for waypoints, increase maxNumberOfWaypoints.");
      }
      this.waypoints = waypointsArray;
      this.numberOfWaypoints = this.waypoints.length;

      resetAdjuster();
      updateWaypointPointGraphics();
   }

   private void resetAdjuster()
   {
      currentIteration = 0;
      totalYawsShifted.clear();
      shiftSigns.clear();
      collisionGradients.clear();
      convolvedGradients.clear();
      yaws.clear();
      convolvedYaws.clear();
      convolutionWeights.clear();
      convolutionYawWeights.clear();
   }

   public BodyPathPlanningResult getBodyPathPlanningResult()
   {
      return bodyPathPlanningResult;
   }

   /*
    * Different from the Vector3DBasics.scaleAdd, which scales the mutated vector
    * a = a + alpha * b
    */
   static void scaleAdd(Vector3DBasics vectorA, double alpha, Vector3DReadOnly vectorB)
   {
      vectorA.addX(alpha * vectorB.getX());
      vectorA.addY(alpha * vectorB.getY());
      vectorA.addZ(alpha * vectorB.getZ());
   }

   /* exponent function assuming non-negative positive exponent */
   static double exp(double base, int exponent)
   {
      double value = 1.0;
      int i = 0;

      while (i < exponent)
      {
         value *= base;
         i++;
      }

      return value;
   }
}
