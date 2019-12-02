package us.ihmc.avatar.networkProcessor.kinematicsToolboxModule;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.initialSetup.DRCRobotInitialSetup;
import us.ihmc.avatar.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.avatar.networkProcessor.DRCNetworkProcessor;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.Conversions;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.robotics.sensors.ForceSensorDataHolderReadOnly;
import us.ihmc.robotics.sensors.ForceSensorDefinition;
import us.ihmc.ros2.RealtimeRos2Node;
import us.ihmc.sensorProcessing.communication.producers.DRCPoseCommunicator;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.sensorProcessing.parameters.HumanoidRobotSensorInformation;
import us.ihmc.sensorProcessing.sensorData.JointConfigurationGatherer;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.simulatedSensors.SDFPerfectSimulatedSensorReader;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.simulationConstructionSetTools.util.HumanoidFloatingRootJointRobot;
import us.ihmc.util.PeriodicNonRealtimeThreadScheduler;
import us.ihmc.wholeBodyController.DRCRobotJointMap;

public class KinematicToolboxDiagnosticEnvironment
{
   private final String threadName = "NonRealtimeScheduler";
   private final RealtimeRos2Node realtimeRos2Node = ROS2Tools.createRealtimeRos2Node(PubSubImplementation.INTRAPROCESS, "ihmc_fake_controller");

   public KinematicToolboxDiagnosticEnvironment(DRCRobotModel drcRobotModel)
   {
      FullHumanoidRobotModel humanoidFullRobotModel = drcRobotModel.createFullRobotModel();
      DRCRobotJointMap jointMap = drcRobotModel.getJointMap();
      HumanoidFloatingRootJointRobot humanoidFloatingRobotModel = drcRobotModel.createHumanoidFloatingRootJointRobot(false);
      DRCRobotInitialSetup<HumanoidFloatingRootJointRobot> robotInitialSetup = drcRobotModel.getDefaultRobotInitialSetup(0.0, 0.0);
      robotInitialSetup.initializeRobot(humanoidFloatingRobotModel, jointMap);
      SDFPerfectSimulatedSensorReader sdfPerfectReader = new SDFPerfectSimulatedSensorReader(humanoidFloatingRobotModel, humanoidFullRobotModel, null);
      sdfPerfectReader.read();

      ForceSensorDefinition[] forceSensorDefinitionArray = humanoidFullRobotModel.getForceSensorDefinitions();
      List<ForceSensorDefinition> forceSensorDefinitionList = Arrays.asList(forceSensorDefinitionArray);
      ForceSensorDataHolder forceSensorDataHolder = new ForceSensorDataHolder(forceSensorDefinitionList);
      JointConfigurationGatherer jointConfigurationGatherer = new JointConfigurationGatherer(humanoidFullRobotModel, forceSensorDataHolder);

      SensorOutputMapReadOnly sensorOutputMapReadOnly = initializeSensorOutputMapReadOnly();
      RobotMotionStatusHolder robotMotionStatusFromController = new RobotMotionStatusHolder();
      HumanoidRobotSensorInformation sensorInformation = drcRobotModel.getSensorInformation();
      MessageTopicNameGenerator publisherTopicNameGenerator = ControllerAPIDefinition.getPublisherTopicNameGenerator(drcRobotModel.getSimpleRobotName());
      final DRCPoseCommunicator poseCommunicator = new DRCPoseCommunicator(humanoidFullRobotModel, jointConfigurationGatherer, publisherTopicNameGenerator,
                                                                           realtimeRos2Node, sensorOutputMapReadOnly, sensorOutputMapReadOnly,
                                                                           robotMotionStatusFromController, sensorInformation);
      PeriodicNonRealtimeThreadScheduler scheduler2 = new PeriodicNonRealtimeThreadScheduler(threadName);
      scheduler2.schedule(new Runnable()
      {
         @Override
         public void run()
         {
            poseCommunicator.write();
         }
      }, 1, TimeUnit.MILLISECONDS);

      DRCNetworkModuleParameters parameters = new DRCNetworkModuleParameters();
      parameters.enableNetworkProcessor(true);
      parameters.enableUiModule(true);
      parameters.enableKinematicsToolbox(true);
      parameters.enableKinematicsToolboxVisualizer(true);
      parameters.enableLocalControllerCommunicator(true);
      parameters.setEnableJoystickBasedStepping(true);
      new DRCNetworkProcessor(drcRobotModel, parameters);
   }

   private long timestamp = 0L;

   private SensorOutputMapReadOnly initializeSensorOutputMapReadOnly()
   {
      return new SensorOutputMapReadOnly()
      {
         @Override
         public long getWallTime()
         {
            timestamp += Conversions.millisecondsToNanoseconds(1L);
            return timestamp;
         }

         @Override
         public long getMonotonicTime()
         {
            return timestamp;
         }

         @Override
         public long getSyncTimestamp()
         {
            return timestamp;
         }

         @Override
         public boolean isJointEnabled(OneDoFJointBasics oneDoFJoint)
         {
            return false;
         }

         @Override
         public double getJointVelocityOutput(OneDoFJointBasics oneDoFJoint)
         {
            return 0;
         }

         @Override
         public double getJointTauOutput(OneDoFJointBasics oneDoFJoint)
         {
            return 0;
         }

         @Override
         public double getJointPositionOutput(OneDoFJointBasics oneDoFJoint)
         {
            return 0;
         }

         @Override
         public double getJointAccelerationOutput(OneDoFJointBasics oneDoFJoint)
         {
            return 0;
         }

         @Override
         public List<? extends IMUSensorReadOnly> getIMUOutputs()
         {
            return null;
         }

         @Override
         public ForceSensorDataHolderReadOnly getForceSensorOutputs()
         {
            return null;
         }
      };
   }

}
