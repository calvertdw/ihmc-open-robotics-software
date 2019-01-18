package us.ihmc.robotics.math.trajectories.waypoints;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameQuaternionReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.euclid.transform.interfaces.Transform;
import us.ihmc.robotics.geometry.frameObjects.FrameSE3Waypoint;
import us.ihmc.robotics.math.trajectories.waypoints.interfaces.FrameSE3TrajectoryPointInterface;

public class FrameSE3TrajectoryPoint implements FrameSE3TrajectoryPointInterface
{
   private final FrameSE3Waypoint se3Waypoint = new FrameSE3Waypoint();
   private final TrajectoryPoint trajectoryPoint = new TrajectoryPoint();

   public FrameSE3TrajectoryPoint()
   {
   }

   public FrameSE3TrajectoryPoint(double time, FramePoint3DReadOnly position, FrameQuaternionReadOnly orientation, FrameVector3DReadOnly linearVelocity,
                                  FrameVector3DReadOnly angularVelocity)
   {
      setIncludingFrame(time, position, orientation, linearVelocity, angularVelocity);
   }

   @Override
   public FramePoint3DReadOnly getPosition()
   {
      return se3Waypoint.getPosition();
   }

   @Override
   public void setPosition(double x, double y, double z)
   {
      se3Waypoint.setPosition(x, y, z);
   }

   @Override
   public FrameVector3DReadOnly getLinearVelocity()
   {
      return se3Waypoint.getLinearVelocity();
   }

   @Override
   public void setLinearVelocity(double x, double y, double z)
   {
      se3Waypoint.setLinearVelocity(x, y, z);
   }

   @Override
   public void applyTransform(Transform transform)
   {
      se3Waypoint.applyTransform(transform);
   }

   @Override
   public void applyInverseTransform(Transform transform)
   {
      se3Waypoint.applyInverseTransform(transform);
   }

   @Override
   public FrameQuaternionReadOnly getOrientation()
   {
      return se3Waypoint.getOrientation();
   }

   @Override
   public void setOrientation(double x, double y, double z, double s)
   {
      se3Waypoint.setOrientation(x, y, z, s);
   }

   @Override
   public FrameVector3DReadOnly getAngularVelocity()
   {
      return se3Waypoint.getAngularVelocity();
   }

   @Override
   public void setAngularVelocity(double x, double y, double z)
   {
      se3Waypoint.setAngularVelocity(x, y, z);
   }

   @Override
   public void setReferenceFrame(ReferenceFrame referenceFrame)
   {
      se3Waypoint.setReferenceFrame(referenceFrame);
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return se3Waypoint.getReferenceFrame();
   }

   @Override
   public void setTime(double time)
   {
      trajectoryPoint.setTime(time);
   }

   @Override
   public double getTime()
   {
      return trajectoryPoint.getTime();
   }

   @Override
   public String toString()
   {
      return "SE3 trajectory point: (time = " + WaypointToStringTools.format(getTime()) + ", " + WaypointToStringTools.waypointToString(se3Waypoint) + ")";
   }
}
