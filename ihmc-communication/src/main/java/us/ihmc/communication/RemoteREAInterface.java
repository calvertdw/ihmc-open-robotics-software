package us.ihmc.communication;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.REAStateRequestMessage;
import us.ihmc.commons.time.Stopwatch;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.Ros2NodeInterface;

import java.util.function.Consumer;

public class RemoteREAInterface
{
   private final IHMCROS2Publisher<REAStateRequestMessage> reaStateRequestPublisher;
   private final ROS2Input<PlanarRegionsListMessage> planarRegionsListInput;

   private final Stopwatch stopwatch = new Stopwatch();

   public RemoteREAInterface(Ros2NodeInterface ros2Node)
   {
      reaStateRequestPublisher = new IHMCROS2Publisher<>(ros2Node, REAStateRequestMessage.class, null, ROS2Tools.REA);
      planarRegionsListInput = new ROS2Input<>(ros2Node, PlanarRegionsListMessage.class, null, ROS2Tools.REA);

      planarRegionsListInput.addCallback(planarRegionsListMessage -> stopwatch.start());
   }

   public void addPlanarRegionsListCallback(Consumer<PlanarRegionsList> planarRegionsListConsumer)
   {
      planarRegionsListInput.addCallback(planarRegionsListMessage ->
                                          planarRegionsListConsumer.accept(PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsListMessage)));
   }

   public PlanarRegionsList getLatestPlanarRegionsList()
   {
      return PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsListInput.getLatest());
   }

   public PlanarRegionsListMessage getLatestPlanarRegionsListMessage()
   {
      return planarRegionsListInput.getLatest();
   }

   public void clearREA()
   {
      REAStateRequestMessage clearMessage = new REAStateRequestMessage();
      clearMessage.setRequestClear(true);
      reaStateRequestPublisher.publish(clearMessage);
   }

   /**
    * NaN is no planar regions yet received.
    * @return time since planar regions last updated
    */
   public double timeSinceLastUpdate()
   {
      return stopwatch.lapElapsed();
   }

   public boolean getPlanarRegionsListExpired(double expirationDuration)
   {
      return Double.isNaN(timeSinceLastUpdate()) || timeSinceLastUpdate() > expirationDuration;
   }
}
