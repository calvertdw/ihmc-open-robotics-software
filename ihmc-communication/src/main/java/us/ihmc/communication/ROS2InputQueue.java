package us.ihmc.communication;

import us.ihmc.ros2.ROS2Callback;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.ros2.ROS2NodeInterface;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * To replace usages of RosQueuedSubscription.
 *
 * @param <T>
 */
public class ROS2InputQueue<T>
{
   private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
   private final ROS2Callback<T> ros2Callback;

   public ROS2InputQueue(ROS2NodeInterface ros2Node, Class<T> messageType, ROS2Topic topicName)
   {
      this(ros2Node, messageType, topicName.withTypeName(messageType).toString());
   }

   public ROS2InputQueue(ROS2NodeInterface ros2Node, Class<T> messageType, String topicName)
   {
      ros2Callback = new ROS2Callback<>(ros2Node, messageType, topicName, this::messageReceivedCallback);
   }

   private void messageReceivedCallback(T incomingData)
   {
      queue.add(incomingData);
   }

   public ConcurrentLinkedQueue getQueue()
   {
      return queue;
   }

   public void setEnabled(boolean enabled)
   {
      ros2Callback.setEnabled(enabled);
   }

   public void destroy()
   {
      ros2Callback.destroy();
   }
}
