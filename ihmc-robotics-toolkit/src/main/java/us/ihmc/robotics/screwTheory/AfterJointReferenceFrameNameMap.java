package us.ihmc.robotics.screwTheory;

import java.util.LinkedHashMap;
import java.util.List;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.tools.MultiBodySystemTools;

public class AfterJointReferenceFrameNameMap
{
   private final LinkedHashMap<String, ReferenceFrame> afterJointReferenceFrames = new LinkedHashMap<String, ReferenceFrame>();
   
   public AfterJointReferenceFrameNameMap(RigidBodyBasics base)
   {
      RigidBodyBasics[] rootBodies = {base};
      JointBasics[] joints = MultiBodySystemTools.collectSubtreeJoints(rootBodies);
      
      for(JointBasics joint : joints)
      {
         afterJointReferenceFrames.put(joint.getFrameAfterJoint().getName(), joint.getFrameAfterJoint());
      }
   }
   
   public AfterJointReferenceFrameNameMap(List<ReferenceFrame> frames)
   {
      for(ReferenceFrame frame : frames)
      {
         afterJointReferenceFrames.put(frame.getName(), frame);
      }
   }

   public ReferenceFrame getFrameByName(String name)
   {
      final ReferenceFrame referenceFrame = afterJointReferenceFrames.get(name);
      if(referenceFrame == null)
      {
         throw new RuntimeException("Unknown frame " + name);
      }
      return referenceFrame;
   }
}
