package us.ihmc.atlas.behaviors;

import controller_msgs.msg.dds.WalkingControllerFailureStatusMessage;
import javafx.application.Platform;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.kinematicsSimulation.HumanoidKinematicsSimulation;
import us.ihmc.avatar.kinematicsSimulation.HumanoidKinematicsSimulationParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.ContinuousIntegrationTools;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.commons.thread.Notification;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.CommunicationMode;
import us.ihmc.communication.IHMCROS2Callback;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.graphicsDescription.appearance.YoAppearanceTexture;
import us.ihmc.humanoidBehaviors.BehaviorModule;
import us.ihmc.humanoidBehaviors.BehaviorRegistry;
import us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehavior;
import us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehaviorAPI;
import us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehaviorParameters;
import us.ihmc.humanoidBehaviors.tools.RemoteHumanoidRobotInterface;
import us.ihmc.humanoidBehaviors.tools.RemoteSyncedRobotModel;
import us.ihmc.humanoidBehaviors.ui.behaviors.LookAndStepRemoteVisualizer;
import us.ihmc.humanoidBehaviors.ui.simulation.BehaviorPlanarRegionEnvironments;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.simulationConstructionSetTools.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationConstructionSetTools.util.environments.PlanarRegionsListDefinedEnvironment;
import us.ihmc.tools.thread.PausablePeriodicThread;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static us.ihmc.pubsub.DomainFactory.PubSubImplementation.*;
import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(OrderAnnotation.class)
public class AtlasLookAndStepBehaviorTest
{
   private static final boolean VISUALIZE = Boolean.parseBoolean(System.getProperty("visualize"));
   static
   {
      System.setProperty("create.scs.gui", Boolean.toString(VISUALIZE));
   }

   private BehaviorModule behaviorModule;
   private ROS2Node ros2Node;
   private Messager behaviorMessager;
   private PausablePeriodicThread monitorThread;
   private LookAndStepRemoteVisualizer lookAndStepVisualizer;
   private HumanoidKinematicsSimulation kinematicsSimulation;
   private AtlasDynamicsSimulation dynamicsSimulation;
   private AtlasPerceptionSimulation perceptionStack;

   private static class TestWaypoint
   {
      String name;
      Pose3D goalPose;
      Function<Pose3DReadOnly, Boolean> reachedCondition;
      Notification reachedNotification = new Notification();
   }

   @Test
   @Order(1)
   public void testLookAndStepOverFlatGround()
   {
      List<TestWaypoint> waypoints = new ArrayList<>();
      waypoints.add(new TestWaypoint());
      waypoints.get(0).name = "HALFWAY";
      waypoints.get(0).goalPose = new Pose3D(1.5, 0.0, 0.0, 0.0, 0.0, 0.0);
      waypoints.get(0).reachedCondition = pelvisPose -> pelvisPose.getPosition().getX() > 0.8;
      waypoints.add(new TestWaypoint());
      waypoints.get(1).name = "ALL THE WAY";
      waypoints.get(1).goalPose = new Pose3D(3.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      waypoints.get(1).reachedCondition = pelvisPose -> pelvisPose.getPosition().getX() > 2.2;

      assertTimeoutPreemptively(Duration.ofMinutes(3), () -> runTheTest(BehaviorPlanarRegionEnvironments::flatGround, false, waypoints));
   }

   @Test
   @Order(2)
   public void testLookAndStepOverStairs()
   {
      List<TestWaypoint> waypoints = new ArrayList<>();
      waypoints.add(new TestWaypoint());
      waypoints.get(0).name = "THE TOP";
      waypoints.get(0).goalPose = new Pose3D(3.0, 0.0, BehaviorPlanarRegionEnvironments.topPlatformHeight, 0.0, 0.0, 0.0);
      waypoints.get(0).reachedCondition = pelvisPose -> pelvisPose.getPosition().getX() > 2.2 && pelvisPose.getPosition().getZ() > 1.3;
      waypoints.add(new TestWaypoint());
      waypoints.get(1).name = "OTHER SIDE";
      waypoints.get(1).goalPose = new Pose3D(6.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      waypoints.get(1).reachedCondition = pelvisPose -> pelvisPose.getPosition().getX() > 5.2;

      assertTimeoutPreemptively(Duration.ofMinutes(5),
                                () -> runTheTest(BehaviorPlanarRegionEnvironments::createRoughUpAndDownStairsWithFlatTop, true, waypoints));
   }

   private void runTheTest(Supplier<PlanarRegionsList> environment, boolean useDynamicsSimulation, List<TestWaypoint> waypoints)
   {
      ThreadTools.startAsDaemon(() -> reaModule(environment), "REAModule");
      Notification finishedSimulationSetup = new Notification();
      if (useDynamicsSimulation)
      {
         ThreadTools.startAsDaemon(() -> dynamicsSimulation(environment, finishedSimulationSetup), "DynamicsSimulation");
      }
      else
      {
         ThreadTools.startAsDaemon(() -> kinematicSimulation(finishedSimulationSetup), "KinematicsSimulation");
      }

      ros2Node = ROS2Tools.createROS2Node(INTRAPROCESS, "Helper");


      AtlasRobotModel robotModelForBehavior = createRobotModel();
      robotModelForBehavior.getSwingPlannerParameters().setMinimumSwingFootClearance(0.0);
      behaviorModule = BehaviorModule.createIntraprocess(BehaviorRegistry.of(LookAndStepBehavior.DEFINITION), robotModelForBehavior);
      behaviorMessager = behaviorModule.getMessager();

      if (VISUALIZE)
      {
         lookAndStepVisualizer = new LookAndStepRemoteVisualizer(createRobotModel(), ros2Node, behaviorMessager);
      }

      finishedSimulationSetup.blockingPoll();

      ThreadTools.sleepSeconds(3.0);

      AtlasRobotModel robotModel = createRobotModel();
      RemoteHumanoidRobotInterface robot = new RemoteHumanoidRobotInterface(ros2Node, robotModel);

      AtomicReference<String> currentState = behaviorMessager.createInput(LookAndStepBehaviorAPI.CurrentState);
      behaviorMessager.submitMessage(BehaviorModule.API.BehaviorSelection, LookAndStepBehavior.DEFINITION.getName());

      Notification reachedGoalOrFallen = new Notification();

      new IHMCROS2Callback<>(ros2Node,
                             ControllerAPIDefinition.getTopic(WalkingControllerFailureStatusMessage.class, robotModel.getSimpleRobotName()),
                             message ->
                             {
                                LogTools.error("Controller failure detected! Fall direction: {}", message.getFallingDirection());
                                reachedGoalOrFallen.set();
                             });

      LookAndStepBehaviorParameters lookAndStepBehaviorParameters = new LookAndStepBehaviorParameters();
      lookAndStepBehaviorParameters.set(LookAndStepBehaviorParameters.minimumSwingFootClearanceOverride, 0.0);
      behaviorMessager.submitMessage(LookAndStepBehaviorAPI.LookAndStepParameters, lookAndStepBehaviorParameters.getAllAsStrings());

      behaviorMessager.submitMessage(LookAndStepBehaviorAPI.OperatorReviewEnabled, false);
      IHMCROS2Publisher<Pose3D> goalInputPublisher = IHMCROS2Publisher.newPose3DPublisher(ros2Node, LookAndStepBehaviorAPI.GOAL_INPUT);
      new IHMCROS2Callback<>(ros2Node, LookAndStepBehaviorAPI.REACHED_GOAL, message -> reachedGoalOrFallen.set());

      RemoteSyncedRobotModel syncedRobot = robot.newSyncedRobot();
      monitorThread = new PausablePeriodicThread(
            "RobotStatusThread",
            0.5,
            () -> monitorThread(currentState, syncedRobot, waypoints));
      monitorThread.start();

      for (TestWaypoint waypoint : waypoints)
      {
         goalInputPublisher.publish(waypoint.goalPose);
         reachedGoalOrFallen.blockingPoll();
         assertTrue(waypoint.reachedNotification.poll(), "NOT " + waypoint.name);
         LogTools.info("REACHED " + waypoint.name);
      }
   }

   private void process()
   {

   }

   private void monitorThread(AtomicReference<String> currentState,
                              RemoteSyncedRobotModel syncedRobot,
                              List<TestWaypoint> waypoints)
   {
      syncedRobot.update();
      FramePose3DReadOnly pelvisPose = syncedRobot.getFramePoseReadOnly(HumanoidReferenceFrames::getPelvisZUpFrame);
      LogTools.info("{} pose: {}", currentState.get(), pelvisPose);

      for (TestWaypoint waypoint : waypoints)
      {
         if (waypoint.reachedCondition.apply(pelvisPose))
         {
            waypoint.reachedNotification.set();
         }
      }
   }

   private void reaModule(Supplier<PlanarRegionsList> environment)
   {
      perceptionStack = new AtlasPerceptionSimulation(CommunicationMode.INTRAPROCESS, environment.get(), false, createRobotModel());
   }

   private void dynamicsSimulation(Supplier<PlanarRegionsList> environment, Notification finishedSettingUp)
   {
      LogTools.info("Creating dynamics simulation");
      int recordFrequencySpeedup = 50; // Increase to 10 when you want the sims to run a little faster and don't need all of the YoVariable data.
      int scsDataBufferSize = 10;
      dynamicsSimulation = AtlasDynamicsSimulation.create(createRobotModel(),
                                                          createCommonAvatarEnvironment(environment),
                                                          INTRAPROCESS,
                                                          recordFrequencySpeedup,
                                                          scsDataBufferSize);
      dynamicsSimulation.simulate();
      LogTools.info("Finished setting up dynamics simulation.");
      finishedSettingUp.set();
   }

   private CommonAvatarEnvironmentInterface createCommonAvatarEnvironment(Supplier<PlanarRegionsList> environment)
   {
      String environmentName = PlanarRegionsListDefinedEnvironment.class.getSimpleName();
      YoAppearanceTexture cinderBlockTexture = new YoAppearanceTexture("sampleMeshes/cinderblock.png");
      return new PlanarRegionsListDefinedEnvironment(environmentName,
                                                     environment.get(),
                                                     cinderBlockTexture,
                                                     0.02,
                                                     false);
   }

   private void kinematicSimulation(Notification finishedSimulationSetup)
   {
      LogTools.info("Creating kinematics  simulation");
      HumanoidKinematicsSimulationParameters kinematicsSimulationParameters = new HumanoidKinematicsSimulationParameters();
      kinematicsSimulationParameters.setPubSubImplementation(INTRAPROCESS);
      kinematicsSimulationParameters.setLogToFile(true);
      if (ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer())
      {
         kinematicsSimulationParameters.setLogToFile(false);
         kinematicsSimulationParameters.setIncomingLogsDirectory(Paths.get("/opt/BambooVideos"));
      }
      kinematicsSimulationParameters.setCreateYoVariableServer(false);
      kinematicsSimulation = AtlasKinematicSimulation.create(createRobotModel(), kinematicsSimulationParameters);
      finishedSimulationSetup.set();
   }

   private AtlasRobotModel createRobotModel()
   {
      return new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.SCS, false);
   }

   @AfterEach
   public void afterEach()
   {
      perceptionStack.destroy();
      behaviorModule.destroy();
      ros2Node.destroy();
      ExceptionTools.handle(() -> behaviorMessager.closeMessager(), DefaultExceptionHandler.PRINT_STACKTRACE);
      monitorThread.stop();
      if (VISUALIZE)
         Platform.runLater(() -> lookAndStepVisualizer.close());
      if (kinematicsSimulation != null)
      {
         kinematicsSimulation.destroy();
         kinematicsSimulation = null;
      }
      if (dynamicsSimulation != null)
      {
         dynamicsSimulation.destroy();
         dynamicsSimulation = null;
      }
   }
}
