package us.ihmc.robotics.math.trajectories;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.math.trajectories.generators.MultipleWaypointsPoseTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.trajectorypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.math.trajectories.trajectorypoints.FrameSE3TrajectoryPoint;
import us.ihmc.yoVariables.registry.YoRegistry;

public class MultipleWaypointsBlendedPoseTrajectoryGenerator extends BlendedPoseTrajectoryGenerator
{
   private final MultipleWaypointsPoseTrajectoryGenerator trajectory;

   public MultipleWaypointsBlendedPoseTrajectoryGenerator(String prefix, MultipleWaypointsPoseTrajectoryGenerator trajectory, ReferenceFrame trajectoryFrame,
                                                          YoRegistry parentRegistry)
   {
      super(prefix, trajectory, trajectoryFrame, parentRegistry);

      this.trajectory = trajectory;
   }

   public int getCurrentPositionWaypointIndex()
   {
      return trajectory.getCurrentPositionWaypointIndex();
   }

   public void clearTrajectory(ReferenceFrame referenceFrame)
   {
      this.trajectory.clear(referenceFrame);
   }

   public void appendPositionWaypoint(FrameEuclideanTrajectoryPoint positionWaypoint)
   {
      this.trajectory.appendPositionWaypoint(positionWaypoint);
   }

   public void appendPositionWaypoint(double timeAtWaypoint, FramePoint3D position, FrameVector3D linearVelocity)
   {
      this.trajectory.appendPositionWaypoint(timeAtWaypoint, position, linearVelocity);
   }

   public void appendOrientationWaypoint(double timeAtWaypoint, FrameQuaternion orientation, FrameVector3D angularVelocity)
   {
      this.trajectory.appendOrientationWaypoint(timeAtWaypoint, orientation, angularVelocity);
   }

   public void appendPoseWaypoint(FrameSE3TrajectoryPoint waypoint)
   {
      this.trajectory.appendPoseWaypoint(waypoint);
   }

}
