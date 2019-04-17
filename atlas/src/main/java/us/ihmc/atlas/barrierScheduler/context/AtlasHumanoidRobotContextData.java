package us.ihmc.atlas.barrierScheduler.context;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.realtime.barrierScheduler.context.HumanoidRobotContextData;
import us.ihmc.realtime.barrierScheduler.context.HumanoidRobotContextJointData;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputList;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;

public class AtlasHumanoidRobotContextData extends HumanoidRobotContextData
{
   private final RawJointSensorDataHolderMap rawJointSensorDataHolderMap;
   private final List<String> jointNames;

   protected AtlasHumanoidRobotContextData(HumanoidRobotContextJointData processedJointData, ForceSensorDataHolder forceSensorDataHolder,
                                           CenterOfPressureDataHolder centerOfPressureDataHolder, RobotMotionStatusHolder robotMotionStatusHolder,
                                           JointDesiredOutputList jointDesiredOutputList, RawJointSensorDataHolderMap rawJointSensorDataHolderMap)
   {
      super(processedJointData, forceSensorDataHolder, centerOfPressureDataHolder, robotMotionStatusHolder, jointDesiredOutputList);
      this.rawJointSensorDataHolderMap = rawJointSensorDataHolderMap;
      jointNames = new ArrayList<>(rawJointSensorDataHolderMap.keySet());
   }

   @Override
   public void copyFrom(HumanoidRobotContextData src)
   {
      super.copyFrom(src);

      AtlasHumanoidRobotContextData atlasSrc = (AtlasHumanoidRobotContextData) src;
      for (int i = 0; i < jointNames.size(); i++)
      {
         String jointName = jointNames.get(i);
         rawJointSensorDataHolderMap.get(jointName).set(atlasSrc.rawJointSensorDataHolderMap.get(jointName));
      }
   }
}
