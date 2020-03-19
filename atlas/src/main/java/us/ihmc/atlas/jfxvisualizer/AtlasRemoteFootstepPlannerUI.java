package us.ihmc.atlas.jfxvisualizer;

import controller_msgs.msg.dds.REAStateRequestMessage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Pair;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.footstepPlanning.FootstepPlanningModule;
import us.ihmc.avatar.networkProcessor.footstepPlanningModule.FootstepPlanningModuleLauncher;
import us.ihmc.avatar.networkProcessor.footstepPlanAndProcessModule.FootstepPlanAndProcessModule;
import us.ihmc.avatar.networkProcessor.footstepPlanPostProcessingModule.FootstepPlanPostProcessingModule;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.footstepPlanning.log.FootstepPlannerLogger;
import us.ihmc.footstepPlanning.ui.FootstepPlannerUI;
import us.ihmc.footstepPlanning.ui.RemoteUIMessageConverter;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.pubsub.DomainFactory;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.ros2.RealtimeRos2Node;

/**
 * This class provides a visualizer for the remote footstep planner found in the footstep planner
 * toolbox. It allows users to view the resulting plans calculated by the toolbox. It also allows
 * the user to tune the planner parameters, and request a new plan from the planning toolboxs.
 */
public class AtlasRemoteFootstepPlannerUI extends Application
{
   private static final boolean launchPlannerToolbox = true;
   private static final double GOAL_DISTANCE_PROXIMITY = 0.1;

   private SharedMemoryJavaFXMessager messager;
   private RemoteUIMessageConverter messageConverter;

   private FootstepPlannerUI ui;

   private FootstepPlanAndProcessModule planningAndProcessingModule;

   @Override
   public void start(Stage primaryStage) throws Exception
   {
      DRCRobotModel drcRobotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.REAL_ROBOT, false);
      DRCRobotModel previewModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.REAL_ROBOT, false);
      messager = new SharedMemoryJavaFXMessager(FootstepPlannerMessagerAPI.API);

      RealtimeRos2Node ros2Node = ROS2Tools.createRealtimeRos2Node(DomainFactory.PubSubImplementation.FAST_RTPS, "ihmc_footstep_planner_ui");
      AtlasLowLevelMessenger robotLowLevelMessenger = new AtlasLowLevelMessenger(ros2Node, drcRobotModel.getSimpleRobotName());
      IHMCRealtimeROS2Publisher<REAStateRequestMessage> reaStateRequestPublisher
            = ROS2Tools.createPublisher(ros2Node, REAStateRequestMessage.class, REACommunicationProperties.subscriberTopicNameGenerator);
      messageConverter = new RemoteUIMessageConverter(ros2Node, messager, drcRobotModel.getSimpleRobotName());

      messager.startMessager();
      messager.submitMessage(FootstepPlannerMessagerAPI.GoalDistanceProximity, GOAL_DISTANCE_PROXIMITY);

      ui = FootstepPlannerUI.createMessagerUI(primaryStage, messager, drcRobotModel.getFootstepPlannerParameters(),
                                              drcRobotModel.getVisibilityGraphsParameters(), drcRobotModel.getFootstepPostProcessingParameters(), drcRobotModel,
                                              previewModel, drcRobotModel.getContactPointParameters(), drcRobotModel.getWalkingControllerParameters());
      ui.setRobotLowLevelMessenger(robotLowLevelMessenger);
      ui.setREAStateRequestPublisher(reaStateRequestPublisher);
      ui.show();

      if (launchPlannerToolbox)
      {
         planningAndProcessingModule = new FootstepPlanAndProcessModule(drcRobotModel, DomainFactory.PubSubImplementation.FAST_RTPS);
         FootstepPlanningModule planningModule = planningAndProcessingModule.getPlanningModule();

         // Create logger and connect to messager
         FootstepPlannerLogger logger = new FootstepPlannerLogger(planningModule);
         Runnable loggerRunnable = () -> logger.logSessionAndReportToMessager(messager);
         messager.registerTopicListener(FootstepPlannerMessagerAPI.RequestGenerateLog, b -> new Thread(loggerRunnable).start());

         // Automatically send graph data over messager
         planningModule.addStatusCallback(status ->
                                          {
                                             if (status.getResult().terminalResult())
                                                messager.submitMessage(FootstepPlannerMessagerAPI.GraphData,
                                                                       Pair.of(planningModule.getEdgeDataMap(), planningModule.getIterationData()));
                                          });
      }
   }

   @Override
   public void stop() throws Exception
   {
      super.stop();

      messager.closeMessager();
      messageConverter.destroy();
      ui.stop();

      if (planningAndProcessingModule != null)
         planningAndProcessingModule.closeAndDispose();

      Platform.exit();
   }

   public static void main(String[] args)
   {
      launch(args);
   }
}