package us.ihmc.sensorProcessing.communication.producers;

import java.util.List;

import controller_msgs.msg.dds.IMUPacket;
import controller_msgs.msg.dds.RobotConfigurationData;
import controller_msgs.msg.dds.SpatialVectorMessage;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.robotics.robotController.RawOutputWriter;
import us.ihmc.robotics.sensors.ForceSensorDataReadOnly;
import us.ihmc.ros2.RealtimeRos2Node;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.RobotConfigurationDataFactory;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.sensorProcessing.sensorProcessors.FloatingJointStateReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.OneDoFJointStateReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorTimestampHolder;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class RobotConfigurationDataPublisher implements RawOutputWriter
{
   private final FloatingJointStateReadOnly rootJointSensorData;
   private final List<? extends OneDoFJointStateReadOnly> jointSensorData;
   private final List<? extends IMUSensorReadOnly> imuSensorData;
   private final List<? extends ForceSensorDataReadOnly> forceSensorData;
   private final SensorTimestampHolder timestampHolder;
   private final RobotMotionStatusHolder robotMotionStatusHolder;

   private final RobotConfigurationData robotConfigurationData = new RobotConfigurationData();
   private final IHMCRealtimeROS2Publisher<RobotConfigurationData> robotConfigurationDataPublisher;

   public RobotConfigurationDataPublisher(RealtimeRos2Node realtimeRos2Node, MessageTopicNameGenerator publisherTopicNameGenerator,
                                          FloatingJointStateReadOnly rootJointSensorData, List<? extends OneDoFJointStateReadOnly> jointSensorData,
                                          List<? extends IMUSensorReadOnly> imuSensorData, List<? extends ForceSensorDataReadOnly> forceSensorData,
                                          SensorTimestampHolder timestampHolder, RobotMotionStatusHolder robotMotionStatusHolder)
   {
      this.rootJointSensorData = rootJointSensorData;
      this.jointSensorData = jointSensorData;
      this.imuSensorData = imuSensorData;
      this.forceSensorData = forceSensorData;
      this.timestampHolder = timestampHolder;
      this.robotMotionStatusHolder = robotMotionStatusHolder;

      robotConfigurationData.setJointNameHash(RobotConfigurationDataFactory.calculateJointNameHash(jointSensorData, forceSensorData, imuSensorData));
      robotConfigurationDataPublisher = ROS2Tools.createPublisher(realtimeRos2Node, RobotConfigurationData.class, publisherTopicNameGenerator);
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public void write()
   {
      // Write timestamps
      robotConfigurationData.setWallTime(timestampHolder.getWallTime());
      robotConfigurationData.setMonotonicTime(timestampHolder.getMonotonicTime());
      robotConfigurationData.setSyncTimestamp(timestampHolder.getSyncTimestamp());

      // Write root joint data
      robotConfigurationData.getRootOrientation().set(rootJointSensorData.getPose().getOrientation());
      robotConfigurationData.getRootTranslation().set(rootJointSensorData.getPose().getPosition());
      robotConfigurationData.getPelvisAngularVelocity().set(rootJointSensorData.getTwist().getAngularPart());
      robotConfigurationData.getPelvisLinearVelocity().set(rootJointSensorData.getTwist().getLinearPart());
      robotConfigurationData.getPelvisLinearAcceleration().set(rootJointSensorData.getAcceleration().getLinearPart());

      // Write 1-DoF joint data
      robotConfigurationData.getJointAngles().reset();
      robotConfigurationData.getJointVelocities().reset();
      robotConfigurationData.getJointTorques().reset();

      for (int i = 0; i < jointSensorData.size(); i++)
      {
         OneDoFJointStateReadOnly jointSensorOutput = jointSensorData.get(i);
         robotConfigurationData.getJointAngles().add((float) jointSensorOutput.getPosition());
         robotConfigurationData.getJointVelocities().add((float) jointSensorOutput.getVelocity());
         robotConfigurationData.getJointTorques().add((float) jointSensorOutput.getEffort());
      }

      // Write IMU sensor data
      if (imuSensorData != null)
      {
         robotConfigurationData.getImuSensorData().clear();

         for (int i = 0; i < imuSensorData.size(); i++)
         {
            IMUSensorReadOnly imuSensor = imuSensorData.get(i);
            IMUPacket imuPacketToPack = robotConfigurationData.getImuSensorData().add();

            imuPacketToPack.getOrientation().set(imuSensor.getOrientationMeasurement());
            imuPacketToPack.getAngularVelocity().set(imuSensor.getAngularVelocityMeasurement());
            imuPacketToPack.getLinearAcceleration().set(imuSensor.getLinearAccelerationMeasurement());
         }
      }

      // Write force sensor data
      if (forceSensorData != null)
      {
         robotConfigurationData.getForceSensorData().clear();

         for (int i = 0; i < forceSensorData.size(); i++)
         {
            SpatialVectorMessage forceDataToPack = robotConfigurationData.getForceSensorData().add();
            forceSensorData.get(i).getWrench(forceDataToPack.getAngularPart(), forceDataToPack.getLinearPart());
         }
      }

      // Write robot motion status
      if (robotMotionStatusHolder != null)
         robotConfigurationData.setRobotMotionStatus(robotMotionStatusHolder.getCurrentRobotMotionStatus().toByte());

      // Write last packet info, fill up with -1 since this information is gone since the switch to RTPS
      robotConfigurationData.setLastReceivedPacketTypeId(-1);
      robotConfigurationData.setLastReceivedPacketUniqueId(-1);
      robotConfigurationData.setLastReceivedPacketRobotTimestamp(-1);

      robotConfigurationDataPublisher.publish(robotConfigurationData);
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return null;
   }
}
