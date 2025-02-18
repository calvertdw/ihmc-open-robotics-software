package us.ihmc;

import perception_msgs.msg.dds.ImageMessage;
import us.ihmc.avatar.colorVision.BlackflyImagePublisher;
import us.ihmc.avatar.colorVision.BlackflyImageRetriever;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.ROS2SyncedRobotModel;
import us.ihmc.avatar.ros2.ROS2ControllerHelper;
import us.ihmc.behaviors.behaviorTree.ros2.ROS2BehaviorTreeExecutor;
import us.ihmc.behaviors.behaviorTree.ros2.ROS2BehaviorTreeState;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.CommunicationMode;
import us.ihmc.communication.PerceptionAPI;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ros2.ROS2DemandGraphNode;
import us.ihmc.communication.ros2.ROS2Heartbeat;
import us.ihmc.communication.ros2.ROS2Helper;
import us.ihmc.communication.ros2.ROS2PublishSubscribeAPI;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.log.LogTools;
import us.ihmc.perception.IterativeClosestPointManager;
import us.ihmc.perception.RawImage;
import us.ihmc.perception.opencv.OpenCVArUcoMarkerDetectionResults;
import us.ihmc.perception.ouster.OusterDepthImagePublisher;
import us.ihmc.perception.ouster.OusterDepthImageRetriever;
import us.ihmc.perception.ouster.OusterNetServer;
import us.ihmc.perception.realsense.RealsenseConfiguration;
import us.ihmc.perception.realsense.RealsenseDeviceManager;
import us.ihmc.perception.sceneGraph.arUco.ArUcoDetectionUpdater;
import us.ihmc.perception.sceneGraph.arUco.ArUcoSceneTools;
import us.ihmc.perception.sceneGraph.centerpose.CenterposeDetectionManager;
import us.ihmc.perception.sceneGraph.ros2.ROS2SceneGraph;
import us.ihmc.perception.sensorHead.BlackflyLensProperties;
import us.ihmc.robotics.referenceFrames.ReferenceFrameLibrary;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.sensors.RealsenseColorDepthImagePublisher;
import us.ihmc.sensors.RealsenseColorDepthImageRetriever;
import us.ihmc.sensors.ZEDColorDepthImagePublisher;
import us.ihmc.sensors.ZEDColorDepthImageRetriever;
import us.ihmc.tools.thread.RestartableThread;
import us.ihmc.tools.thread.RestartableThrottledThread;
import us.ihmc.tools.thread.SwapReference;

import java.util.Collections;
import java.util.function.Supplier;

/**
 * <p>
 *    This class holds all sensor drivers and publishers, as well as some algorithms which consume the images
 *    produced by the sensors. In general, each sensor has "Retriever" and "Publisher" classes. The retriever
 *    is responsible for grabbing images from the sensor and providing them to this class. Then, any kind of
 *    processing can be performed on the images, which are then given to the publisher or any algorithm which
 *    requires the images.
 * </p>
 * <p>
 *    In general, the retrievers have a thread in which images are grabbed from the sensors.
 *    The publishers have threads which wait for new images to arrive, and once given an image they publish it
 *    and wait again.
 *    Within this class, each sensor has a thread which takes the most recent image from a retriever, performs
 *    any processing, and passes the image on to the publisher.
 * </p>
 * <p>
 *    To launch the Nadia specific sensor configuration, see: {@code NadiaPerceptionAndAutonomyProcess}
 *    To launch the process with no specific configuration, uncomment the call to {@code forceEnableAllSensors}
 *    in the {@code main} method. When launching only one sensor, comment out the other sensor heartbeats in the
 *    {@code forceEnableAllSensors} method.
 * </p>
 *
 * TODO: Add HumanoidPerceptionModule, add depth image overlap removal.
 */
public class PerceptionAndAutonomyProcess
{
   private static final int ZED_CAMERA_ID = 0;
   private static final SideDependentList<ROS2Topic<ImageMessage>> ZED_COLOR_TOPICS = PerceptionAPI.ZED2_COLOR_IMAGES;
   private static final ROS2Topic<ImageMessage> ZED_DEPTH_TOPIC = PerceptionAPI.ZED2_DEPTH;

   private static final String REALSENSE_SERIAL_NUMBER = System.getProperty("d455.serial.number", "215122253249");
   private static final ROS2Topic<ImageMessage> REALSENSE_COLOR_TOPIC = PerceptionAPI.D455_COLOR_IMAGE;
   private static final ROS2Topic<ImageMessage> REALSENSE_DEPTH_TOPIC = PerceptionAPI.D455_DEPTH_IMAGE;

   private static final ROS2Topic<ImageMessage> OUSTER_DEPTH_TOPIC = PerceptionAPI.OUSTER_DEPTH_IMAGE;

   private static final String LEFT_BLACKFLY_SERIAL_NUMBER = System.getProperty("blackfly.left.serial.number", "00000000");
   private static final String RIGHT_BLACKFLY_SERIAL_NUMBER = System.getProperty("blackfly.right.serial.number", "00000000");
   private static final BlackflyLensProperties BLACKFLY_LENS = BlackflyLensProperties.BFS_U3_27S5C_FE185C086HA_1;
   private static final ROS2Topic<ImageMessage> BLACKFLY_IMAGE_TOPIC = PerceptionAPI.BLACKFLY_FISHEYE_COLOR_IMAGE.get(RobotSide.RIGHT);

   private ROS2DemandGraphNode zedPointCloudDemandNode;
   private ROS2DemandGraphNode zedColorDemandNode;
   private ROS2DemandGraphNode zedDepthDemandNode;
   private RawImage zedDepthImage;
   private final SideDependentList<RawImage> zedColorImages = new SideDependentList<>();
   private final ZEDColorDepthImageRetriever zedImageRetriever;
   private final ZEDColorDepthImagePublisher zedImagePublisher;
   private final RestartableThread zedProcessAndPublishThread;

   private ROS2DemandGraphNode realsenseDemandNode;
   private RawImage realsenseDepthImage;
   private RawImage realsenseColorImage;
   private final RealsenseColorDepthImageRetriever realsenseImageRetriever;
   private final RealsenseColorDepthImagePublisher realsenseImagePublisher;
   private final RestartableThread realsenseProcessAndPublishThread;

   private ROS2DemandGraphNode ousterDepthDemandNode;
   private ROS2DemandGraphNode ousterLidarScanDemandNode;
   private ROS2DemandGraphNode ousterHeightMapDemandNode;
   private final OusterNetServer ouster;
   private RawImage ousterDepthImage;
   private final OusterDepthImageRetriever ousterDepthImageRetriever;
   private final OusterDepthImagePublisher ousterDepthImagePublisher;
   private final RestartableThread ousterProcessAndPublishThread;

   private final SideDependentList<Supplier<ReferenceFrame>> blackflyFrameSuppliers = new SideDependentList<>();
   private final SideDependentList<ROS2DemandGraphNode> blackflyImageDemandNodes = new SideDependentList<>();
   private final SideDependentList<RawImage> blackflyImages = new SideDependentList<>();
   private final SideDependentList<BlackflyImageRetriever> blackflyImageRetrievers = new SideDependentList<>();
   private final SideDependentList<BlackflyImagePublisher> blackflyImagePublishers = new SideDependentList<>();
   private final RestartableThread blackflyProcessAndPublishThread;

   private final RestartableThread arUcoMarkerDetectionThread;
   private final ArUcoDetectionUpdater arUcoUpdater;
   private final SwapReference<OpenCVArUcoMarkerDetectionResults> sharedArUcoDetectionResults = new SwapReference<>(OpenCVArUcoMarkerDetectionResults::new);

   private final Supplier<ReferenceFrame> robotPelvisFrameSupplier;
   private final ROS2SceneGraph sceneGraph;
   private final RestartableThrottledThread sceneGraphUpdateThread;
   private ROS2DemandGraphNode arUcoDetectionDemandNode;

   private final CenterposeDetectionManager centerposeDetectionManager;
   private ROS2DemandGraphNode centerposeDemandNode;

   private final IterativeClosestPointManager icpManager;

   private ROS2SyncedRobotModel behaviorTreeSyncedRobot;
   private ReferenceFrameLibrary behaviorTreeReferenceFrameLibrary;
   private ROS2BehaviorTreeExecutor behaviorTreeExecutor;

   // Sensor heartbeats to run main method without UI
   private ROS2Heartbeat zedHeartbeat;
   private ROS2Heartbeat realsenseHeartbeat;
   private ROS2Heartbeat ousterHeartbeat;
   private ROS2Heartbeat leftBlackflyHeartbeat;
   private ROS2Heartbeat rightBlackflyHeartbeat;

   public PerceptionAndAutonomyProcess(ROS2Helper ros2Helper,
                                       Supplier<ReferenceFrame> zedFrameSupplier,
                                       Supplier<ReferenceFrame> realsenseFrameSupplier,
                                       Supplier<ReferenceFrame> ousterFrameSupplier,
                                       Supplier<ReferenceFrame> leftBlackflyFrameSupplier,
                                       Supplier<ReferenceFrame> rightBlackflyFrameSupplier,
                                       Supplier<ReferenceFrame> robotPelvisFrameSupplier,
                                       ReferenceFrame zed2iLeftCameraFrame)
   {
      initializeDependencyGraph(ros2Helper);

      zedImageRetriever = new ZEDColorDepthImageRetriever(ZED_CAMERA_ID, zedFrameSupplier, zedDepthDemandNode, zedColorDemandNode);
      zedImagePublisher = new ZEDColorDepthImagePublisher(ZED_COLOR_TOPICS, ZED_DEPTH_TOPIC);
      zedProcessAndPublishThread = new RestartableThread("ZEDImageProcessAndPublish", this::processAndPublishZED);
      zedProcessAndPublishThread.start();

      realsenseImageRetriever = new RealsenseColorDepthImageRetriever(new RealsenseDeviceManager(),
                                                                      REALSENSE_SERIAL_NUMBER,
                                                                      RealsenseConfiguration.D455_COLOR_720P_DEPTH_720P_30HZ,
                                                                      realsenseFrameSupplier, realsenseDemandNode);
      realsenseImagePublisher = new RealsenseColorDepthImagePublisher(REALSENSE_DEPTH_TOPIC, REALSENSE_COLOR_TOPIC);
      realsenseProcessAndPublishThread = new RestartableThread("RealsenseProcessAndPublish", this::processAndPublishRealsense);
      realsenseProcessAndPublishThread.start();

      ouster = new OusterNetServer();
      ouster.start();
      ousterDepthImageRetriever = new OusterDepthImageRetriever(ouster,
                                                                ousterFrameSupplier,
                                                                ousterLidarScanDemandNode::isDemanded,
                                                                ousterHeightMapDemandNode::isDemanded, ousterDepthDemandNode);
      ousterDepthImagePublisher = new OusterDepthImagePublisher(ouster, OUSTER_DEPTH_TOPIC);
      ousterProcessAndPublishThread = new RestartableThread("OusterProcessAndPublish", this::processAndPublishOuster);
      ousterProcessAndPublishThread.start();

      blackflyFrameSuppliers.put(RobotSide.LEFT, leftBlackflyFrameSupplier);
      blackflyFrameSuppliers.put(RobotSide.RIGHT, rightBlackflyFrameSupplier);
      blackflyProcessAndPublishThread = new RestartableThread("BlackflyProcessAndPublish", this::processAndPublishBlackfly);
      blackflyProcessAndPublishThread.start();

      arUcoUpdater = new ArUcoDetectionUpdater(ros2Helper, BLACKFLY_LENS, blackflyFrameSuppliers.get(RobotSide.RIGHT));
      arUcoMarkerDetectionThread = new RestartableThread("ArUcoMarkerDetection", this::detectAndPublishArUcoMarkers);
      arUcoMarkerDetectionThread.start();

      this.robotPelvisFrameSupplier = robotPelvisFrameSupplier;
      sceneGraph = new ROS2SceneGraph(ros2Helper);
      sceneGraphUpdateThread = new RestartableThrottledThread("SceneGraphUpdater", ROS2BehaviorTreeState.SYNC_FREQUENCY, this::updateSceneGraph);

      centerposeDetectionManager = new CenterposeDetectionManager(ros2Helper);

      icpManager = new IterativeClosestPointManager(ros2Helper, sceneGraph);
      icpManager.startWorkers();
   }

   /** Needs to be a separate method to allow constructing test bench version. */
   public void addBehaviorTree(ROS2Node ros2Node, DRCRobotModel robotModel)
   {
      ROS2ControllerHelper ros2ControllerHelper = new ROS2ControllerHelper(ros2Node, robotModel);
      behaviorTreeSyncedRobot = new ROS2SyncedRobotModel(robotModel, ros2ControllerHelper.getROS2NodeInterface());

      behaviorTreeReferenceFrameLibrary = new ReferenceFrameLibrary();
      behaviorTreeReferenceFrameLibrary.addAll(Collections.singleton(ReferenceFrame.getWorldFrame()));
      behaviorTreeReferenceFrameLibrary.addAll(behaviorTreeSyncedRobot.getReferenceFrames().getCommonReferenceFrames());
      behaviorTreeReferenceFrameLibrary.addDynamicCollection(sceneGraph.asNewDynamicReferenceFrameCollection());

      behaviorTreeExecutor = new ROS2BehaviorTreeExecutor(ros2ControllerHelper,
                                                          robotModel,
                                                          behaviorTreeSyncedRobot,
                                                          behaviorTreeReferenceFrameLibrary);
   }

   public void startAutonomyThread()
   {
      sceneGraphUpdateThread.start(); // scene graph runs at all times
   }

   public void destroy()
   {
      LogTools.info("Destroying {}", getClass().getSimpleName());
      if (zedImageRetriever != null)
      {
         zedProcessAndPublishThread.stop();
         zedImagePublisher.destroy();
         zedImageRetriever.destroy();
      }

      if (realsenseImageRetriever != null)
      {
         realsenseProcessAndPublishThread.stop();
         realsenseImagePublisher.destroy();
         realsenseImageRetriever.destroy();
      }

      if (ouster != null)
      {
         System.out.println("Destroying OusterNetServer...");
         ouster.destroy();
         System.out.println("Destroyed OusterNetServer...");
         ousterProcessAndPublishThread.stop();
         ousterDepthImagePublisher.destroy();
         ousterDepthImageRetriever.destroy();
      }

      sceneGraphUpdateThread.stop();

      if (behaviorTreeExecutor != null)
      {
         behaviorTreeExecutor.destroy();
         behaviorTreeSyncedRobot.destroy();
      }

      if (blackflyProcessAndPublishThread != null)
         blackflyProcessAndPublishThread.stop();
      for (RobotSide side : RobotSide.values)
      {
         if (blackflyImagePublishers.get(side) != null)
            blackflyImagePublishers.get(side).destroy();
         if (blackflyImageRetrievers.get(side) != null)
            blackflyImageRetrievers.get(side).destroy();
      }

      icpManager.destroy();

      zedPointCloudDemandNode.destroy();
      zedColorDemandNode.destroy();
      zedDepthDemandNode.destroy();
      realsenseDemandNode.destroy();
      ousterDepthDemandNode.destroy();
      ousterLidarScanDemandNode.destroy();
      blackflyImageDemandNodes.forEach(ROS2DemandGraphNode::destroy);
      arUcoDetectionDemandNode.destroy();
      centerposeDemandNode.destroy();

      if (zedHeartbeat != null)
         zedHeartbeat.destroy();
      if (realsenseHeartbeat != null)
         realsenseHeartbeat.destroy();
      if (ousterHeartbeat != null)
         ousterHeartbeat.destroy();
      if (leftBlackflyHeartbeat != null)
         leftBlackflyHeartbeat.destroy();
      if (rightBlackflyHeartbeat != null)
         rightBlackflyHeartbeat.destroy();

      LogTools.info("Destroyed {}", getClass().getSimpleName());
   }

   private void processAndPublishZED()
   {
      if (zedDepthDemandNode.isDemanded() || zedColorDemandNode.isDemanded())
      {
         zedDepthImage = zedImageRetriever.getLatestRawDepthImage();
         for (RobotSide side : RobotSide.values)
         {
            zedColorImages.put(side, zedImageRetriever.getLatestRawColorImage(side));
         }

         if (zedDepthImage != null && !zedDepthImage.isEmpty() && icpManager.isDemanded())
            icpManager.setEnvironmentPointCloud(zedDepthImage);

         zedImagePublisher.setNextGpuDepthImage(zedDepthImage.get());
         for (RobotSide side : RobotSide.values)
         {
            zedImagePublisher.setNextColorImage(zedColorImages.get(side).get(), side);
         }

         zedDepthImage.release();
         zedColorImages.forEach(RawImage::release);
      }
      else
         ThreadTools.sleep(500);
   }

   private void processAndPublishRealsense()
   {
      if (realsenseDemandNode.isDemanded())
      {
         realsenseDepthImage = realsenseImageRetriever.getLatestRawDepthImage();
         realsenseColorImage = realsenseImageRetriever.getLatestRawColorImage();

         // Do processing on image

         realsenseImagePublisher.setNextDepthImage(realsenseDepthImage.get());
         realsenseImagePublisher.setNextColorImage(realsenseColorImage.get());

         realsenseDepthImage.release();
         realsenseColorImage.release();
      }
      else
         ThreadTools.sleep(500);
   }

   private void processAndPublishOuster()
   {
      if (ousterDepthDemandNode.isDemanded())
      {
         ouster.start();
         ousterDepthImage = ousterDepthImageRetriever.getLatestRawDepthImage();
         if (ousterDepthImage == null)
            return;

         ousterDepthImagePublisher.setNextDepthImage(ousterDepthImage.get());

         ousterDepthImage.release();
      }
      else
      {
         ouster.stop();
         ThreadTools.sleep(500);
      }
   }

   private void processAndPublishBlackfly()
   {
      boolean atLeastOneDemanded = false;
      for (RobotSide side : RobotSide.values)
      {
         if (blackflyImageDemandNodes.get(side).isDemanded())
         {
            atLeastOneDemanded = true;
            if (blackflyImageRetrievers.get(side) != null && blackflyImagePublishers.get(side) != null)
            {
               blackflyImages.put(side, blackflyImageRetrievers.get(side).getLatestRawImage());

               blackflyImagePublishers.get(side).setNextDistortedImage(blackflyImages.get(side).get());
               arUcoUpdater.setNextDistortedImage(blackflyImages.get(side).get());

               blackflyImages.get(side).release();
            }
            else
            {
               initializeBlackfly(side);
            }
         }
      }

      if (!atLeastOneDemanded)
         ThreadTools.sleep(500);
   }

   private void detectAndPublishArUcoMarkers()
   {
      if (arUcoDetectionDemandNode.isDemanded())
      {
         boolean performedDetection = arUcoUpdater.undistortAndUpdateArUco();
         if (performedDetection)
         {
            sharedArUcoDetectionResults.getForThreadOne().copyOutputData(arUcoUpdater.getArUcoMarkerDetector());
            sharedArUcoDetectionResults.swap();
         }
      }
      else
      {
         ThreadTools.sleep(500);
      }
   }

   private void updateSceneGraph()
   {
      sceneGraph.updateSubscription();
      synchronized (sharedArUcoDetectionResults)
      {
         ArUcoSceneTools.updateSceneGraph(sharedArUcoDetectionResults.getForThreadTwo(), blackflyFrameSuppliers.get(RobotSide.RIGHT).get(), sceneGraph);
      }

      // Update CenterPose stuff
      if (centerposeDemandNode.isDemanded())
         centerposeDetectionManager.updateSceneGraph(sceneGraph);

      // Update general stuff
      sceneGraph.updateOnRobotOnly(robotPelvisFrameSupplier.get());
      sceneGraph.updatePublication();

      if (behaviorTreeExecutor != null)
      {
         behaviorTreeSyncedRobot.update();
         behaviorTreeExecutor.update();
      }
   }

   private void initializeBlackfly(RobotSide side)
   {
      String serialNumber = side == RobotSide.LEFT ? LEFT_BLACKFLY_SERIAL_NUMBER : RIGHT_BLACKFLY_SERIAL_NUMBER;
      if (serialNumber.equals("00000000"))
      {
         LogTools.error("{} blackfly with serial number was not provided", side.getPascalCaseName());
         ThreadTools.sleep(3000);
         return;
      }

      blackflyImageRetrievers.put(side,
                                  new BlackflyImageRetriever(serialNumber,
                                                             BLACKFLY_LENS,
                                                             RobotSide.RIGHT,
                                                             blackflyFrameSuppliers.get(side),
                                                             blackflyImageDemandNodes.get(side)));
      blackflyImagePublishers.put(side, new BlackflyImagePublisher(BLACKFLY_LENS, BLACKFLY_IMAGE_TOPIC));
   }

   private void initializeDependencyGraph(ROS2PublishSubscribeAPI ros2)
   {
      // Initialize all nodes
      zedPointCloudDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_ZED_POINT_CLOUD);
      zedDepthDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_ZED_DEPTH);
      zedColorDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_ZED_COLOR);

      realsenseDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_REALSENSE_POINT_CLOUD);

      ousterDepthDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_OUSTER_DEPTH);
      ousterHeightMapDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_HEIGHT_MAP);
      ousterLidarScanDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_LIDAR_SCAN);

      for (RobotSide side : RobotSide.values)
         blackflyImageDemandNodes.put(side, new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_BLACKFLY_COLOR_IMAGE.get(side)));

      arUcoDetectionDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_ARUCO);

      centerposeDemandNode = new ROS2DemandGraphNode(ros2, PerceptionAPI.REQUEST_CENTERPOSE);

      // build the graph
      zedDepthDemandNode.addDependents(zedPointCloudDemandNode);
      zedColorDemandNode.addDependents(zedPointCloudDemandNode, centerposeDemandNode);

      blackflyImageDemandNodes.get(RobotSide.RIGHT).addDependents(arUcoDetectionDemandNode);
   }

   /*
    * This method is used to enable sensors without needing to run the UI.
    * Unneeded sensors can be commented out.
    */
   private void forceEnableAllSensors(ROS2PublishSubscribeAPI ros2)
   {
      zedHeartbeat = new ROS2Heartbeat(ros2, PerceptionAPI.REQUEST_ZED_POINT_CLOUD);
      zedHeartbeat.setAlive(true);

      realsenseHeartbeat = new ROS2Heartbeat(ros2, PerceptionAPI.REQUEST_REALSENSE_POINT_CLOUD);
      realsenseHeartbeat.setAlive(true);

      ousterHeartbeat = new ROS2Heartbeat(ros2, PerceptionAPI.REQUEST_OUSTER_DEPTH);
      ousterHeartbeat.setAlive(true);

      leftBlackflyHeartbeat = new ROS2Heartbeat(ros2, PerceptionAPI.REQUEST_BLACKFLY_COLOR_IMAGE.get(RobotSide.LEFT));
      leftBlackflyHeartbeat.setAlive(true);

      rightBlackflyHeartbeat = new ROS2Heartbeat(ros2, PerceptionAPI.REQUEST_BLACKFLY_COLOR_IMAGE.get(RobotSide.RIGHT));
      rightBlackflyHeartbeat.setAlive(true);
   }

   public static void main(String[] args)
   {
      ROS2Node ros2Node = ROS2Tools.createROS2Node(CommunicationMode.INTERPROCESS.getPubSubImplementation(), "perception_autonomy_process");
      ROS2Helper ros2Helper = new ROS2Helper(ros2Node);

      PerceptionAndAutonomyProcess perceptionAndAutonomyProcess = new PerceptionAndAutonomyProcess(ros2Helper,
                                                                                                   ReferenceFrame::getWorldFrame,
                                                                                                   ReferenceFrame::getWorldFrame,
                                                                                                   ReferenceFrame::getWorldFrame,
                                                                                                   ReferenceFrame::getWorldFrame,
                                                                                                   ReferenceFrame::getWorldFrame,
                                                                                                   ReferenceFrame::getWorldFrame,
                                                                                                   ReferenceFrame.getWorldFrame());
      perceptionAndAutonomyProcess.startAutonomyThread();

      // To run a sensor without the UI, uncomment the below line.
      // perceptionAndAutonomyProcess.forceEnableAllSensors(ros2Helper);
      Runtime.getRuntime().addShutdownHook(new Thread(perceptionAndAutonomyProcess::destroy, "PerceptionAutonomyProcessShutdown"));
   }
}
