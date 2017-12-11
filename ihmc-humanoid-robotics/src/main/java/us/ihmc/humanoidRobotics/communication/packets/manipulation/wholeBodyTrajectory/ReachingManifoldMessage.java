package us.ihmc.humanoidRobotics.communication.packets.manipulation.wholeBodyTrajectory;

import java.util.Arrays;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.tools.ArrayTools;

/**
 * Point, Cylinder, Sphere, Box will be converted into this message.
 */
public class ReachingManifoldMessage extends Packet<ReachingManifoldMessage>
{
   public long endEffectorNameBasedHashCode;
   public Point3D manifoldOriginPosition;
   public Quaternion manifoldOriginOrientation;

   public ConfigurationSpaceName[] manifoldConfigurationSpaces;
   public double[] manifoldLowerLimits;
   public double[] manifoldUpperLimits;

   public ReachingManifoldMessage()
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public ReachingManifoldMessage(RigidBody rigidBody)
   {
      this.endEffectorNameBasedHashCode = rigidBody.getNameBasedHashCode();

      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public void setOrigin(Point3D position, Quaternion orientation)
   {
      manifoldOriginPosition = new Point3D(position);
      manifoldOriginOrientation = new Quaternion(orientation);
   }

   public void setMenifoldAPoint()
   {
      ConfigurationSpaceName[] configurations = {};
      double[] lowerLimits = {};
      double[] upperLimits = {};
      
      setManifold(configurations, lowerLimits, upperLimits);
   }
   
   public void setManifold(ConfigurationSpaceName[] configurationSpaces, double[] lowerLimits, double[] upperLimits)
   {
      if (configurationSpaces.length != lowerLimits.length || configurationSpaces.length != upperLimits.length || lowerLimits.length != upperLimits.length)
         throw new RuntimeException("Inconsistent array lengths: configurationSpaces = " + configurationSpaces.length);

      this.manifoldConfigurationSpaces = new ConfigurationSpaceName[configurationSpaces.length];
      this.manifoldLowerLimits = new double[configurationSpaces.length];
      this.manifoldUpperLimits = new double[configurationSpaces.length];

      for (int i = 0; i < configurationSpaces.length; i++)
      {
         this.manifoldConfigurationSpaces[i] = configurationSpaces[i];
         this.manifoldLowerLimits[i] = lowerLimits[i];
         this.manifoldUpperLimits[i] = upperLimits[i];
      }
   }
   
   @Override
   public boolean epsilonEquals(ReachingManifoldMessage other, double epsilon)
   {
      if (endEffectorNameBasedHashCode != other.endEffectorNameBasedHashCode)
         return false;
      if (!manifoldOriginPosition.epsilonEquals(other.manifoldOriginPosition, epsilon))
         return false;
      if (!manifoldOriginOrientation.epsilonEquals(other.manifoldOriginOrientation, epsilon))
         return false;
      if (!Arrays.equals(manifoldConfigurationSpaces, other.manifoldConfigurationSpaces))
         return false;
      if (!ArrayTools.deltaEquals(manifoldLowerLimits, other.manifoldLowerLimits, epsilon))
         return false;
      if (!ArrayTools.deltaEquals(manifoldUpperLimits, other.manifoldUpperLimits, epsilon))
         return false;
      return true;
   }
}
