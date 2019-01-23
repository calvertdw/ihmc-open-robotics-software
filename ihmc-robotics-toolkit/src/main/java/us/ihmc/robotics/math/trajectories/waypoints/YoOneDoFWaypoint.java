package us.ihmc.robotics.math.trajectories.waypoints;

import static us.ihmc.robotics.math.frames.YoFrameVariableNameTools.createName;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import us.ihmc.robotics.math.trajectories.waypoints.interfaces.OneDoFWaypointInterface;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class YoOneDoFWaypoint implements OneDoFWaypointInterface
{
   private final String namePrefix;
   private final String nameSuffix;

   private final YoDouble position;
   private final YoDouble velocity;

   public YoOneDoFWaypoint(String namePrefix, String nameSuffix, YoVariableRegistry registry)
   {
      this.namePrefix = namePrefix;
      this.nameSuffix = nameSuffix;

      position = new YoDouble(createName(namePrefix, "position", nameSuffix), registry);
      velocity = new YoDouble(createName(namePrefix, "velocity", nameSuffix), registry);
   }

   @Override
   public void setPosition(double position)
   {
      this.position.set(position);
   }

   @Override
   public void setVelocity(double velocity)
   {
      this.velocity.set(velocity);
   }

   @Override
   public double getPosition()
   {
      return position.getDoubleValue();
   }

   @Override
   public double getVelocity()
   {
      return velocity.getDoubleValue();
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
      String positionString = "position = " + doubleFormat.format(getPosition());
      String velocityString = "velocity = " + doubleFormat.format(getVelocity());
      return "Waypoint 1D: (" + positionString + ", " + velocityString + ")";
   }
}
