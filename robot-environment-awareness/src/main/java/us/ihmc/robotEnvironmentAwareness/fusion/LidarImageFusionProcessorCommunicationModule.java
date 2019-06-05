package us.ihmc.robotEnvironmentAwareness.fusion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import controller_msgs.msg.dds.ImageMessage;
import controller_msgs.msg.dds.LidarScanMessage;
import controller_msgs.msg.dds.StereoVisionPointCloudMessage;
import us.ihmc.commons.Conversions;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.pubsub.subscriber.Subscriber;
import us.ihmc.robotEnvironmentAwareness.communication.KryoMessager;
import us.ihmc.robotEnvironmentAwareness.communication.LidarImageFusionAPI;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.robotEnvironmentAwareness.communication.REAModuleAPI;
import us.ihmc.robotEnvironmentAwareness.fusion.data.LidarImageFusionDataBuffer;
import us.ihmc.robotEnvironmentAwareness.fusion.data.StereoREAPlanarRegionFeatureUpdater;
import us.ihmc.robotEnvironmentAwareness.fusion.objectDetection.FusionSensorObjectDetectionManager;
import us.ihmc.robotEnvironmentAwareness.fusion.objectDetection.ObjectType;
import us.ihmc.robotEnvironmentAwareness.fusion.tools.ImageVisualizationHelper;
import us.ihmc.robotEnvironmentAwareness.fusion.tools.PointCloudProjectionHelper;
import us.ihmc.robotEnvironmentAwareness.updaters.REAModuleStateReporter;
import us.ihmc.ros2.Ros2Node;

public class LidarImageFusionProcessorCommunicationModule
{
   private final Messager messager;

   private final Ros2Node ros2Node;
   private final REAModuleStateReporter moduleStateReporter;

   private final LidarImageFusionDataBuffer lidarImageFusionDataBuffer;
   private final StereoREAPlanarRegionFeatureUpdater planarRegionFeatureUpdater;

   private final FusionSensorObjectDetectionManager objectDetectionManager;

   private final AtomicReference<String> socketHostIPAddress;
   private final AtomicReference<BufferedImage> latestBufferedImage = new AtomicReference<>(null);
   private final AtomicReference<List<ObjectType>> selectedObjecTypes;
   private final Messager reaMessager;
   private LidarImageFusionProcessorCommunicationModule(Ros2Node ros2Node, Messager reaMessager, SharedMemoryJavaFXMessager messager)
   {
      this.reaMessager = reaMessager;
      this.messager = messager;
      this.ros2Node = ros2Node;

      moduleStateReporter = new REAModuleStateReporter(reaMessager);
      lidarImageFusionDataBuffer = new LidarImageFusionDataBuffer(messager, PointCloudProjectionHelper.multisenseOnCartIntrinsicParameters);
      planarRegionFeatureUpdater = new StereoREAPlanarRegionFeatureUpdater(reaMessager);

      ROS2Tools.createCallbackSubscription(ros2Node, LidarScanMessage.class, "/ihmc/lidar_scan", this::dispatchLidarScanMessage);
      ROS2Tools.createCallbackSubscription(ros2Node, StereoVisionPointCloudMessage.class, "/ihmc/stereo_vision_point_cloud",
                                           this::dispatchStereoVisionPointCloudMessage);
      ROS2Tools.createCallbackSubscription(ros2Node, ImageMessage.class, "/ihmc/image", this::dispatchImageMessage);

      objectDetectionManager = new FusionSensorObjectDetectionManager(ros2Node, messager);

      messager.registerTopicListener(LidarImageFusionAPI.RequestSocketConnection, (content) -> connect());
      messager.registerTopicListener(LidarImageFusionAPI.RequestObjectDetection, (content) -> request());
      selectedObjecTypes = messager.createInput(LidarImageFusionAPI.SelectedObjecTypes, new ArrayList<ObjectType>());
      socketHostIPAddress = messager.createInput(LidarImageFusionAPI.ObjectDetectionModuleAddress);

      
      messager.registerTopicListener(LidarImageFusionAPI.RunStereoREA, (content) -> runSREA());
   }

   private void connect()
   {
      objectDetectionManager.connectExternalModule(socketHostIPAddress.get());
   }

   private void request()
   {
      objectDetectionManager.requestObjectDetection(latestBufferedImage.getAndSet(null), selectedObjecTypes.get());
   }

   // TODO: Temp method.
   private void runSREA()
   {
      reaMessager.submitMessage(REAModuleAPI.OcTreeEnable, true);
      // TODO: visualize.
      // Reduce and optimize computation time.
      // Visualize manual test.
      // Unit tests.
      // Online system.
      long updateStartTime = System.nanoTime();
      lidarImageFusionDataBuffer.updateNewBuffer();
      planarRegionFeatureUpdater.updateLatestLidarImageFusionData(lidarImageFusionDataBuffer.pollNewBuffer());

      if(planarRegionFeatureUpdater.update())
      {
         moduleStateReporter.reportPlanarRegionsState(planarRegionFeatureUpdater);
      }
      
      double updatingTime = Conversions.nanosecondsToSeconds(System.nanoTime() - updateStartTime);
      String computationTime = Double.toString(updatingTime);
      messager.submitMessage(LidarImageFusionAPI.ComputationTime, computationTime);
   }

   private void dispatchLidarScanMessage(Subscriber<LidarScanMessage> subscriber)
   {
      LidarScanMessage message = subscriber.takeNextData();
      moduleStateReporter.registerLidarScanMessage(message);
   }

   private void dispatchStereoVisionPointCloudMessage(Subscriber<StereoVisionPointCloudMessage> subscriber)
   {
      StereoVisionPointCloudMessage message = subscriber.takeNextData();
      moduleStateReporter.registerStereoVisionPointCloudMessage(message);
      objectDetectionManager.updateLatestStereoVisionPointCloudMessage(message);
      lidarImageFusionDataBuffer.updateLatestStereoVisionPointCloudMessage(message);
   }

   private void dispatchImageMessage(Subscriber<ImageMessage> subscriber)
   {
      ImageMessage message = subscriber.takeNextData();
      if (messager.isMessagerOpen())
         messager.submitMessage(LidarImageFusionAPI.ImageState, new ImageMessage(message));

      latestBufferedImage.set(ImageVisualizationHelper.convertImageMessageToBufferedImage(message));
      lidarImageFusionDataBuffer.updateLatestBufferedImage(latestBufferedImage.get());
   }

   public void start() throws IOException
   {

   }

   public void stop() throws Exception
   {
      messager.closeMessager();
      ros2Node.destroy();
      objectDetectionManager.close();
   }

   public static LidarImageFusionProcessorCommunicationModule createIntraprocessModule(Ros2Node ros2Node, SharedMemoryJavaFXMessager messager)
         throws IOException
   {
      KryoMessager kryoMessager = KryoMessager.createIntraprocess(REAModuleAPI.API, NetworkPorts.REA_MODULE_UI_PORT,
                                                                  REACommunicationProperties.getPrivateNetClassList());
      kryoMessager.setAllowSelfSubmit(true);
      kryoMessager.startMessager();

      return new LidarImageFusionProcessorCommunicationModule(ros2Node, kryoMessager, messager);
   }
}