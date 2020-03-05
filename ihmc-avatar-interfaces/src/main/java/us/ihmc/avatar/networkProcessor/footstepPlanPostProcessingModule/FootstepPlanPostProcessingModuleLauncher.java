package us.ihmc.avatar.networkProcessor.footstepPlanPostProcessingModule;

import controller_msgs.msg.dds.FootstepPostProcessingPacket;
import controller_msgs.msg.dds.FootstepPostProcessingParametersPacket;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.ICPPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.footstepPlanning.postProcessing.parameters.FootstepPostProcessingParametersBasics;
import us.ihmc.pubsub.DomainFactory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.wholeBodyController.RobotContactPointParameters;

public class FootstepPlanPostProcessingModuleLauncher
{
   public static FootstepPlanPostProcessingModule createModule(DRCRobotModel robotModel)
   {
      String moduleName = robotModel.getSimpleRobotName();
      FootstepPostProcessingParametersBasics parameters = robotModel.getFootstepPostProcessingParameters();
      WalkingControllerParameters walkingControllerParameters = robotModel.getWalkingControllerParameters();
      RobotContactPointParameters<RobotSide> contactPointParameters = robotModel.getContactPointParameters();
      ICPPlannerParameters cmpPlannerParameters = robotModel.getCapturePointPlannerParameters();

      return new FootstepPlanPostProcessingModule(moduleName, parameters, walkingControllerParameters, contactPointParameters, cmpPlannerParameters);
   }

   public static FootstepPlanPostProcessingModule createModule(DRCRobotModel robotModel, DomainFactory.PubSubImplementation pubSubImplementation)
   {
      FootstepPlanPostProcessingModule postProcessingModule = createModule(robotModel);
      return setupForRos(postProcessingModule, pubSubImplementation);
   }

   public static FootstepPlanPostProcessingModule setupForRos(FootstepPlanPostProcessingModule postProcessingModule, DomainFactory.PubSubImplementation pubSubImplementation)
   {
      Ros2Node ros2Node = ROS2Tools.createRos2Node(pubSubImplementation, "footstep_post_processor");
      return setupForRos(postProcessingModule, ros2Node);
   }

   public static FootstepPlanPostProcessingModule setupForRos(FootstepPlanPostProcessingModule postProcessingModule, Ros2Node ros2Node)
   {
      postProcessingModule.registerRosNode(ros2Node);
      String name = postProcessingModule.getName();

      ROS2Tools.MessageTopicNameGenerator subscriberTopicNameGenerator = ROS2Tools
            .getTopicNameGenerator(name, ROS2Tools.FOOTSTEP_POSTPROCESSING_TOOLBOX, ROS2Tools.ROS2TopicQualifier.INPUT);
      ROS2Tools.MessageTopicNameGenerator publisherTopicNameGenerator = ROS2Tools
            .getTopicNameGenerator(name, ROS2Tools.FOOTSTEP_POSTPROCESSING_TOOLBOX, ROS2Tools.ROS2TopicQualifier.OUTPUT);

      // Parameters callback
      ROS2Tools.createCallbackSubscription(ros2Node, FootstepPostProcessingParametersPacket.class, subscriberTopicNameGenerator,
                                           s -> postProcessingModule.getParameters().set(s.readNextData()));

      // Planner request callback
      ROS2Tools.createCallbackSubscription(ros2Node, FootstepPostProcessingPacket.class, subscriberTopicNameGenerator, s -> {
         FootstepPostProcessingPacket requestPacket = s.takeNextData();
         new Thread(() -> postProcessingModule.handleRequestPacket(requestPacket)).start();
      });

      // Result publisher
      IHMCROS2Publisher<FootstepPostProcessingPacket> resultPublisher = ROS2Tools
            .createPublisher(ros2Node, FootstepPostProcessingPacket.class, publisherTopicNameGenerator);
      postProcessingModule.addStatusCallback(resultPublisher::publish);

      return postProcessingModule;
   }

}
