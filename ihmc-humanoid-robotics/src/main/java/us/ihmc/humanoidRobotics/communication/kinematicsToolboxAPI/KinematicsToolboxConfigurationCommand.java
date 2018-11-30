package us.ihmc.humanoidRobotics.communication.kinematicsToolboxAPI;

import controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;

public class KinematicsToolboxConfigurationCommand implements Command<KinematicsToolboxConfigurationCommand, KinematicsToolboxConfigurationMessage>
{
   private boolean hasPrivilegedRootJointPosition = false;
   private final Point3D privilegedRootJointPosition = new Point3D();
   private boolean hasPrivilegedRootJointOrientation = false;
   private final Quaternion privilegedRootJointOrientation = new Quaternion();

   private boolean hasPrivilegedJointAngles = false;
   private final TIntArrayList jointHashCodes = new TIntArrayList();
   private final TFloatArrayList privilegedJointAngles = new TFloatArrayList();

   @Override
   public void clear()
   {
      hasPrivilegedRootJointPosition = false;
      privilegedRootJointPosition.setToNaN();
      hasPrivilegedRootJointOrientation = false;
      privilegedRootJointOrientation.setToNaN();
   }

   @Override
   public void set(KinematicsToolboxConfigurationCommand other)
   {
      hasPrivilegedRootJointPosition = other.hasPrivilegedRootJointPosition;
      if (hasPrivilegedRootJointPosition)
         privilegedRootJointPosition.set(other.getPrivilegedRootJointPosition());
      else
         privilegedRootJointPosition.setToNaN();

      hasPrivilegedRootJointOrientation = other.hasPrivilegedRootJointOrientation;
      if (hasPrivilegedRootJointOrientation)
         privilegedRootJointOrientation.set(other.getPrivilegedRootJointOrientation());
      else
         privilegedRootJointOrientation.setToNaN();

      hasPrivilegedJointAngles = other.hasPrivilegedJointAngles;
      jointHashCodes.reset();
      privilegedJointAngles.reset();

      if (hasPrivilegedJointAngles)
      {
         jointHashCodes.addAll(other.getJointHashCodes());
         privilegedJointAngles.addAll(other.getPrivilegedJointAngles());
      }
   }

   @Override
   public void setFromMessage(KinematicsToolboxConfigurationMessage message)
   {
      hasPrivilegedRootJointPosition = message.getPrivilegedRootJointPosition() != null;
      if (hasPrivilegedRootJointPosition)
         privilegedRootJointPosition.set(message.getPrivilegedRootJointPosition());
      else
         privilegedRootJointPosition.setToNaN();

      hasPrivilegedRootJointOrientation = message.getPrivilegedRootJointOrientation() != null;
      if (hasPrivilegedRootJointOrientation)
         privilegedRootJointOrientation.set(message.getPrivilegedRootJointOrientation());
      else
         privilegedRootJointOrientation.setToNaN();

      TIntArrayList messageHashCodes = message.getPrivilegedJointHashCodes();
      TFloatArrayList messageJointAngles = message.getPrivilegedJointAngles();

      hasPrivilegedJointAngles = messageHashCodes != null && messageJointAngles != null;
      jointHashCodes.reset();
      privilegedJointAngles.reset();

      if (hasPrivilegedJointAngles)
      {
         jointHashCodes.addAll(messageHashCodes);
         privilegedJointAngles.addAll(messageJointAngles);
      }
   }

   public boolean hasPrivilegedRootJointPosition()
   {
      return hasPrivilegedRootJointPosition;
   }

   public boolean hasPrivilegedRootJointOrientation()
   {
      return hasPrivilegedRootJointOrientation;
   }

   public boolean hasPrivilegedJointAngles()
   {
      return hasPrivilegedJointAngles;
   }

   public Point3D getPrivilegedRootJointPosition()
   {
      return privilegedRootJointPosition;
   }

   public Quaternion getPrivilegedRootJointOrientation()
   {
      return privilegedRootJointOrientation;
   }

   public TIntArrayList getJointHashCodes()
   {
      return jointHashCodes;
   }

   public TFloatArrayList getPrivilegedJointAngles()
   {
      return privilegedJointAngles;
   }

   @Override
   public Class<KinematicsToolboxConfigurationMessage> getMessageClass()
   {
      return KinematicsToolboxConfigurationMessage.class;
   }

   @Override
   public boolean isCommandValid()
   {
      return true;
   }
}
