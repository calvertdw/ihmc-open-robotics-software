package us.ihmc.robotics.math.trajectories.trajectorypoints;

import static us.ihmc.robotics.math.frames.YoFrameVariableNameTools.createName;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import us.ihmc.robotics.math.trajectories.trajectorypoints.interfaces.OneDoFTrajectoryPointBasics;
import us.ihmc.robotics.math.trajectories.waypoints.YoOneDoFWaypoint;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class YoOneDoFTrajectoryPoint implements OneDoFTrajectoryPointBasics
{
   private final String namePrefix;
   private final String nameSuffix;

   private final YoOneDoFWaypoint waypoint;
   private final YoDouble time;

   public YoOneDoFTrajectoryPoint(String namePrefix, String nameSuffix, YoVariableRegistry registry)
   {
      this.namePrefix = namePrefix;
      this.nameSuffix = nameSuffix;

      time = new YoDouble(createName(namePrefix, "time", nameSuffix), registry);
      waypoint = new YoOneDoFWaypoint(namePrefix, nameSuffix, registry);
   }

   @Override
   public void setPosition(double position)
   {
      waypoint.setPosition(position);
   }

   @Override
   public void setVelocity(double velocity)
   {
      waypoint.setVelocity(velocity);
   }

   @Override
   public double getPosition()
   {
      return waypoint.getPosition();
   }

   @Override
   public double getVelocity()
   {
      return waypoint.getVelocity();
   }

   @Override
   public void setTime(double time)
   {
      this.time.set(time);
   }

   @Override
   public double getTime()
   {
      return time.getValue();
   }

   public String getNamePrefix()
   {
      return namePrefix;
   }

   public String getNameSuffix()
   {
      return nameSuffix;
   }

   @Override
   public String toString()
   {
      NumberFormat doubleFormat = new DecimalFormat(" 0.00;-0.00");
      String timeString = "time = " + doubleFormat.format(getTime());
      return "Trajectory point 1D: (" + timeString + ", " + waypoint + ")";
   }
}
