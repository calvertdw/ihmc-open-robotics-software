package us.ihmc.sensorProcessing.communication.producers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import controller_msgs.msg.dds.RobotConfigurationData;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.log.LogTools;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotModels.FullRobotModelUtils;
import us.ihmc.robotics.sensors.ForceSensorData;
import us.ihmc.robotics.sensors.ForceSensorDataHolderReadOnly;
import us.ihmc.robotics.sensors.ForceSensorDataReadOnly;
import us.ihmc.robotics.sensors.ForceSensorDefinition;
import us.ihmc.robotics.sensors.IMUDefinition;
import us.ihmc.ros2.RealtimeRos2Node;
import us.ihmc.sensorProcessing.imu.IMUSensor;
import us.ihmc.sensorProcessing.model.RobotMotionStatus;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.sensorProcessing.sensorProcessors.FloatingJointStateReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.OneDoFJointStateReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorTimestampHolder;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.tools.factories.FactoryTools;
import us.ihmc.tools.factories.OptionalFactoryField;
import us.ihmc.tools.factories.RequiredFactoryField;

public class RobotConfigurationDataPublisherFactory
{
   private final RequiredFactoryField<List<? extends OneDoFJointStateReadOnly>> oneDoFJointSensorData = new RequiredFactoryField<>("oneDoFJointSensorData");
   private final OptionalFactoryField<FloatingJointStateReadOnly> rootJointSensorData = new OptionalFactoryField<>("rootJointSensorData");
   private final OptionalFactoryField<List<? extends IMUSensorReadOnly>> imuSensorData = new OptionalFactoryField<>("imuSensorData");
   private final OptionalFactoryField<ForceSensorDataHolderReadOnly> forceSensorDataHolder = new OptionalFactoryField<>("forceSensorDataHolder");
   private final OptionalFactoryField<SensorTimestampHolder> timestampHolder = new OptionalFactoryField<>("timestampHolder");

   private final RequiredFactoryField<OneDoFJointBasics[]> jointsField = new RequiredFactoryField<>("joints");
   private final OptionalFactoryField<IMUDefinition[]> imuDefinitionsField = new OptionalFactoryField<>("imuDefinitions");
   private final OptionalFactoryField<ForceSensorDefinition[]> forceSensorDefinitionsField = new OptionalFactoryField<>("forceSensorDefinitions");

   private final OptionalFactoryField<RobotMotionStatusHolder> robotMotionStatusHolderField = new OptionalFactoryField<>("robotMotionStatusHolder");

   private final RequiredFactoryField<RealtimeRos2Node> realtimeRos2NodeField = new RequiredFactoryField<>("realtimeRos2Node");
   private final RequiredFactoryField<MessageTopicNameGenerator> publisherTopicNameGeneratorField = new RequiredFactoryField<>("publisherTopicNameGenerator");

   public RobotConfigurationDataPublisherFactory()
   {
      imuDefinitionsField.setDefaultValue(new IMUDefinition[0]);
      forceSensorDefinitionsField.setDefaultValue(new ForceSensorDefinition[0]);

      robotMotionStatusHolderField.setDefaultValue(new RobotMotionStatusHolder(RobotMotionStatus.UNKNOWN));
   }

   /**
    * Sets the sensor source to use to get the data to publish:
    * <ul>
    * <li>joint states are obtained from the {@code fullRobotModel}.
    * <li>timestamps, force sensor data, and IMU data are obtained from {@code sensorOutput}.
    * </ul>
    * 
    * @param fullRobotModel to get joint state.
    * @param sensorOutput   to get timestamps, force sensor data, and IMU data.
    */
   public void setSensorSource(FullRobotModel fullRobotModel, SensorOutputMapReadOnly sensorOutput)
   {
      setSensorSource(fullRobotModel, sensorOutput.getForceSensorOutputs(), sensorOutput);
   }

   /**
    * Sets the sensor source to use to get the data to publish:
    * <ul>
    * <li>joint states are obtained from the {@code fullRobotModel}.
    * <li>force sensor data from the given {@code forceSensorDataHolderToSend}.
    * <li>timestamps and IMU data are obtained from {@code sensorOutput}.
    * </ul>
    * 
    * @param fullRobotModel              to get joint state.
    * @param forceSensorDataHolderToSend to get force sensor data.
    * @param sensorOutput                to get timestamps and IMU data.
    */
   public void setSensorSource(FullRobotModel fullRobotModel, ForceSensorDataHolderReadOnly forceSensorDataHolderToSend, SensorOutputMapReadOnly sensorOutput)
   {
      FloatingJointStateReadOnly rootJointStateOutput = FloatingJointStateReadOnly.fromFloatingJoint(fullRobotModel.getRootJoint());
      List<OneDoFJointStateReadOnly> jointSensorOutputs = new ArrayList<>();

      for (OneDoFJointBasics joint : fullRobotModel.getOneDoFJoints())
      {
         OneDoFJointStateReadOnly oneDoFJointOutput = sensorOutput.getOneDoFJointOutput(joint);
         if (oneDoFJointOutput == null)
            jointSensorOutputs.add(OneDoFJointStateReadOnly.createFromOneDoFJoint(joint, true));
         else
            jointSensorOutputs.add(OneDoFJointStateReadOnly.createFromOneDoFJoint(joint, oneDoFJointOutput::isJointEnabled));
      }
      setSensorSource(sensorOutput, rootJointStateOutput, jointSensorOutputs, forceSensorDataHolderToSend, sensorOutput.getIMUOutputs());
   }

   /**
    * Sets the sensor source to use to get the data to publish.
    * 
    * @param sensorTimestampHolder to get timestamps.
    * @param rootJointStateOutput  to get root joint state.
    * @param jointSensorOutputs    to get the 1-DoF joint states.
    * @param forceSensorDataHolder to get the force sensor data.
    * @param imuSensorOutputs      to get the IMU data.
    */
   public void setSensorSource(SensorTimestampHolder sensorTimestampHolder, FloatingJointStateReadOnly rootJointStateOutput,
                               List<? extends OneDoFJointStateReadOnly> jointSensorOutputs, ForceSensorDataHolderReadOnly forceSensorDataHolder,
                               List<? extends IMUSensorReadOnly> imuSensorOutputs)
   {
      oneDoFJointSensorData.set(jointSensorOutputs);
      rootJointSensorData.set(rootJointStateOutput);
      imuSensorData.set(imuSensorOutputs);
      this.forceSensorDataHolder.set(forceSensorDataHolder);
      timestampHolder.set(sensorTimestampHolder);
   }

   /**
    * Extracts and sets the sensor definitions from the given {@code fullRobotModel}.
    * <p>
    * Note that the finger joints are excluded by default.
    * </p>
    * 
    * @param fullHumanoidRobotModel the instance to get the definitions from.
    * @see #setDefinitionsToPublish(OneDoFJointBasics[], ForceSensorDefinition[], IMUDefinition[])
    */
   public void setDefinitionsToPublish(FullHumanoidRobotModel fullHumanoidRobotModel)
   {
      setDefinitionsToPublish(FullRobotModelUtils.getAllJointsExcludingHands(fullHumanoidRobotModel),
                              fullHumanoidRobotModel.getForceSensorDefinitions(),
                              fullHumanoidRobotModel.getIMUDefinitions());
   }

   /**
    * Extracts and sets the sensor definitions from the given {@code fullRobotModel}.
    * 
    * @param fullRobotModel the instance to get the definitions from.
    * @see #setDefinitionsToPublish(OneDoFJointBasics[], ForceSensorDefinition[], IMUDefinition[])
    */
   public void setDefinitionsToPublish(FullRobotModel fullRobotModel)
   {
      setDefinitionsToPublish(fullRobotModel.getOneDoFJoints(), fullRobotModel.getForceSensorDefinitions(), fullRobotModel.getIMUDefinitions());
   }

   /**
    * Specifies for which sensors the data should be sent.
    * 
    * @param joints                 only the data for the given joints will be published.
    * @param forceSensorDefinitions only the data for the given force sensors will be published.
    * @param imuDefinitions         only the data for the given IMUs will be published.
    */
   public void setDefinitionsToPublish(OneDoFJointBasics[] joints, ForceSensorDefinition[] forceSensorDefinitions, IMUDefinition[] imuDefinitions)
   {
      jointsField.set(joints);
      forceSensorDefinitionsField.set(forceSensorDefinitions);
      imuDefinitionsField.set(imuDefinitions);
   }

   /**
    * Sets the robot motion status holder that will be used to update the corresponding field in
    * {@link RobotConfigurationData#robot_motion_status_}.
    * 
    * @param robotMotionStatusHolder the status holder to use for publishing data.
    */
   public void setRobotMotionStatusHolder(RobotMotionStatusHolder robotMotionStatusHolder)
   {
      robotMotionStatusHolderField.set(robotMotionStatusHolder);
   }

   /**
    * ROS 2 necessary information to create the real-time publisher.
    * 
    * @param ros2Node                    the real-time node to create the publisher with.
    * @param publisherTopicNameGenerator the generator to use for creating the topic name.
    */
   public void setROS2Info(RealtimeRos2Node ros2Node, MessageTopicNameGenerator publisherTopicNameGenerator)
   {
      realtimeRos2NodeField.set(ros2Node);
      publisherTopicNameGeneratorField.set(publisherTopicNameGenerator);
   }

   /**
    * Instantiates a new publisher and disposes of this factory.
    * 
    * @return the new publisher ready to use.
    */
   public RobotConfigurationDataPublisher createRobotConfigurationDataPublisher()
   {
      FactoryTools.checkAllFactoryFieldsAreSet(this);

      List<OneDoFJointStateReadOnly> jointSensorDataToPublish = filterJointSensorDataToPublish();
      List<IMUSensorReadOnly> imuSensorDataToPublish = filterIMUSensorDataToPublish();
      List<ForceSensorDataReadOnly> forceSensorDataToPublish = filterForceSensorDataToPublish();

      RobotConfigurationDataPublisher publisher = new RobotConfigurationDataPublisher(realtimeRos2NodeField.get(),
                                                                                      publisherTopicNameGeneratorField.get(),
                                                                                      rootJointSensorData.get(),
                                                                                      jointSensorDataToPublish,
                                                                                      imuSensorDataToPublish,
                                                                                      forceSensorDataToPublish,
                                                                                      timestampHolder.get(),
                                                                                      robotMotionStatusHolderField.get());

      FactoryTools.disposeFactory(this);

      return publisher;
   }

   private List<OneDoFJointStateReadOnly> filterJointSensorDataToPublish()
   {
      List<OneDoFJointStateReadOnly> sensorDataToPublish = new ArrayList<>();

      OneDoFJointBasics[] jointSelection = jointsField.get();
      List<? extends OneDoFJointStateReadOnly> allSensorData = oneDoFJointSensorData.get();

      for (OneDoFJointBasics joint : jointSelection)
      {
         Optional<? extends OneDoFJointStateReadOnly> sensorDataOptional = allSensorData.stream()
                                                                                        .filter(sensorData -> sensorData.getJointName().equals(joint.getName()))
                                                                                        .findFirst();

         if (sensorDataOptional.isPresent())
         {
            sensorDataToPublish.add(sensorDataOptional.get());
         }
         else
         {
            sensorDataToPublish.add(OneDoFJointStateReadOnly.dummyOneDoFJointState(joint.getName()));
            LogTools.warn("Could not find sensor data for joint: " + joint.getName());
         }
      }
      return sensorDataToPublish;
   }

   private List<IMUSensorReadOnly> filterIMUSensorDataToPublish()
   {
      List<IMUSensorReadOnly> sensorDataToPublish = new ArrayList<>();

      IMUDefinition[] imuSelection = imuDefinitionsField.get();
      List<? extends IMUSensorReadOnly> allSensorData = imuSensorData.get();

      for (IMUDefinition imu : imuSelection)
      {
         Optional<? extends IMUSensorReadOnly> sensorDataOptional = allSensorData.stream()
                                                                                 .filter(sensorData -> sensorData.getSensorName().equals(imu.getName()))
                                                                                 .findFirst();

         if (sensorDataOptional.isPresent())
         {
            sensorDataToPublish.add(sensorDataOptional.get());
         }
         else
         {
            sensorDataToPublish.add(new IMUSensor(imu, null));
            LogTools.warn("Could not find sensor data for the IMU: " + imu.getName());
         }
      }
      return sensorDataToPublish;
   }

   private List<ForceSensorDataReadOnly> filterForceSensorDataToPublish()
   {
      List<ForceSensorDataReadOnly> sensorDataToPublish = new ArrayList<>();

      ForceSensorDefinition[] forceSensorSelection = forceSensorDefinitionsField.get();
      ForceSensorDataHolderReadOnly allSensorData = forceSensorDataHolder.get();

      for (ForceSensorDefinition forceSensor : forceSensorSelection)
      {
         ForceSensorDataReadOnly sensorData = allSensorData.get(forceSensor);

         if (sensorData != null)
         {
            sensorDataToPublish.add(sensorData);
         }
         else
         {
            ForceSensorData dummySensor = new ForceSensorData();
            dummySensor.setDefinition(forceSensor);
            sensorDataToPublish.add(dummySensor);
            LogTools.warn("Could not find sensor data for the F/T sensor: " + forceSensor.getSensorName());
         }
      }
      return sensorDataToPublish;
   }
}
