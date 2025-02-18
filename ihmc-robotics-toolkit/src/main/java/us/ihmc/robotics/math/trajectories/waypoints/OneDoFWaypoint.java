package us.ihmc.robotics.math.trajectories.waypoints;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import us.ihmc.robotics.math.trajectories.waypoints.interfaces.OneDoFWaypointBasics;

public class OneDoFWaypoint implements OneDoFWaypointBasics
{
   private double position;
   private double velocity;

   @Override
   public void setPosition(double position)
   {
      this.position = position;
   }

   @Override
   public void setVelocity(double velocity)
   {
      this.velocity = velocity;
   }

   @Override
   public double getPosition()
   {
      return position;
   }

   @Override
   public double getVelocity()
   {
      return velocity;
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
