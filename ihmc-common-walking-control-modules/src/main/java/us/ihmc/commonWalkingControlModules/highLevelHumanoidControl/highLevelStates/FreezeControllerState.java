package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import us.ihmc.commonWalkingControlModules.configurations.HighLevelControllerParameters;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelController;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.sensorProcessing.outputData.JointDesiredControlMode;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutput;

public class FreezeControllerState extends HoldPositionControllerState
{
   private static final HighLevelController controllerState = HighLevelController.FREEZE_STATE;

   public FreezeControllerState(HighLevelHumanoidControllerToolbox controllerToolbox, HighLevelControllerParameters highLevelControllerParameters)
   {
      super(controllerState, controllerToolbox);

      OneDoFJoint[] oneDoFJoints = controllerToolbox.getFullRobotModel().getOneDoFJoints();
      for (OneDoFJoint joint : oneDoFJoints)
      {
         JointDesiredControlMode jointControlMode = highLevelControllerParameters.getJointDesiredControlMode(joint.getName(), controllerState);
         JointDesiredOutput jointDesiredOutput = lowLevelOneDoFJointDesiredDataHolder.getJointDesiredOutput(joint);
         jointDesiredOutput.setControlMode(jointControlMode);
         jointDesiredOutput.setStiffness(highLevelControllerParameters.getDesiredJointStiffness(joint.getName(), controllerState));
         jointDesiredOutput.setDamping(highLevelControllerParameters.getDesiredJointDamping(joint.getName(), controllerState));
      }
   }
}
