package us.ihmc.commonWalkingControlModules.barrierScheduler.context;

import us.ihmc.mecano.multiBodySystem.interfaces.FloatingJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.robotModels.FullHumanoidRobotModel;

public class HumanoidRobotContextTools
{
   public static void updateContext(FullHumanoidRobotModel fullRobotModel, HumanoidRobotContextJointData contextData)
   {
      for (int jointIndex = 0; jointIndex < fullRobotModel.getOneDoFJoints().length; jointIndex++)
      {
         OneDoFJointBasics joint = fullRobotModel.getOneDoFJoints()[jointIndex];
         contextData.setJointQForIndex(jointIndex, joint.getQ());
         contextData.setJointQdForIndex(jointIndex, joint.getQd());
         contextData.setJointQddForIndex(jointIndex, joint.getQdd());
         contextData.setJointTauForIndex(jointIndex, joint.getTau());
      }
      FloatingJointBasics rootJoint = fullRobotModel.getRootJoint();
      contextData.setRootJointData(rootJoint.getJointPose(), rootJoint.getJointTwist(), rootJoint.getJointAcceleration());
   }

   public static void updateRobot(FullHumanoidRobotModel fullRobotModel, HumanoidRobotContextJointData contextData)
   {
      for (int jointIndex = 0; jointIndex < fullRobotModel.getOneDoFJoints().length; jointIndex++)
      {
         OneDoFJointBasics joint = fullRobotModel.getOneDoFJoints()[jointIndex];
         joint.setQ(contextData.getJointQForIndex(jointIndex));
         joint.setQd(contextData.getJointQdForIndex(jointIndex));
         joint.setQdd(contextData.getJointQddForIndex(jointIndex));
         joint.setTau(contextData.getJointTauForIndex(jointIndex));
      }
      FloatingJointBasics rootJoint = fullRobotModel.getRootJoint();
      HumanoidRobotContextRootJointData rootJointData = contextData.getRootJointData();
      rootJoint.setJointOrientation(rootJointData.getRootJointOrientation());
      rootJoint.setJointAngularVelocity(rootJointData.getRootJointAngularVelocity());
      rootJoint.setJointAngularAcceleration(rootJointData.getRootJointAngularAcceleration());
      rootJoint.setJointPosition(rootJointData.getRootJointLocation());
      rootJoint.setJointLinearVelocity(rootJointData.getRootJointLinearVelocity());
      rootJoint.setJointLinearAcceleration(rootJointData.getRootJointLinearAcceleration());
      fullRobotModel.updateFrames();
   }
}
