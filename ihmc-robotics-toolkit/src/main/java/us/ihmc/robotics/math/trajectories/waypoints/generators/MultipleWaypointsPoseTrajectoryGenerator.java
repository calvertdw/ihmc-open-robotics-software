package us.ihmc.robotics.math.trajectories.waypoints.generators;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.math.trajectories.PoseTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.math.trajectories.waypoints.FrameSE3TrajectoryPoint;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class MultipleWaypointsPoseTrajectoryGenerator implements PoseTrajectoryGenerator
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final MultipleWaypointsPositionTrajectoryGenerator positionTrajectory;
   private final MultipleWaypointsOrientationTrajectoryGenerator orientationTrajectory;

   private ReferenceFrame activeFrame;

   private final FramePoint3D desiredPosition = new FramePoint3D();
   private final FrameQuaternion desiredOrientation = new FrameQuaternion();

   public MultipleWaypointsPoseTrajectoryGenerator(String namePrefix, int maxNumberOfWaypoints, YoVariableRegistry parentRegistry)
   {
      positionTrajectory = new MultipleWaypointsPositionTrajectoryGenerator(namePrefix, maxNumberOfWaypoints, worldFrame, parentRegistry);
      orientationTrajectory = new MultipleWaypointsOrientationTrajectoryGenerator(namePrefix, maxNumberOfWaypoints, worldFrame, parentRegistry);
      activeFrame = worldFrame;
   }

   public void clear(ReferenceFrame referenceFrame)
   {
      positionTrajectory.clear(referenceFrame);
      orientationTrajectory.clear(referenceFrame);
      activeFrame = referenceFrame;
   }

   public void appendPoseWaypoint(FrameSE3TrajectoryPoint waypoint)
   {
      waypoint.changeFrame(activeFrame);
      positionTrajectory.appendWaypoint(waypoint);
      orientationTrajectory.appendWaypoint(waypoint);
   }

   public void appendPoseWaypoint(double timeAtWaypoint, FramePose3D pose, FrameVector3D linearVelocity, FrameVector3D angularVelocity)
   {
      pose.changeFrame(activeFrame);
      linearVelocity.changeFrame(activeFrame);
      angularVelocity.changeFrame(activeFrame);

      positionTrajectory.appendWaypoint(timeAtWaypoint, pose.getPosition(), linearVelocity);
      orientationTrajectory.appendWaypoint(timeAtWaypoint, pose.getOrientation(), angularVelocity);
   }

   public void appendPositionWaypoint(double timeAtWaypoint, FramePoint3D position, FrameVector3D linearVelocity)
   {
      position.changeFrame(activeFrame);
      linearVelocity.changeFrame(activeFrame);
      positionTrajectory.appendWaypoint(timeAtWaypoint, position, linearVelocity);
   }

   public void appendPositionWaypoint(FrameEuclideanTrajectoryPoint positionWaypoint)
   {
      positionWaypoint.changeFrame(activeFrame);
      positionTrajectory.appendWaypoint(positionWaypoint);
   }

   public void appendOrientationWaypoint(double timeAtWaypoint, FrameQuaternion orientation, FrameVector3D angularVelocity)
   {
      orientation.changeFrame(activeFrame);
      angularVelocity.changeFrame(activeFrame);
      orientationTrajectory.appendWaypoint(timeAtWaypoint, orientation, angularVelocity);
   }

   public void changeFrame(ReferenceFrame referenceFrame)
   {
      positionTrajectory.changeFrame(referenceFrame);
      orientationTrajectory.changeFrame(referenceFrame);
      activeFrame = referenceFrame;
   }

   public int getCurrentPositionWaypointIndex()
   {
      return positionTrajectory.getCurrentWaypointIndex();
   }

   @Override
   public void initialize()
   {
      positionTrajectory.initialize();
      orientationTrajectory.initialize();
   }

   @Override
   public void compute(double time)
   {
      positionTrajectory.compute(time);
      orientationTrajectory.compute(time);
   }

   @Override
   public boolean isDone()
   {
      return positionTrajectory.isDone() && orientationTrajectory.isDone();
   }

   @Override
   public void getPosition(FramePoint3D positionToPack)
   {
      positionTrajectory.getPosition(positionToPack);
   }

   @Override
   public void getVelocity(FrameVector3D velocityToPack)
   {
      positionTrajectory.getVelocity(velocityToPack);
   }

   @Override
   public void getAcceleration(FrameVector3D accelerationToPack)
   {
      positionTrajectory.getAcceleration(accelerationToPack);
   }

   @Override
   public void getOrientation(FrameQuaternion orientationToPack)
   {
      orientationTrajectory.getOrientation(orientationToPack);
   }

   @Override
   public void getAngularVelocity(FrameVector3D angularVelocityToPack)
   {
      orientationTrajectory.getAngularVelocity(angularVelocityToPack);
   }

   @Override
   public void getAngularAcceleration(FrameVector3D angularAccelerationToPack)
   {
      orientationTrajectory.getAngularAcceleration(angularAccelerationToPack);
   }

   @Override
   public void getPose(FramePose3D framePoseToPack)
   {
      getPosition(desiredPosition);
      getOrientation(desiredOrientation);
      framePoseToPack.set(desiredPosition, desiredOrientation);
   }

   @Override
   public void showVisualization()
   {
   }

   @Override
   public void hideVisualization()
   {
   }
}
