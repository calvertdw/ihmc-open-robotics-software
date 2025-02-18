package us.ihmc.avatar.ros2;

import controller_msgs.msg.dds.RobotConfigurationData;
import controller_msgs.msg.dds.WalkingStatusMessage;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.thread.Notification;
import us.ihmc.communication.IHMCROS2Input;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ros2.ROS2ControllerPublishSubscribeAPI;
import us.ihmc.communication.ros2.ROS2Helper;
import us.ihmc.ros2.ROS2NodeInterface;
import us.ihmc.ros2.ROS2Topic;

import java.util.function.Consumer;
import java.util.function.Function;

public class ROS2ControllerHelper extends ROS2Helper implements ROS2ControllerPublishSubscribeAPI
{
   protected final ROS2ControllerPublisherMap ros2ControllerPublisherMap;
   private final String simpleRobotName;

   public ROS2ControllerHelper(ROS2NodeInterface ros2Node, DRCRobotModel robotModel)
   {
      this(ros2Node, robotModel.getSimpleRobotName());
   }

   public ROS2ControllerHelper(ROS2NodeInterface ros2Node, String simpleRobotName)
   {
      super(ros2Node);
      this.simpleRobotName = simpleRobotName;
      ros2ControllerPublisherMap = new ROS2ControllerPublisherMap(simpleRobotName, ros2PublisherMap);
   }

   @Override
   public <T> void subscribeViaCallback(Function<String, ROS2Topic<T>> topicFunction, Consumer<T> callback)
   {
      subscribeViaCallback(topicFunction.apply(simpleRobotName), callback);
   }

   @Override
   public void publishToController(Object message)
   {
      ros2ControllerPublisherMap.publish(message);
   }

   @Override
   public <T> void publish(Function<String, ROS2Topic<T>> topic, T message)
   {
      publish(topic.apply(simpleRobotName), message);
   }

   @Override
   public <T> IHMCROS2Input<T> subscribeToController(Class<T> messageClass)
   {
      return subscribe(ControllerAPIDefinition.getTopic(messageClass, simpleRobotName));
   }

   @Override
   public IHMCROS2Input<RobotConfigurationData> subscribeToRobotConfigurationData()
   {
      return subscribe(ROS2Tools.getRobotConfigurationDataTopic(getRobotName()));
   }

   @Override
   public <T> void subscribeToControllerViaCallback(Class<T> messageClass, Consumer<T> callback)
   {
      subscribeViaCallback(ControllerAPIDefinition.getTopic(messageClass, simpleRobotName), callback);
   }

   @Override
   public Notification subscribeToWalkingCompletedViaNotification()
   {
      Notification notification = new Notification();
      subscribeToControllerViaCallback(WalkingStatusMessage.class, walkingStatusMessage -> {
         if (walkingStatusMessage.getWalkingStatus() == WalkingStatusMessage.COMPLETED)
         {
            notification.set();
         }
      });
      return notification;
   }

   @Override
   public String getRobotName()
   {
      return simpleRobotName;
   }
}
