package us.ihmc.humanoidBehaviors.tools;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import us.ihmc.commons.time.Stopwatch;
import us.ihmc.communication.ROS2Callback;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.robotEnvironmentAwareness.planarRegion.CustomPlanarRegionHandler;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.Ros2NodeInterface;

import java.util.ArrayList;
import java.util.HashMap;

public class RemoteEnvironmentMapInterface
{
   private volatile PlanarRegionsList latestCombinedRegionsList = new PlanarRegionsList();

   private volatile PlanarRegionsList realsenseSLAMRegions = new PlanarRegionsList();
   private final HashMap<Integer, PlanarRegion> supportRegions = new HashMap<>();

   private final Stopwatch stopwatch = new Stopwatch();

   public RemoteEnvironmentMapInterface(Ros2NodeInterface ros2Node)
   {
      new ROS2Callback<>(ros2Node, PlanarRegionsListMessage.class, ROS2Tools.REALSENSE_SLAM_MAP_TOPIC_NAME, this::acceptRealsenseSLAMRegions);

      new ROS2Callback<>(ros2Node,
                         PlanarRegionsListMessage.class,
                         null,
                         ROS2Tools.REA.qualifyMore(ROS2Tools.REA_CUSTOM_REGION_QUALIFIER),
                         this::acceptAdditionalRegionList);
   }

   public synchronized PlanarRegionsList getLatestCombinedRegionsList()
   {
      return latestCombinedRegionsList;
   }

   private void acceptRealsenseSLAMRegions(PlanarRegionsListMessage message)
   {
      stopwatch.start();

      synchronized (this)
      {
         realsenseSLAMRegions = PlanarRegionMessageConverter.convertToPlanarRegionsList(message);

         combineRegions();
      }
   }

   private void acceptAdditionalRegionList(PlanarRegionsListMessage message)
   {
      PlanarRegionsList newRegions = PlanarRegionMessageConverter.convertToPlanarRegionsList(message);

      synchronized (this)
      {
         for (PlanarRegion region : newRegions.getPlanarRegionsAsList())
         {
            if (region.getRegionId() == PlanarRegion.NO_REGION_ID)
            {
               continue;
            }
            else if (region.isEmpty())
            {
               supportRegions.remove(region.getRegionId());
            }
            else
            {
               CustomPlanarRegionHandler.performConvexDecompositionIfNeeded(region);
               supportRegions.put(region.getRegionId(), region);
            }
         }

         combineRegions();
      }
   }

   private void combineRegions()
   {
      ArrayList<PlanarRegion> combinedRegionsList = new ArrayList<>();
      combinedRegionsList.addAll(realsenseSLAMRegions.getPlanarRegionsAsList());
      combinedRegionsList.addAll(supportRegions.values());
      latestCombinedRegionsList = new PlanarRegionsList(combinedRegionsList);
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
