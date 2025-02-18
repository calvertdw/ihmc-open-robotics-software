package us.ihmc.avatar.networkProcessor.kinemtaticsStreamingToolboxModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import controller_msgs.msg.dds.CapturabilityBasedStatus;
import controller_msgs.msg.dds.ControllerCrashNotificationPacket;
import controller_msgs.msg.dds.RobotConfigurationData;
import controller_msgs.msg.dds.WholeBodyStreamingMessage;
import controller_msgs.msg.dds.WholeBodyTrajectoryMessage;
import toolbox_msgs.msg.dds.KinematicsStreamingToolboxConfigurationMessage;
import toolbox_msgs.msg.dds.KinematicsStreamingToolboxInputMessage;
import toolbox_msgs.msg.dds.KinematicsToolboxConfigurationMessage;
import toolbox_msgs.msg.dds.KinematicsToolboxOutputStatus;
import toolbox_msgs.msg.dds.ToolboxStateMessage;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.modules.ToolboxController;
import us.ihmc.avatar.networkProcessor.modules.ToolboxModule;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.WholeBodySetpointParameters;
import us.ihmc.commons.Conversions;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.humanoidRobotics.communication.kinematicsStreamingToolboxAPI.KinematicsStreamingToolboxConfigurationCommand;
import us.ihmc.humanoidRobotics.communication.kinematicsStreamingToolboxAPI.KinematicsStreamingToolboxInputCommand;
import us.ihmc.humanoidRobotics.communication.kinematicsToolboxAPI.KinematicsToolboxConfigurationCommand;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotDataLogger.util.JVMStatisticsGenerator;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.ros2.ROS2NodeInterface;
import us.ihmc.ros2.ROS2Topic;

public class KinematicsStreamingToolboxModule extends ToolboxModule
{
   private static final int DEFAULT_UPDATE_PERIOD_MILLISECONDS = 5;

   protected final KinematicsStreamingToolboxController controller;
   private IHMCROS2Publisher<WholeBodyTrajectoryMessage> trajectoryMessagePublisher;
   private IHMCROS2Publisher<WholeBodyStreamingMessage> streamingMessagePublisher;

   public KinematicsStreamingToolboxModule(DRCRobotModel robotModel, boolean startYoVariableServer, PubSubImplementation pubSubImplementation)
   {
      this(robotModel, KinematicsStreamingToolboxParameters.defaultParameters(), startYoVariableServer, pubSubImplementation);
   }

   public KinematicsStreamingToolboxModule(DRCRobotModel robotModel,
                                           KinematicsStreamingToolboxParameters parameters,
                                           boolean startYoVariableServer,
                                           PubSubImplementation pubSubImplementation)
   {
      super(robotModel.getSimpleRobotName(), robotModel.createFullRobotModel(), robotModel.getLogModelProvider(), startYoVariableServer,
            DEFAULT_UPDATE_PERIOD_MILLISECONDS, pubSubImplementation);

      setTimeWithoutInputsBeforeGoingToSleep(3.0);
      controller = new KinematicsStreamingToolboxController(commandInputManager,
                                                            statusOutputManager,
                                                            parameters,
                                                            fullRobotModel,
                                                            robotModel,
                                                            robotModel.getControllerDT(),
                                                            Conversions.millisecondsToSeconds(updatePeriodMilliseconds),
                                                            yoGraphicsListRegistry,
                                                            registry);
      controller.setCollisionModel(robotModel.getHumanoidRobotKinematicsCollisionModel());
      Map<String, Double> initialConfiguration = fromStandPrep(robotModel);
      if (initialConfiguration != null)
         controller.setInitialRobotConfigurationNamedMap(initialConfiguration);
      controller.setTrajectoryMessagePublisher(trajectoryMessagePublisher::publish);
      controller.setStreamingMessagePublisher(streamingMessagePublisher::publish);
      startYoVariableServer();
      if (yoVariableServer != null)
      {
         JVMStatisticsGenerator jvmStatisticsGenerator = new JVMStatisticsGenerator(yoVariableServer);
         jvmStatisticsGenerator.start();
      }
   }

   private static Map<String, Double> fromStandPrep(DRCRobotModel robotModel)
   {
      WholeBodySetpointParameters standPrepParameters = robotModel.getHighLevelControllerParameters().getStandPrepParameters();
      if (standPrepParameters == null)
         return null;

      Map<String, Double> initialConfigurationMap = new HashMap<>();
      FullHumanoidRobotModel fullRobotModel = robotModel.createFullRobotModel();

      for (OneDoFJointBasics joint : fullRobotModel.getOneDoFJoints())
      {
         String jointName = joint.getName();
         initialConfigurationMap.put(jointName, standPrepParameters.getSetpoint(jointName));
      }
      return initialConfigurationMap;
   }

   @Override
   public void registerExtraPuSubs(ROS2NodeInterface ros2Node)
   {
      ROS2Topic<?> controllerInputTopic = ROS2Tools.getControllerInputTopic(robotName);
      ROS2Topic<?> controllerOutputTopic = ROS2Tools.getControllerOutputTopic(robotName);

      trajectoryMessagePublisher = ROS2Tools.createPublisherTypeNamed(ros2Node, WholeBodyTrajectoryMessage.class, controllerInputTopic);
      streamingMessagePublisher = ROS2Tools.createPublisherTypeNamed(ros2Node, WholeBodyStreamingMessage.class, controllerInputTopic);

      RobotConfigurationData robotConfigurationData = new RobotConfigurationData();

      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node, RobotConfigurationData.class, controllerOutputTopic, s ->
      {
         if (controller != null)
         {
            s.takeNextData(robotConfigurationData, null);
            controller.updateRobotConfigurationData(robotConfigurationData);
         }
      });

      CapturabilityBasedStatus capturabilityBasedStatus = new CapturabilityBasedStatus();

      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node, CapturabilityBasedStatus.class, controllerOutputTopic, s ->
      {
         if (controller != null)
         {
            s.takeNextData(capturabilityBasedStatus, null);
            controller.updateCapturabilityBasedStatus(capturabilityBasedStatus);
         }
      });
   }

   @Override
   public ToolboxController getToolboxController()
   {
      return controller;
   }

   @Override
   public List<Class<? extends Command<?, ?>>> createListOfSupportedCommands()
   {
      return supportedCommands();
   }

   public static List<Class<? extends Command<?, ?>>> supportedCommands()
   {
      List<Class<? extends Command<?, ?>>> commands = new ArrayList<>();
      commands.add(KinematicsStreamingToolboxInputCommand.class);
      commands.add(KinematicsStreamingToolboxConfigurationCommand.class);
      commands.add(KinematicsToolboxConfigurationCommand.class);
      return commands;
   }

   @Override
   public List<Class<? extends Settable<?>>> createListOfSupportedStatus()
   {
      return supportedStatus();
   }

   public static List<Class<? extends Settable<?>>> supportedStatus()
   {
      List<Class<? extends Settable<?>>> status = new ArrayList<>();
      status.add(KinematicsToolboxOutputStatus.class);
      status.add(ControllerCrashNotificationPacket.class);
      return status;
   }

   @Override
   public ROS2Topic<?> getOutputTopic()
   {
      return getOutputTopic(robotName);
   }

   public static ROS2Topic<?> getOutputTopic(String robotName)
   {
      return ROS2Tools.KINEMATICS_STREAMING_TOOLBOX.withRobot(robotName).withOutput();
   }

   @Override
   public ROS2Topic<?> getInputTopic()
   {
      return getInputTopic(robotName);
   }

   public static ROS2Topic<?> getInputTopic(String robotName)
   {
      return ROS2Tools.KINEMATICS_STREAMING_TOOLBOX.withRobot(robotName).withInput();
   }

   public static ROS2Topic<ToolboxStateMessage> getInputStateTopic(String robotName)
   {
      return getInputTopic(robotName).withTypeName(ToolboxStateMessage.class);
   }

   public static ROS2Topic<KinematicsStreamingToolboxInputMessage> getInputCommandTopic(String robotName)
   {
      return getInputTopic(robotName).withTypeName(KinematicsStreamingToolboxInputMessage.class);
   }

   public static ROS2Topic<KinematicsStreamingToolboxConfigurationMessage> getInputStreamingConfigurationTopic(String robotName)
   {
      return getInputTopic(robotName).withTypeName(KinematicsStreamingToolboxConfigurationMessage.class);
   }

   public static ROS2Topic<KinematicsToolboxConfigurationMessage> getInputToolboxConfigurationTopic(String robotName)
   {
      return getInputTopic(robotName).withTypeName(KinematicsToolboxConfigurationMessage.class);
   }

   public static ROS2Topic<KinematicsToolboxOutputStatus> getOutputStatusTopic(String robotName)
   {
      return getOutputTopic(robotName).withTypeName(KinematicsToolboxOutputStatus.class);
   }

   public static ROS2Topic<ControllerCrashNotificationPacket> getOutputCrashNotificationTopic(String robotName)
   {
      return getOutputTopic(robotName).withTypeName(ControllerCrashNotificationPacket.class);
   }
}
