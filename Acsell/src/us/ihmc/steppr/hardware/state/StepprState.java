package us.ihmc.steppr.hardware.state;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.steppr.hardware.StepprActuator;
import us.ihmc.steppr.hardware.StepprJoint;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.LongYoVariable;

public class StepprState
{
   private final YoVariableRegistry registry = new YoVariableRegistry("Steppr");
   
   private final LongYoVariable lastReceivedTime = new LongYoVariable("lastReceivedTime", registry);
   private final LongYoVariable timeSincePreviousPacket = new LongYoVariable("timeSincePreviousPacket", registry);
   
   private final IntegerYoVariable corruptedPackets = new IntegerYoVariable("corruptedPackets", registry);
   
   private final LongYoVariable stateCollectionStartTime = new LongYoVariable("stateCollectionStartTime", registry);
   private final LongYoVariable stateCollectionFinishTime = new LongYoVariable("stateCollectionFinishTime", registry);
   
   private final EnumMap<StepprActuator, StepprActuatorState> actuatorStates = new EnumMap<>(StepprActuator.class);
   private final StepprPowerDistributionADCState powerDistributionState = new StepprPowerDistributionADCState("powerDistribution", registry);
   private final StepprXSensState xsens = new StepprXSensState("xsens", registry);
   
   private final EnumMap<StepprJoint, StepprJointState> jointStates = new EnumMap<>(StepprJoint.class);

   public StepprState(YoVariableRegistry parentRegistry)
   {
      createActuators();
      createJoints();
      
      parentRegistry.addChild(registry);
   }

   private void createJoints()
   {
      for(StepprJoint joint : StepprJoint.values)
      {
         if(joint.isLinear())
         {
            StepprActuatorState actuator = actuatorStates.get(joint.getActuators()[0]);
            jointStates.put(joint, new StepprLinearTransmissionJointState(joint.getSdfName(), joint.getRatio(), actuator, registry));            
         }
      }
      
      // Create ankles
      
      StepprAnkleJointState leftAnkle = new StepprAnkleJointState(RobotSide.LEFT, actuatorStates.get(StepprActuator.LEFT_ANKLE_RIGHT),
            actuatorStates.get(StepprActuator.LEFT_ANKLE_LEFT), registry);
      jointStates.put(StepprJoint.LEFT_ANKLE_Y, leftAnkle.ankleY());
      jointStates.put(StepprJoint.LEFT_ANKLE_X, leftAnkle.ankleX());
      
      StepprAnkleJointState rightAnkle = new StepprAnkleJointState(RobotSide.RIGHT, actuatorStates.get(StepprActuator.RIGHT_ANKLE_RIGHT),
            actuatorStates.get(StepprActuator.RIGHT_ANKLE_LEFT), registry);
      jointStates.put(StepprJoint.RIGHT_ANKLE_Y, rightAnkle.ankleY());
      jointStates.put(StepprJoint.RIGHT_ANKLE_X, rightAnkle.ankleX());
      
   }

   private void createActuators()
   {
      for(StepprActuator actuatorName : StepprActuator.values)
      {
         actuatorStates.put(actuatorName, new StepprActuatorState(actuatorName.getName(), actuatorName.getKt(), registry));
      }
   }
   
   public boolean update(ByteBuffer buffer, long timestamp) throws IOException
   {
      timeSincePreviousPacket.set(timestamp - lastReceivedTime.getLongValue());
      lastReceivedTime.set(timestamp);

      try
      {
         stateCollectionStartTime.set(buffer.getLong());
         stateCollectionFinishTime.set(buffer.getLong());
         for(StepprActuator actuatorName : StepprActuator.values)
         {
            actuatorStates.get(actuatorName).update(buffer);
         }
         powerDistributionState.update(buffer);
         xsens.update(buffer);
         
         for(StepprJoint joint : StepprJoint.values)
         {
            jointStates.get(joint).update();
         }
         
         return true;
         
      }
      catch(IOException e)
      {
         System.err.println(e.getMessage());
         corruptedPackets.increment();
         return false;
      }
   }
   
   
   public StepprJointState getJointState(StepprJoint joint)
   {
      return jointStates.get(joint);
   }
   
   public StepprXSensState getXSensState()
   {
      return xsens;
   }
}
