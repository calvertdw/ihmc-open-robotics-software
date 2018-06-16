package us.ihmc.humanoidBehaviors.behaviors.primitives;

import controller_msgs.msg.dds.HighLevelStateMessage;
import controller_msgs.msg.dds.HighLevelStateMessagePubSubType;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.yoVariables.variable.YoBoolean;

public class HighLevelStateBehavior extends AbstractBehavior
{
   private final YoBoolean packetHasBeenSent = new YoBoolean("packetHasBeenSent" + behaviorName, registry);
   private HighLevelStateMessage outgoingHighLevelStatePacket;
   private IHMCROS2Publisher<HighLevelStateMessage> publisher;

   public HighLevelStateBehavior(Ros2Node ros2Node)
   {
      super(ros2Node);
      publisher = createPublisher(new HighLevelStateMessagePubSubType(), "/ihmc/high_level_state");
   }

   public void setInput(HighLevelStateMessage highLevelStatePacket)
   {
      this.outgoingHighLevelStatePacket = highLevelStatePacket;
   }

   @Override
   public void doControl()
   {
      if (!packetHasBeenSent.getBooleanValue() && (outgoingHighLevelStatePacket != null))
      {
         sendPacketToController();
      }
   }

   private void sendPacketToController()
   {
      if (!isPaused.getBooleanValue() && !isAborted.getBooleanValue())
      {
         publisher.publish(outgoingHighLevelStatePacket);
         packetHasBeenSent.set(true);
      }
   }

   @Override
   public void onBehaviorEntered()
   {
      packetHasBeenSent.set(false);

      isPaused.set(false);
      isAborted.set(false);
   }

   @Override
   public void onBehaviorExited()
   {
      packetHasBeenSent.set(false);
      outgoingHighLevelStatePacket = null;

      isPaused.set(false);
      isAborted.set(false);
   }

   @Override
   public boolean isDone()
   {
      return packetHasBeenSent.getBooleanValue() && !isPaused.getBooleanValue();
   }

   public boolean hasInputBeenSet()
   {
      if (outgoingHighLevelStatePacket != null)
         return true;
      else
         return false;
   }

   @Override
   public void onBehaviorAborted()
   {
   }

   @Override
   public void onBehaviorPaused()
   {
   }

   @Override
   public void onBehaviorResumed()
   {
   }
}
