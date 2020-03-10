package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import us.ihmc.commonWalkingControlModules.configurations.SteppingParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.Plane3D;
import us.ihmc.euclid.referenceFrame.FrameConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.robotics.math.YoCounter;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.*;

public class SwingOverPlanarRegionsTrajectoryExpander
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final double[] swingWaypointProportions;

   private final TwoWaypointSwingGenerator twoWaypointSwingGenerator;
   private final ConvexPolygon2D footPolygonShape;
   private final FrameConvexPolygon2D swingStartPolygon = new FrameConvexPolygon2D();
   private final FrameConvexPolygon2D swingEndPolygon = new FrameConvexPolygon2D();

   private final YoBoolean doInitialFastApproximation;
   private final YoInteger numberOfCheckpoints;
   private final YoCounter numberOfTriesCounter;
   private final YoDouble minimumClearance;
   private final YoDouble fastApproximationLessClearance;
   private final YoDouble minimumAdjustmentIncrementDistance;
   private final YoDouble maximumAdjustmentIncrementDistance;
   private final YoDouble adjustmentIncrementDistanceGain;
   private final YoDouble maximumAdjustmentDistance;
   private final YoDouble minimumHeightAboveFloorForCollision;
   private final YoEnum<SwingOverPlanarRegionsCollisionType> mostSevereCollisionType;
   private final YoEnum<SwingOverPlanarRegionsStatus> status;

   private final YoBoolean wereWaypointsAdjusted;
   private final YoFramePoint3D trajectoryPosition;
   private final PoseReferenceFrame solePoseReferenceFrame = new PoseReferenceFrame("desiredPositionFrame", worldFrame);
   private final PoseReferenceFrame startOfSwingReferenceFrame = new PoseReferenceFrame("startOfSwingFrame", worldFrame);
   private final PoseReferenceFrame endOfSwingReferenceFrame = new PoseReferenceFrame("endOfSwingFrame", worldFrame);
   private final PoseReferenceFrame midFootReferenceFrame = new PoseReferenceFrame("midFootReferenceFrame", worldFrame);

   private final RecyclingArrayList<FramePoint3D> originalWaypoints;
   private final RecyclingArrayList<FramePoint3D> adjustedWaypoints;
   private final double minimumSwingHeight;
   private final double maximumSwingHeight;
   private final double collisionSphereRadius;
   private final double toeLength;
   private final double heelLength;

   private final Map<SwingOverPlanarRegionsCollisionType, FramePoint3D> closestPolygonPointMap;
   private final FramePoint3D midGroundPoint;
   private final FrameVector3D waypointAdjustment;
   private final Plane3D swingTrajectoryPlane;
   private final Plane3D swingFloorPlane;
   private final AxisAngle axisAngle;
   private final RigidBodyTransform rigidBodyTransform;

   private final Vector3D tempPlaneNormal = new Vector3D();

   // Boilerplate variables
   private final FrameVector3D initialVelocity;
   private final FrameVector3D touchdownVelocity;
   private final FramePoint3D swingStartPosition;
   private final FramePoint3D swingEndPosition;
   private final FramePoint3D stanceFootPosition;
   private final FramePoint3D collisionRelativeToStart;
   private final FramePoint3D stepRelativeToStart;

   // Visualization
   private Optional<Runnable> visualizer;

   public enum SwingOverPlanarRegionsCollisionType
   {
      NO_INTERSECTION, TOO_CLOSE_TO_IGNORE_PLANE, COLLISION_ABOVE_FOOT, OUTSIDE_TRAJECTORY, COLLISION_INSIDE_TRAJECTORY, COLLISION_BETWEEN_FEET
   }

   public enum SwingOverPlanarRegionsStatus
   {
      INITIALIZED, FAILURE_HIT_MAX_ADJUSTMENT_DISTANCE, SEARCHING_FOR_SOLUTION, SOLUTION_FOUND,
   }

   public SwingOverPlanarRegionsTrajectoryExpander(WalkingControllerParameters walkingControllerParameters,
                                                   YoVariableRegistry parentRegistry,
                                                   YoGraphicsListRegistry graphicsListRegistry)
   {
      String namePrefix = "trajectoryExpander";
      SteppingParameters steppingParameters = walkingControllerParameters.getSteppingParameters();
      twoWaypointSwingGenerator = new TwoWaypointSwingGenerator(namePrefix,
                                                                steppingParameters.getMinSwingHeightFromStanceFoot(),
                                                                steppingParameters.getMaxSwingHeightFromStanceFoot(),
                                                                steppingParameters.getMinSwingHeightFromStanceFoot(),
                                                                parentRegistry,
                                                                graphicsListRegistry);
      minimumSwingHeight = steppingParameters.getMinSwingHeightFromStanceFoot();
      maximumSwingHeight = steppingParameters.getMaxSwingHeightFromStanceFoot();
      toeLength = steppingParameters.getFootForwardOffset();
      heelLength = steppingParameters.getFootBackwardOffset();
      collisionSphereRadius = Math.max(toeLength, heelLength);

      swingWaypointProportions = walkingControllerParameters.getSwingTrajectoryParameters().getSwingWaypointProportions();

      footPolygonShape = new ConvexPolygon2D();
      footPolygonShape.addVertex(steppingParameters.getFootForwardOffset(), 0.5 * steppingParameters.getToeWidth());
      footPolygonShape.addVertex(steppingParameters.getFootForwardOffset(), -0.5 * steppingParameters.getToeWidth());
      footPolygonShape.addVertex(-steppingParameters.getFootBackwardOffset(), 0.5 * steppingParameters.getFootWidth());
      footPolygonShape.addVertex(-steppingParameters.getFootBackwardOffset(), -0.5 * steppingParameters.getFootWidth());
      footPolygonShape.update();

      doInitialFastApproximation = new YoBoolean(namePrefix + "DoInitialFastApproximation", parentRegistry);
      numberOfCheckpoints = new YoInteger(namePrefix + "NumberOfCheckpoints", parentRegistry);
      numberOfTriesCounter = new YoCounter(namePrefix + "NumberOfTriesCounter", parentRegistry);
      minimumClearance = new YoDouble(namePrefix + "MinimumClearance", parentRegistry);
      fastApproximationLessClearance = new YoDouble(namePrefix + "FastApproximationLessClearance", parentRegistry);
      minimumHeightAboveFloorForCollision = new YoDouble(namePrefix + "MinimumHeightAboveFloorForCollision", parentRegistry);
      minimumAdjustmentIncrementDistance = new YoDouble(namePrefix + "MinimumAdjustmentIncrementDistance", parentRegistry);
      maximumAdjustmentIncrementDistance = new YoDouble(namePrefix + "MaximumAdjustmentIncrementDistance", parentRegistry);
      adjustmentIncrementDistanceGain = new YoDouble(namePrefix + "AdjustmentIncrementDistanceGain", parentRegistry);
      maximumAdjustmentDistance = new YoDouble(namePrefix + "MaximumAdjustmentDistance", parentRegistry);
      wereWaypointsAdjusted = new YoBoolean(namePrefix + "WereWaypointsAdjusted", parentRegistry);
      status = new YoEnum<>(namePrefix + "Status", parentRegistry, SwingOverPlanarRegionsStatus.class);
      mostSevereCollisionType = new YoEnum<>(namePrefix + "CollisionType", parentRegistry, SwingOverPlanarRegionsCollisionType.class);

      trajectoryPosition = new YoFramePoint3D(namePrefix + "TrajectoryPosition", worldFrame, parentRegistry);
      originalWaypoints = new RecyclingArrayList<>(2, FramePoint3D.class);
      originalWaypoints.add();
      originalWaypoints.add();
      adjustedWaypoints = new RecyclingArrayList<>(2, FramePoint3D.class);
      adjustedWaypoints.add();
      adjustedWaypoints.add();

      closestPolygonPointMap = new HashMap<>();
      for (SwingOverPlanarRegionsCollisionType swingOverPlanarRegionsTrajectoryCollisionType : SwingOverPlanarRegionsCollisionType.values())
      {
         closestPolygonPointMap.put(swingOverPlanarRegionsTrajectoryCollisionType, new FramePoint3D());
      }
      midGroundPoint = new FramePoint3D();
      waypointAdjustment = new FrameVector3D();
      swingTrajectoryPlane = new Plane3D();
      swingFloorPlane = new Plane3D();
      axisAngle = new AxisAngle();
      rigidBodyTransform = new RigidBodyTransform();

      initialVelocity = new FrameVector3D();
      touchdownVelocity = new FrameVector3D();
      touchdownVelocity.setZ(walkingControllerParameters.getSwingTrajectoryParameters().getDesiredTouchdownVelocity());
      swingStartPosition = new FramePoint3D();
      swingEndPosition = new FramePoint3D();
      stanceFootPosition = new FramePoint3D();
      collisionRelativeToStart = new FramePoint3D();
      stepRelativeToStart = new FramePoint3D();

      visualizer = Optional.empty();

      // Set default values
      doInitialFastApproximation.set(true);
      fastApproximationLessClearance.set(0.05);
      minimumHeightAboveFloorForCollision.set(0.02);
      numberOfCheckpoints.set(100);
      numberOfTriesCounter.setMaxCount(50);
      minimumClearance.set(0.04);
      minimumAdjustmentIncrementDistance.set(0.03);
      maximumAdjustmentIncrementDistance.set(0.15);
      adjustmentIncrementDistanceGain.set(0.95);
      maximumAdjustmentDistance.set(maximumSwingHeight - minimumSwingHeight);
   }

   public void setDoInitialFastApproximation(boolean doInitialFastApproximation)
   {
      this.doInitialFastApproximation.set(doInitialFastApproximation);
   }

   public void setFastApproximationLessClearance(double fastApproximationLessClearance)
   {
      this.fastApproximationLessClearance.set(fastApproximationLessClearance);
   }

   public void setNumberOfCheckpoints(int numberOfCheckpoints)
   {
      this.numberOfCheckpoints.set(numberOfCheckpoints);
   }

   public void setMaximumNumberOfTries(int maximumNumberOfTries)
   {
      this.numberOfTriesCounter.setMaxCount(maximumNumberOfTries);
   }

   public void setMinimumSwingFootClearance(double minimumSwingFootClearance)
   {
      minimumClearance.set(minimumSwingFootClearance);
   }

   public void setMinimumAdjustmentIncrementDistance(double minimumAdjustmentIncrementDistance)
   {
      this.minimumAdjustmentIncrementDistance.set(minimumAdjustmentIncrementDistance);
   }

   public void setMaximumAdjustmentIncrementDistance(double maximumAdjustmentIncrementDistance)
   {
      this.maximumAdjustmentIncrementDistance.set(maximumAdjustmentIncrementDistance);
   }

   public void setAdjustmentIncrementDistanceGain(double adjustmentIncrementDistanceGain)
   {
      this.adjustmentIncrementDistanceGain.set(adjustmentIncrementDistanceGain);
   }

   public void setMaximumAdjustmentDistance(double maximumAdjustmentDistance)
   {
      this.maximumAdjustmentDistance.set(maximumAdjustmentDistance);
   }

   public void setMinimumHeightAboveFloorForCollision(double heightAboveFloorForCollision)
   {
      this.minimumHeightAboveFloorForCollision.set(heightAboveFloorForCollision);
   }

   private final RigidBodyTransform transformToStart = new RigidBodyTransform();
   private final RigidBodyTransform transformFromStart = new RigidBodyTransform();
   private final RigidBodyTransform transformToEnd = new RigidBodyTransform();
   private final RigidBodyTransform transformFromEnd = new RigidBodyTransform();

   public double expandTrajectoryOverPlanarRegions(FramePose3DReadOnly stanceFootPose,
                                                   FramePose3DReadOnly swingStartPose,
                                                   FramePose3DReadOnly swingEndPose,
                                                   PlanarRegionsList planarRegionsList)
   {
      stanceFootPosition.setMatchingFrame(stanceFootPose.getPosition());
      twoWaypointSwingGenerator.setStanceFootPosition(stanceFootPosition);

      swingStartPosition.setMatchingFrame(swingStartPose.getPosition());
      twoWaypointSwingGenerator.setInitialConditions(swingStartPosition, initialVelocity);

      swingEndPosition.setMatchingFrame(swingEndPose.getPosition());
      twoWaypointSwingGenerator.setFinalConditions(swingEndPosition, touchdownVelocity);
      twoWaypointSwingGenerator.setStepTime(1.0);

      swingStartPose.get(transformToStart);
      swingEndPose.get(transformToEnd);
      transformToStart.inverseTransform(transformFromStart);
      transformToEnd.inverseTransform(transformFromEnd);

      initializeSwingWaypoints();

      adjustSwingEndIfCoincidentWithSwingStart();

      startOfSwingReferenceFrame.setPoseAndUpdate(swingStartPose);
      endOfSwingReferenceFrame.setPoseAndUpdate(swingEndPose);
      stepRelativeToStart.setIncludingFrame(swingEndPosition);
      stepRelativeToStart.changeFrame(startOfSwingReferenceFrame);

      swingStartPolygon.setIncludingFrame(startOfSwingReferenceFrame, footPolygonShape);
      swingEndPolygon.setIncludingFrame(endOfSwingReferenceFrame, footPolygonShape);
      swingStartPolygon.changeFrameAndProjectToXYPlane(worldFrame);
      swingEndPolygon.changeFrameAndProjectToXYPlane(worldFrame);

      midGroundPoint.interpolate(swingStartPosition, swingEndPosition, 0.5);
      swingTrajectoryPlane.set(swingStartPosition, adjustedWaypoints.get(0), swingEndPosition);

      axisAngle.set(swingTrajectoryPlane.getNormal(), Math.PI / 2.0);
      midFootReferenceFrame.setPoseAndUpdate(midGroundPoint, axisAngle);

      rigidBodyTransform.setRotation(axisAngle);
      tempPlaneNormal.sub(swingStartPosition, swingEndPosition);
      rigidBodyTransform.transform(tempPlaneNormal);
      tempPlaneNormal.normalize();
      swingFloorPlane.set(swingStartPosition, tempPlaneNormal);

      wereWaypointsAdjusted.set(false);

      double filterDistance = maximumSwingHeight + collisionSphereRadius + 2.0 * minimumClearance.getDoubleValue();
      List<PlanarRegion> filteredRegions = PlanarRegionTools.filterPlanarRegionsWithBoundingCapsule(swingStartPosition,
                                                                                                    swingEndPosition,
                                                                                                    filterDistance,
                                                                                                    planarRegionsList.getPlanarRegionsAsList());

      status.set(SwingOverPlanarRegionsStatus.SEARCHING_FOR_SOLUTION);
      numberOfTriesCounter.resetCount();

      // do an initial fast approximation. This approximation just looks to see what the closest point to the line segments that make up the basic trajectory
      // are, and modifies based on that.
      while (doInitialFastApproximation.getBooleanValue() && status.getEnumValue().equals(SwingOverPlanarRegionsStatus.SEARCHING_FOR_SOLUTION)
             && !numberOfTriesCounter.maxCountReached())
      {
         for (SwingOverPlanarRegionsCollisionType swingOverPlanarRegionsTrajectoryCollisionType : SwingOverPlanarRegionsCollisionType.values())
         {
            closestPolygonPointMap.get(swingOverPlanarRegionsTrajectoryCollisionType)
                                  .setIncludingFrame(worldFrame, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
         }
         mostSevereCollisionType.set(SwingOverPlanarRegionsCollisionType.NO_INTERSECTION);

         status.set(checkAndAdjustForCollisions(filteredRegions, this::getFractionAlongLineForCollision));
         updateVisualizer();
         numberOfTriesCounter.countOne();
      }

      twoWaypointSwingGenerator.setTrajectoryType(TrajectoryType.CUSTOM, adjustedWaypoints);
      twoWaypointSwingGenerator.initialize();

      status.set(SwingOverPlanarRegionsStatus.SEARCHING_FOR_SOLUTION);
      // walk along the trajectory and look for collisions, and adjust your waypoints if there is one.
      while (status.getEnumValue().equals(SwingOverPlanarRegionsStatus.SEARCHING_FOR_SOLUTION) && !numberOfTriesCounter.maxCountReached())
      {
         for (SwingOverPlanarRegionsCollisionType swingOverPlanarRegionsTrajectoryCollisionType : SwingOverPlanarRegionsCollisionType.values())
         {
            closestPolygonPointMap.get(swingOverPlanarRegionsTrajectoryCollisionType)
                                  .setIncludingFrame(worldFrame, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
         }
         mostSevereCollisionType.set(SwingOverPlanarRegionsCollisionType.NO_INTERSECTION);

         status.set(checkAndAdjustForCollisions(filteredRegions, this::getFractionThroughTrajectoryForCollision));

         if (!status.getEnumValue().equals(SwingOverPlanarRegionsStatus.FAILURE_HIT_MAX_ADJUSTMENT_DISTANCE))
         {
            twoWaypointSwingGenerator.setTrajectoryType(TrajectoryType.CUSTOM, adjustedWaypoints);
            twoWaypointSwingGenerator.initialize();
         }

         updateVisualizer();
         numberOfTriesCounter.countOne();
      }

      return twoWaypointSwingGenerator.computeAndGetMaxSpeed();
   }

   // TODO figure out a better solution for coincident points and replace this
   private void adjustSwingEndIfCoincidentWithSwingStart()
   {
      if (swingStartPosition.distance(swingEndPosition) < 1e-8)
         swingEndPosition.add(1e-4, 1e-4, 1e-4);
   }

   private void initializeSwingWaypoints()
   {
      FramePoint3DBasics firstBaseWaypoint = originalWaypoints.get(0);
      FramePoint3DBasics secondBaseWaypoint = originalWaypoints.get(1);

      firstBaseWaypoint.interpolate(swingStartPosition, swingEndPosition, swingWaypointProportions[0]);
      secondBaseWaypoint.interpolate(swingStartPosition, swingEndPosition, swingWaypointProportions[1]);
      double firstWaypointHeight = Math.max(swingStartPosition.getZ(), firstBaseWaypoint.getZ()) + minimumSwingHeight;
      double secondWaypointHeight = Math.max(swingEndPosition.getZ(), secondBaseWaypoint.getZ()) + minimumSwingHeight;
      firstBaseWaypoint.setZ(firstWaypointHeight);
      secondBaseWaypoint.setZ(secondWaypointHeight);

      adjustedWaypoints.get(0).set(firstBaseWaypoint);
      adjustedWaypoints.get(1).set(secondBaseWaypoint);
   }

   /**
    * Checks for collisions using the {@param collisionChecker}, and then modifies the waypoints if one is detected
    */
   private SwingOverPlanarRegionsStatus checkAndAdjustForCollisions(List<PlanarRegion> planarRegionsList,
                                                                    FractionThroughTrajectoryForCollision collisionChecker)
   {
      double maxAdjustmentDistanceSquared = MathTools.square(maximumAdjustmentDistance.getDoubleValue());

      FramePoint3DBasics originalFirstWaypoint = originalWaypoints.get(0);
      FramePoint3DBasics originalSecondWaypoint = originalWaypoints.get(1);
      FramePoint3DBasics adjustedFirstWaypoint = adjustedWaypoints.get(0);
      FramePoint3DBasics adjustedSecondWaypoint = adjustedWaypoints.get(1);

      Point3D pointOnTrajectory = new Point3D();
      Point3D nearestCollision = new Point3D();

      double fractionForCollision = collisionChecker.getFractionThroughTrajectoryForCollision(planarRegionsList, pointOnTrajectory, nearestCollision);

      if (fractionForCollision >= 0.0)
      { // we've detected a collision. We need to adjust to avoid the collision
         wereWaypointsAdjusted.set(true);
         waypointAdjustment.setToZero(worldFrame);
         waypointAdjustment.sub(pointOnTrajectory, nearestCollision);
         double distanceToCollision = waypointAdjustment.length();

//         if (MathTools.epsilonEquals(distanceToCollision, 0.0, 1e-3))
         {  // we are directly going through an object here. That means we don't have a 'vector' to the collision, so instead we can push away from the
            // ground midpoint of the trajectory
            computeWaypointAdjustmentDirection(fractionForCollision);
            waypointAdjustment.scale(minimumAdjustmentIncrementDistance.getDoubleValue());
         }
//         else
//         {
//            waypointAdjustment.changeFrame(midFootReferenceFrame);
//            waypointAdjustment.setY(0.0);
//            waypointAdjustment.changeFrame(worldFrame);
//
//             we've detected a collision. Let's push the waypoints away from it. We don't necessarily want to completely move that far (see gradient descent
//             theory), so let's scale the adjustment a little bit, and also clamp it to be between two predictable values.
//            double adjustmentDistance = MathTools.clamp(adjustmentIncrementDistanceGain.getDoubleValue() * distanceToCollision,
//                                                        minimumAdjustmentIncrementDistance.getDoubleValue(),
//                                                        maximumAdjustmentIncrementDistance.getDoubleValue());
//            waypointAdjustment.scale(adjustmentDistance / distanceToCollision);
//         }

         // TODO clamp the adjustment so that the waypoints remain above the foot
         // apply the total waypoint adjustment scaled by how far through the swing we are.
         adjustedFirstWaypoint.scaleAdd(1.0 - fractionForCollision, waypointAdjustment, adjustedFirstWaypoint);
         adjustedSecondWaypoint.scaleAdd(fractionForCollision, waypointAdjustment, adjustedSecondWaypoint);

         if (adjustedFirstWaypoint.distanceSquared(originalFirstWaypoint) > maxAdjustmentDistanceSquared
             || adjustedSecondWaypoint.distanceSquared(originalSecondWaypoint) > maxAdjustmentDistanceSquared)
         {  // If we've adjusted either waypoint too much, terminate the adjustment.
            return SwingOverPlanarRegionsStatus.FAILURE_HIT_MAX_ADJUSTMENT_DISTANCE;
         }

         // Keep searching, because we had a collision.
         return SwingOverPlanarRegionsStatus.SEARCHING_FOR_SOLUTION;
      }

      // We didn't have a collision, so return that we have a valid trajectory.
      return SwingOverPlanarRegionsStatus.SOLUTION_FOUND;
   }

   /**
    * This approach draws a straight line between each waypoint (start, middle one, middle two, end), and checks each one for a collision.
     */
   private double getFractionAlongLineForCollision(List<PlanarRegion> planarRegions,
                                                   Point3DBasics collisionOnSegmentToPack,
                                                   Point3DBasics collisionOnRegionToPack)
   {
      double firstSegmentLength = swingStartPosition.distance(adjustedWaypoints.get(0));
      double secondSegmentLength = adjustedWaypoints.get(0).distance(adjustedWaypoints.get(1));
      double thirdSegmentLength = adjustedWaypoints.get(1).distance(swingEndPosition);
      double totalLength = firstSegmentLength + secondSegmentLength + thirdSegmentLength;

      double fractionThroughSegmentForCollision = checkLineSegmentForCollision(swingStartPosition,
                                                                               adjustedWaypoints.get(0),
                                                                               planarRegions,
                                                                               collisionOnSegmentToPack,
                                                                               collisionOnRegionToPack,
                                                                               true);
      if (fractionThroughSegmentForCollision >= 0.0)
      {
         return fractionThroughSegmentForCollision * firstSegmentLength / totalLength;
      }

      fractionThroughSegmentForCollision = checkLineSegmentForCollision(adjustedWaypoints.get(0),
                                                                        adjustedWaypoints.get(1),
                                                                        planarRegions,
                                                                        collisionOnSegmentToPack,
                                                                        collisionOnRegionToPack,
                                                                        false);
      if (fractionThroughSegmentForCollision >= 0.0)
      {
         return (fractionThroughSegmentForCollision * secondSegmentLength + firstSegmentLength) / totalLength;
      }

      fractionThroughSegmentForCollision = checkLineSegmentForCollision(adjustedWaypoints.get(1),
                                                                        swingEndPosition,
                                                                        planarRegions,
                                                                        collisionOnSegmentToPack,
                                                                        collisionOnRegionToPack,
                                                                        false);
      if (fractionThroughSegmentForCollision >= 0.0)
      {
         return (fractionThroughSegmentForCollision * thirdSegmentLength + secondSegmentLength + firstSegmentLength) / totalLength;
      }

      return -1.0;
   }

   /**
    * Returns the fraction through the the linesegment between two waypoints and also packs the point on the segment and the collision point in world.
    */
   private double checkLineSegmentForCollision(Point3DReadOnly firstEndpoint,
                                               Point3DReadOnly secondEndpoint,
                                               List<PlanarRegion> planarRegions,
                                               Point3DBasics collisionOnSegmentToPack,
                                               Point3DBasics collisionOnRegionToPack,
                                               boolean collisionIsOnRising)
   {
      double avoidanceDistance = collisionSphereRadius + minimumClearance.getDoubleValue() - fastApproximationLessClearance.getDoubleValue();
      double avoidanceDistanceSquared = MathTools.square(avoidanceDistance);
      for (PlanarRegion planarRegion : planarRegions)
      {
         Point3D startInLocal = new Point3D(firstEndpoint);
         Point3D endInLocal = new Point3D(secondEndpoint);
         planarRegion.transformFromWorldToLocal(startInLocal);
         planarRegion.transformFromWorldToLocal(endInLocal);

         Point3D closestPointOnSegment = new Point3D();

         PlanarRegionTools.getDistanceFromLineSegment3DToPlanarRegion(startInLocal, endInLocal, planarRegion, closestPointOnSegment, collisionOnRegionToPack);

         planarRegion.transformFromLocalToWorld(collisionOnRegionToPack);
         planarRegion.getTransformToWorld().transform(closestPointOnSegment, collisionOnSegmentToPack);
         trajectoryPosition.set(collisionOnSegmentToPack);

         updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType.NO_INTERSECTION, collisionOnRegionToPack);

         if (checkValidityOfCollisionPoint(collisionOnSegmentToPack, collisionOnRegionToPack, avoidanceDistanceSquared, collisionIsOnRising))
            return closestPointOnSegment.distance(startInLocal) / endInLocal.distance(startInLocal);
      }

      collisionOnSegmentToPack.setToNaN();
      collisionOnRegionToPack.setToNaN();
      return -1.0;
   }

   /**
    * This approach walks through the swing trajectory, and checks to make sure that the trajectory point at each point along the trajectory is a certain
    * distance from the environment.
    */
   private double getFractionThroughTrajectoryForCollision(List<PlanarRegion> planarRegions,
                                                           Point3DBasics pointOnTrajectoryToPack,
                                                           Point3DBasics closestPointOnRegionToPack)
   {
      twoWaypointSwingGenerator.setTrajectoryType(TrajectoryType.CUSTOM, adjustedWaypoints);
      twoWaypointSwingGenerator.initialize();
      adjustedWaypoints.get(0).set(twoWaypointSwingGenerator.getWaypoint(0));
      adjustedWaypoints.get(1).set(twoWaypointSwingGenerator.getWaypoint(1));

      double avoidanceDistance = collisionSphereRadius + minimumClearance.getDoubleValue();
      double avoidanceDistanceSquared = MathTools.square(avoidanceDistance);
      double stepAmount = 1.0 / numberOfCheckpoints.getIntegerValue();

      boolean collisionIsOnRising = true;

      for (double fraction = 0.0; fraction <= 1.0; fraction += stepAmount)
      {
         twoWaypointSwingGenerator.compute(fraction);
         FramePoint3D frameTupleUnsafe = new FramePoint3D(trajectoryPosition);
         twoWaypointSwingGenerator.getPosition(frameTupleUnsafe);
         trajectoryPosition.set(frameTupleUnsafe);
         solePoseReferenceFrame.setPositionAndUpdate(trajectoryPosition);
         pointOnTrajectoryToPack.set(trajectoryPosition);

         twoWaypointSwingGenerator.getWaypointTime(0);
         if (collisionIsOnRising && fraction > twoWaypointSwingGenerator.getWaypointTime(0))
            collisionIsOnRising = false;

         for (PlanarRegion planarRegion : planarRegions)
         {
            Point3DReadOnly closestPointOnRegion = PlanarRegionTools.closestPointOnPlane(trajectoryPosition, planarRegion);
            updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType.NO_INTERSECTION, closestPointOnRegion);

            if (closestPointOnRegion == null)
               continue;

            closestPointOnRegionToPack.set(closestPointOnRegion);

            if (checkValidityOfCollisionPoint(pointOnTrajectoryToPack, closestPointOnRegionToPack, avoidanceDistanceSquared, collisionIsOnRising))
               return fraction;
         }

         updateVisualizer();
      }

      pointOnTrajectoryToPack.setToNaN();
      closestPointOnRegionToPack.setToNaN();
      return -1.0;
   }

   /**
    * This method checks to make sure that the collision point is a valid collision.
    */
   private boolean checkValidityOfCollisionPoint(Point3DReadOnly pointOnTrajectory,
                                                 Point3DReadOnly closestPointOnRegion,
                                                 double avoidanceDistanceSquared,
                                                 boolean collisionIsOnRising)
   {

      double distanceToClosestPoint = closestPointOnRegion.distanceSquared(pointOnTrajectory);

      // if it's too far away, it's not a valid collision
      if (distanceToClosestPoint > avoidanceDistanceSquared)
         return false;

      updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType.TOO_CLOSE_TO_IGNORE_PLANE, closestPointOnRegion);

      // checks we're not colliding with the floor
      if (!checkIfCollidingWithFloorPlane(closestPointOnRegion))
      {
         // we check to make sure that the collision is in a decent spot. As in, reject it if we're above the foot
         boolean isCollisionAboveFootholds =
               isCollisionAboveStartFoot(closestPointOnRegion, collisionIsOnRising) || isCollisionAboveEndFoot(closestPointOnRegion);
         if (isCollisionAboveFootholds)
         {  // move on, we're above the foot
            updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType.COLLISION_ABOVE_FOOT, closestPointOnRegion);
         }
         else
         {
            // Check to see if the collision is closer to the point between the feet than the trajectory is to the mid point. This is effectively drawing
            // an ellipse around the mid ground point, with the curvature defined by the trajectory. Then it checks to make sure the collision is inside that
            // ellipse. If it's not, it's probably not a valid collision.
            // TODO: This may be wrong. Think about trying to move the foot between an opening that isn't wide enough, like a canyon
            boolean isCollisionInsideTheTrajectory = midGroundPoint.distanceSquared(closestPointOnRegion) < midGroundPoint.distanceSquared(trajectoryPosition);

            if (isCollisionInsideTheTrajectory)
            {  // If that condition is valid, we know we have a bad collision and we need to adjust.
               updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType.COLLISION_INSIDE_TRAJECTORY, closestPointOnRegion);
               return true;
            }

            collisionRelativeToStart.setIncludingFrame(worldFrame, closestPointOnRegion);
            collisionRelativeToStart.changeFrame(startOfSwingReferenceFrame);

            // Check to see if the collision is in front of the toe at lift off and behind the heel at touchdown. If it is, we know it's a bad collision.
            // TODO: think about this one some more. Do we want to actually be between the heel at lift off and the toe at touchdown? Probably.
            double toePoint = toeLength;
            double heelPoint = stepRelativeToStart.getX() - heelLength;
            boolean collisionIsBetweenToeAndHeel = MathTools.intervalContains(collisionRelativeToStart.getX(),
                                                                              Math.min(toePoint, heelPoint),
                                                                              Math.max(toePoint, heelPoint));

            if (collisionIsBetweenToeAndHeel)
            {
               updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType.COLLISION_BETWEEN_FEET, closestPointOnRegion);
               return true;
            }

            updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType.OUTSIDE_TRAJECTORY, closestPointOnRegion);
         }
      }

      return false;
   }

   private void updateClosestAndMostSevereIntersectionPoint(SwingOverPlanarRegionsCollisionType collisionType, Point3DReadOnly collision)
   {
      if (collisionType.ordinal() > this.mostSevereCollisionType.getEnumValue().ordinal())
      {
         this.mostSevereCollisionType.set(collisionType);
      }

      if (collision != null)
      {
         if (trajectoryPosition.distanceSquared(collision) < trajectoryPosition.distanceSquared(closestPolygonPointMap.get(collisionType)))
            closestPolygonPointMap.get(collisionType).set(collision);
      }
   }

   /**
    * This checks to make sure that the collision point is above the virtual floor plane by a certain distance. This virtual floor plane is drawn between
    * the start foot and the end foot.
    */
   private boolean checkIfCollidingWithFloorPlane(Point3DReadOnly collisionPoint)
   {
      return swingFloorPlane.distance(collisionPoint) < minimumHeightAboveFloorForCollision.getDoubleValue() + minimumClearance.getDoubleValue();
   }

   /**
    * Checks to see if the collision point is above or below the end foot. If is is, and it's within a proximity of the height above that foot, we return false,
    * because that's probably the floor.
    */
   private boolean isCollisionAboveEndFoot(Point3DReadOnly collisionPoint)
   {
      if (!swingEndPolygon.isPointInside(collisionPoint.getX(), collisionPoint.getY()))
         return false;

      FramePoint3D collisionInEnd = new FramePoint3D(worldFrame, collisionPoint);
      collisionInEnd.changeFrame(endOfSwingReferenceFrame);

      return Math.abs(endOfSwingReferenceFrame.getZ()) < minimumHeightAboveFloorForCollision.getDoubleValue();
   }

   /**
    * Checks to see if the collision point is above or below the start foot. Then, if we're currently picking the foot off the ground, it returns false. We know
    * we put the foot down there, so that point shouldn't actually collide.
    */
   private boolean isCollisionAboveStartFoot(Point3DReadOnly collisionPoint, boolean collisionIsOnRising)
   {
      return collisionIsOnRising && swingStartPolygon.isPointInside(collisionPoint.getX(), collisionPoint.getY());
   }

   /**
    * This computes a waypoint adjustment direction that is radial w.r.t. how far through swing the collision occured at.
    */
   private void computeWaypointAdjustmentDirection(double fraction)
   {
      axisAngle.set(swingTrajectoryPlane.getNormal(), Math.PI * fraction);
      rigidBodyTransform.setRotation(axisAngle);

      waypointAdjustment.sub(swingStartPosition, swingEndPosition);
      waypointAdjustment.normalize();
      rigidBodyTransform.transform(waypointAdjustment);
   }

   /**
    * Returns the modified waypoints that should avoid collisions in the world.
    */
   public List<FramePoint3D> getExpandedWaypoints()
   {
      return adjustedWaypoints;
   }

   /**
    * Returns whether or not the waypoints were modified.
    */
   public boolean wereWaypointsAdjusted()
   {
      return wereWaypointsAdjusted.getBooleanValue();
   }

   public SwingOverPlanarRegionsStatus getStatus()
   {
      return status.getEnumValue();
   }

   // VISULIZER METHODS

   public void updateVisualizer()
   {
      if (visualizer.isPresent())
      {
         visualizer.get().run();
      }
   }

   public void attachVisualizer(Runnable visualizer)
   {
      this.visualizer = Optional.of(visualizer);
   }

   public PoseReferenceFrame getSolePoseReferenceFrame()
   {
      return solePoseReferenceFrame;
   }

   public FramePoint3D getClosestPolygonPoint(SwingOverPlanarRegionsCollisionType collisionType)
   {
      return closestPolygonPointMap.get(collisionType);
   }

   public double getCollisionSphereRadius()
   {
      return collisionSphereRadius;
   }

   public double getMinimumClearance()
   {
      return minimumClearance.getDoubleValue();
   }

   @FunctionalInterface
   private interface FractionThroughTrajectoryForCollision
   {
      double getFractionThroughTrajectoryForCollision(List<PlanarRegion> planarRegionList,
                                                      Point3DBasics pointOnTrajectoryToPack,
                                                      Point3DBasics nearestPointInWorldToPack);
   }
}
