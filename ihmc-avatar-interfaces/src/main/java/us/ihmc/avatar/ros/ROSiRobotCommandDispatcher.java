package us.ihmc.avatar.ros;

import java.net.URI;
import java.net.URISyntaxException;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import controller_msgs.msg.dds.HandDesiredConfigurationMessage;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.humanoidRobotics.communication.subscribers.HandDesiredConfigurationMessageSubscriber;
import us.ihmc.ros2.RealtimeRos2Node;
import us.ihmc.utilities.ros.RosTools;

public class ROSiRobotCommandDispatcher implements Runnable
{
   private final HandDesiredConfigurationMessageSubscriber handDesiredConfigurationMessageSubscriber = new HandDesiredConfigurationMessageSubscriber(null);

   private final ROSiRobotCommunicator rosHandCommunicator;

   public ROSiRobotCommandDispatcher(String robotName, RealtimeRos2Node realtimeRos2Node, String rosHostIP)
   {
      ROS2Tools.createCallbackSubscription(realtimeRos2Node, HandDesiredConfigurationMessage.class,
                                           ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotName), handDesiredConfigurationMessageSubscriber);

      String rosURI = "http://" + rosHostIP + ":11311";

      rosHandCommunicator = new ROSiRobotCommunicator(rosURI);

      try
      {
         NodeConfiguration nodeConfiguration = RosTools.createNodeConfiguration(new URI(rosURI));
         NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
         nodeMainExecutor.execute(rosHandCommunicator, nodeConfiguration);
      }
      catch (URISyntaxException e)
      {
         e.printStackTrace();
      }
   }

   @Override
   public void run()
   {
      while (true)
      {
         if (handDesiredConfigurationMessageSubscriber.isNewDesiredConfigurationAvailable())
         {
            HandDesiredConfigurationMessage ihmcMessage = handDesiredConfigurationMessageSubscriber.pollMessage();
            rosHandCommunicator.sendHandCommand(ihmcMessage);
         }
      }
   }
}
