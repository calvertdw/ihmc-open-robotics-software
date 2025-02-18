package us.ihmc.footstepPlanning.graphSearch.stepChecking;

import us.ihmc.commonWalkingControlModules.staticReachability.StepReachabilityData;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.footstepPlanning.graphSearch.collision.FootstepPlannerBodyCollisionDetector;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepSnapData;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepSnapDataReadOnly;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepSnapperReadOnly;
import us.ihmc.footstepPlanning.graphSearch.graph.DiscreteFootstep;
import us.ihmc.footstepPlanning.graphSearch.graph.DiscreteFootstepTools;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.BipedalFootstepPlannerNodeRejectionReason;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.heightMap.HeightMapData;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoInteger;

import java.util.ArrayList;
import java.util.List;

import static us.ihmc.footstepPlanning.graphSearch.graph.visualization.BipedalFootstepPlannerNodeRejectionReason.HEIGHT_MAP_NONTRAVERSABLE;

public class HeightMapFootstepChecker implements FootstepCheckerInterface
{
   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());
   public static final String rejectionReasonVariable = "rejectionReason";

   private static final double traversabilityThresholdCenter = 0.08;
   private static final double traversabilityThresholdPerimeter = 0.02;

   private final FootstepPlannerParametersReadOnly parameters;
   final FootstepSnapperReadOnly snapper;
   private final SideDependentList<ConvexPolygon2D> footPolygons;
   private final ConvexPolygon2D tmpFootPolygon = new ConvexPolygon2D();

   private final ObstacleBetweenStepsChecker obstacleBetweenStepsChecker;
   private final FootstepPlannerBodyCollisionDetector collisionDetector;
   private final FootstepPoseHeuristicChecker heuristicPoseChecker;
   private final FootstepPoseReachabilityChecker reachabilityChecker;

   private HeightMapData heightMapData = null;

   private boolean assumeFlatGround = false;

   private final FootstepSnapData candidateStepSnapData = FootstepSnapData.identityData();
   private final YoEnum<BipedalFootstepPlannerNodeRejectionReason> rejectionReason = new YoEnum<>(rejectionReasonVariable, "", registry, BipedalFootstepPlannerNodeRejectionReason.class, true);
   private final YoDouble footAreaPercentage = new YoDouble("footAreaPercentage", registry);
   private final YoInteger footstepIndex = new YoInteger("footstepIndex", registry);
   private final YoDouble achievedDeltaInside = new YoDouble("achievedDeltaInside", registry);
   private final YoDouble rmsError = new YoDouble("rmsError", registry);

   private final List<CustomFootstepChecker> customFootstepCheckers = new ArrayList<>();

   public HeightMapFootstepChecker(FootstepPlannerParametersReadOnly parameters,
                                   SideDependentList<ConvexPolygon2D> footPolygons,
                                   FootstepSnapperReadOnly snapper,
                                   StepReachabilityData stepReachabilityData,
                                   YoRegistry parentRegistry)
   {
      this.parameters = parameters;
      this.snapper = snapper;
      this.footPolygons = footPolygons;
      this.obstacleBetweenStepsChecker = new ObstacleBetweenStepsChecker(parameters, snapper);
      this.collisionDetector = new FootstepPlannerBodyCollisionDetector(parameters);
      this.heuristicPoseChecker = new FootstepPoseHeuristicChecker(parameters, snapper, registry);
      this.reachabilityChecker = new FootstepPoseReachabilityChecker(parameters, snapper, stepReachabilityData, registry);
      parentRegistry.addChild(registry);
   }

   @Override
   public boolean isStepValid(DiscreteFootstep candidateStep, DiscreteFootstep stanceStep, DiscreteFootstep startOfSwing)
   {
      if (stanceStep != null && candidateStep.getRobotSide() == stanceStep.getRobotSide())
      {
         throw new RuntimeException(getClass().getSimpleName() + " stance and next steps have the same side");
      }
      if (startOfSwing != null && candidateStep.getRobotSide() != startOfSwing.getRobotSide())
      {
         throw new RuntimeException(getClass().getSimpleName() + " start of swing and touchdown steps have different sides");
      }

      clearLoggedVariables();
      doValidityCheck(candidateStep, stanceStep, startOfSwing);

      return rejectionReason.getValue() == null;
   }

   // TODO compute step index

   private void doValidityCheck(DiscreteFootstep candidateStep, DiscreteFootstep stanceStep, DiscreteFootstep startOfSwing)
   {
      // Do pre-snap validity checks here -- any validity checks before snapping will have big speedups

      double centerOfFootX = candidateStep.getX();
      double centerOfFootY = candidateStep.getY();
      if (queryHeightMapTraversabilityDistanceMargin(centerOfFootX, centerOfFootY) < traversabilityThresholdCenter)
      {
         rejectionReason.set(HEIGHT_MAP_NONTRAVERSABLE);
         return;
      }

      double averageSignedDistanceFunction = 0.0;
      DiscreteFootstepTools.getFootPolygon(candidateStep, footPolygons.get(candidateStep.getRobotSide()), tmpFootPolygon);
      for (int i = 0; i < tmpFootPolygon.getNumberOfVertices(); i++)
      {
         averageSignedDistanceFunction += queryHeightMapTraversabilityDistanceMargin(tmpFootPolygon.getVertex(i).getX(), tmpFootPolygon.getVertex(i).getY());
      }

      averageSignedDistanceFunction /= tmpFootPolygon.getNumberOfVertices();

      if (averageSignedDistanceFunction < traversabilityThresholdPerimeter)
      {
         rejectionReason.set(HEIGHT_MAP_NONTRAVERSABLE);
         return;
      }


      FootstepSnapDataReadOnly snapData = snapper.snapFootstep(candidateStep, stanceStep, parameters.getWiggleWhilePlanning());
      candidateStepSnapData.set(snapData);
      heuristicPoseChecker.setApproximateStepDimensions(candidateStep, stanceStep);
      achievedDeltaInside.set(snapData.getAchievedInsideDelta());

      if (!doValidityCheckForHeightMap(candidateStep))
         return;

      // Check step placement
      if (!assumeFlatGround && !isStepPlacementValid(candidateStep, snapData))
      {
         return;
      }

      // Check snapped footstep placement
      BipedalFootstepPlannerNodeRejectionReason poseRejectionReason;
      if (parameters.getUseStepReachabilityMap())
      {
         poseRejectionReason = reachabilityChecker.checkStepValidity(candidateStep, stanceStep);
      }
      else
      {
         poseRejectionReason = heuristicPoseChecker.snapAndCheckValidity(candidateStep, stanceStep, startOfSwing);
      }
      if (poseRejectionReason != null)
      {
         rejectionReason.set(poseRejectionReason);
         return;
      }

      // Programmatically added custom checks
      for (CustomFootstepChecker customFootstepChecker : customFootstepCheckers)
      {
         if (!customFootstepChecker.isStepValid(candidateStep, stanceStep))
         {
            rejectionReason.set(customFootstepChecker.getRejectionReason());
            return;
         }
      }

      // Check collisions
      isCollisionFree(candidateStep, stanceStep, startOfSwing);
   }

   private boolean doValidityCheckForHeightMap(DiscreteFootstep candidateStep)
   {
      if (heightMapData == null || heightMapData.isEmpty())
         return true;

      // Area
      double fullFootArea = footPolygons.get(candidateStep.getRobotSide()).getArea();
      double area = candidateStepSnapData.getCroppedFoothold().getArea();
      if (Double.isFinite(area))
         footAreaPercentage.set(candidateStepSnapData.getCroppedFoothold().getArea() / fullFootArea);
      else
         footAreaPercentage.set(1.0);

      double epsilonAreaPercentage = 1e-4;
      if (footAreaPercentage.getValue() < (parameters.getMinimumFootholdPercent() - epsilonAreaPercentage))
      {
         rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.NOT_ENOUGH_AREA);
         return false;
      }

      // Root-mean-squared error
      rmsError.set(candidateStepSnapData.getSnapRMSError());
      if (candidateStepSnapData.getSnapRMSError() > parameters.getRMSErrorThreshold())
      {
         rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.RMS_ERROR_TOO_HIGH);
         return false;
      }

      return true;
   }

   private boolean isStepPlacementValid(DiscreteFootstep candidateStep, FootstepSnapDataReadOnly snapData)
   {
      // Check valid snap
      if (candidateStepSnapData.getSnapTransform().containsNaN())
      {
         rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.COULD_NOT_SNAP);
         return false;
      }

      // Check wiggle parameters satisfied
      if (parameters.getWiggleWhilePlanning())
      {
         checkWiggleParameters(parameters);
         if (snapData.getAchievedInsideDelta() < parameters.getWiggleInsideDeltaMinimum())
         {
            rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.WIGGLE_CONSTRAINT_NOT_MET);
            return false;
         }
      }

      // Check incline
      RigidBodyTransform snappedSoleTransform = candidateStepSnapData.getSnappedStepTransform(candidateStep);
      double minimumSurfaceNormalZ = Math.cos(parameters.getMinimumSurfaceInclineRadians());
      if (snappedSoleTransform.getM22() < minimumSurfaceNormalZ)
      {
         rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.SURFACE_NORMAL_TOO_STEEP_TO_SNAP);
         return false;
      }

      // Check snap area
      ConvexPolygon2D footholdAfterSnap = candidateStepSnapData.getCroppedFoothold();
      double croppedFootArea = footholdAfterSnap.getArea();
      double fullFootArea = footPolygons.get(candidateStep.getRobotSide()).getArea();
      footAreaPercentage.set(croppedFootArea / fullFootArea);

      double epsilonAreaPercentage = 1e-4;
      if (!footholdAfterSnap.isEmpty() && footAreaPercentage.getValue() < (parameters.getMinimumFootholdPercent() - epsilonAreaPercentage))
      {
         rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.NOT_ENOUGH_AREA);
         return false;
      }

      return true;
   }

   private boolean isCollisionFree(DiscreteFootstep candidateStep, DiscreteFootstep stanceStep, DiscreteFootstep startOfSwing)
   {
      if (stanceStep == null)
      {
         return true;
      }


      // Check for obstacle collisions (vertically extruded line between steps)
      if (parameters.checkForPathCollisions())
      {
         try
         {
            if (!obstacleBetweenStepsChecker.isFootstepValid(candidateStep, stanceStep))
            {
               rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.OBSTACLE_BLOCKING_BODY);
               return false;
            }
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }

      // Check for bounding box collisions
      if (parameters.checkForBodyBoxCollisions())
      {
         if (boundingBoxCollisionDetected(candidateStep, stanceStep))
         {
            rejectionReason.set(BipedalFootstepPlannerNodeRejectionReason.OBSTACLE_HITTING_BODY);
            return false;
         }
      }

      return true;
   }

   private boolean boundingBoxCollisionDetected(DiscreteFootstep candidateStep, DiscreteFootstep stanceStep)
   {
      FootstepSnapDataReadOnly stanceStepSnapData = snapper.snapFootstep(stanceStep, null, parameters.getWiggleWhilePlanning());
      if (stanceStepSnapData == null)
      {
         return false;
      }

      double candidateStepHeight = DiscreteFootstepTools.getSnappedStepHeight(candidateStep, candidateStepSnapData.getSnapTransform());
      double stanceStepHeight = DiscreteFootstepTools.getSnappedStepHeight(stanceStep, stanceStepSnapData.getSnapTransform());
      boolean collisionDetected = collisionDetector.checkForCollision(candidateStep,
                                                                      stanceStep,
                                                                      candidateStepHeight,
                                                                      stanceStepHeight,
                                                                      parameters.getIntermediateBodyBoxChecks());
      return collisionDetected;
   }

   private void clearLoggedVariables()
   {
      footAreaPercentage.setToNaN();
      rejectionReason.set(null);
      footstepIndex.set(-1);
      achievedDeltaInside.setToNaN();

      candidateStepSnapData.clear();
      heuristicPoseChecker.clearLoggedVariables();
   }

   public void setAssumeFlatGround(boolean assumeFlatGround)
   {
      this.assumeFlatGround = assumeFlatGround;
   }

   public void setHeightMapData(HeightMapData heightMapData)
   {
      this.heightMapData = heightMapData;
      collisionDetector.setHeightMapData(heightMapData);
      obstacleBetweenStepsChecker.setHeightMapData(heightMapData);
   }

   private static void checkWiggleParameters(FootstepPlannerParametersReadOnly parameters)
   {
      double epsilon = 1e-7;
      if (parameters.getWiggleInsideDeltaMinimum() > parameters.getWiggleInsideDeltaTarget() + epsilon)
      {
         throw new RuntimeException(
               "Illegal wiggle parameters, target should be greater or equal to minimum. Target: " + parameters.getWiggleInsideDeltaTarget() + ", Minimum: "
               + parameters.getWiggleInsideDeltaMinimum());
      }
   }

   public void clearCustomFootstepCheckers()
   {
      customFootstepCheckers.clear();
   }

   public void attachCustomFootstepChecker(CustomFootstepChecker customFootstepChecker)
   {
      customFootstepCheckers.add(customFootstepChecker);
   }

   public BipedalFootstepPlannerNodeRejectionReason getRejectionReason()
   {
      return rejectionReason.getValue();
   }

   private double queryHeightMapTraversabilityDistanceMargin(double x, double y)
   {
      // TODO
      return Double.POSITIVE_INFINITY;
   }

   public static void main(String[] args)
   {

   }
}
