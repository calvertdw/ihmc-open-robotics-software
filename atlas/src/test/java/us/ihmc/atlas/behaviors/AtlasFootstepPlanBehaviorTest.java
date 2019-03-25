package us.ihmc.atlas.behaviors;

import controller_msgs.msg.dds.AbortWalkingMessage;
import controller_msgs.msg.dds.FootstepDataListMessage;
import controller_msgs.msg.dds.FootstepPlanningToolboxOutputStatus;
import org.junit.jupiter.api.*;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.footstepPlanning.MultiStageFootstepPlanningModule;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.humanoidBehaviors.tools.RemoteFootstepPlannerInterface;
import us.ihmc.humanoidBehaviors.tools.RemoteSyncedHumanoidFrames;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.AbortWalkingCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.FootstepDataListCommand;
import us.ihmc.log.LogTools;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationConstructionSetTools.util.environments.FlatGroundEnvironment;
import us.ihmc.simulationConstructionSetTools.util.simulationrunner.GoalOrientedTestConductor;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("humanoid-behaviors")
public class AtlasFootstepPlanBehaviorTest
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();
   private AtlasBehaviorTestYoVariables variables;
   private GoalOrientedTestConductor conductor;
   private AtlasRobotModel robotModel;
   private IHMCROS2Publisher<FootstepDataListMessage> footstepDataListPublisher;
   private IHMCROS2Publisher<AbortWalkingMessage> abortPublisher;
   private Ros2Node ros2Node;

   double lastX = 0.0;
   double lastY = 0.0;
   double lastYaw = 0.0;
   private RemoteFootstepPlannerInterface remoteFootstepPlannerInterface;

   @Test
   public void testExecuteFootstepPlan() throws IOException
   {
      FootstepPlanningToolboxOutputStatus output = setupForFootstepTest();

      footstepDataListPublisher.publish(output.getFootstepDataList());

      AtlasTestScripts.takeSteps(conductor, variables, output.getFootstepDataList().getFootstepDataList().size(), 6.0);

      AtlasTestScripts.holdDoubleSupport(conductor, variables, 3.0, 6.0);
   }

   @Test
   public void testWalkNegativePiToPi() throws IOException
   {
      new MultiStageFootstepPlanningModule(robotModel, null, false, PubSubImplementation.INTRAPROCESS);

      ros2Node = new Ros2Node(PubSubImplementation.INTRAPROCESS, getClass().getSimpleName());

      footstepDataListPublisher = ROS2Tools
            .createPublisher(ros2Node, ROS2Tools.newMessageInstance(FootstepDataListCommand.class).getMessageClass(),
                             ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotModel.getSimpleRobotName()));

      abortPublisher = ROS2Tools
            .createPublisher(ros2Node, ROS2Tools.newMessageInstance(AbortWalkingCommand.class).getMessageClass(),
                             ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotModel.getSimpleRobotName()));

      RemoteSyncedHumanoidFrames remoteSyncedHumanoidFrames = new RemoteSyncedHumanoidFrames(robotModel, ros2Node);
      remoteFootstepPlannerInterface = new RemoteFootstepPlannerInterface(ros2Node, robotModel);

      AtlasTestScripts.wait(conductor, variables, 0.25);  // allows to update frames

      planSteps(0.0, 0.0, 0.5);  // 0.24 s
      planSteps(0.0, 0.0, -0.5); // 0.38 s
      planSteps(0.0, 0.0, 0.5);  // 0.64 s
      planSteps(0.0, 0.0, -0.5); // 0.25 s

      planSteps(0.0, 0.0, -0.5); // 0.008 s
      planSteps(0.0, 0.0, -1.0); // 0.028 s
      planSteps(0.0, 0.0, -3.1); // 8.498 s
      planSteps(0.0, 0.0, 3.1);  // timeout > 30s

      planSteps(0.0, 0.0, 0.5); // 0.24 s
      planSteps(0.0, 0.0, 1.0); // 0.14 s
      planSteps(0.0, 0.0, 3.1); // timeout
      planSteps(0.0, 0.0, -3.1);

      AtlasTestScripts.holdDoubleSupport(conductor, variables, 3.0, 6.0);
   }

   private void planSteps(double x, double y, double yaw)
   {
      FramePose3D startPose = new FramePose3D();
      startPose.set(lastX, lastY, 0.0, lastYaw, 0.0, 0.0);
      LogTools.debug("StartPose = {}", startPose);

      FramePose3D goalPose = new FramePose3D();
      goalPose.set(x, y, 0.0, yaw, 0.0, 0.0);

      LogTools.info("Planning from {}, {}, yaw: {}", lastX, lastY, lastYaw);
      LogTools.info("to {}, {}, yaw: {}", x, y, yaw);

      FootstepPlanningToolboxOutputStatus output1 = remoteFootstepPlannerInterface.requestPlanBlocking(startPose, goalPose, null);

      LogTools.info("Received footstep planning result: {}", FootstepPlanningResult.fromByte(output1.getFootstepPlanningResult()));
      LogTools.info("Received footstep plan took: {} s", output1.getTimeTaken());
      LogTools.info("Received footstep planning status: {}", output1);

      boolean optimal = output1.getFootstepPlanningResult() == FootstepPlanningToolboxOutputStatus.FOOTSTEP_PLANNING_RESULT_OPTIMAL_SOLUTION;
      boolean subOptimal = output1.getFootstepPlanningResult() == FootstepPlanningToolboxOutputStatus.FOOTSTEP_PLANNING_RESULT_SUB_OPTIMAL_SOLUTION;
      assertTrue(optimal || subOptimal, "Solution failed");
      FootstepPlanningToolboxOutputStatus output = output1;

      footstepDataListPublisher.publish(output.getFootstepDataList());

      AtlasTestScripts.takeSteps(conductor, variables, output.getFootstepDataList().getFootstepDataList().size(), 6.0);

      lastX = x;
      lastY = y;
      lastYaw = yaw;
   }

   @Test
   public void testStopWalking() throws IOException
   {
      FootstepPlanningToolboxOutputStatus output = setupForFootstepTest();

      footstepDataListPublisher = ROS2Tools
            .createPublisher(ros2Node, ROS2Tools.newMessageInstance(FootstepDataListCommand.class).getMessageClass(),
                             ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotModel.getSimpleRobotName()));

      footstepDataListPublisher.publish(output.getFootstepDataList());

      AtlasTestScripts.takeSteps(conductor, variables, output.getFootstepDataList().getFootstepDataList().size() / 2, 6.0);

      abortPublisher.publish(new AbortWalkingMessage());   // stop

      AtlasTestScripts.holdDoubleSupport(conductor, variables, 3.0, 0.5);
   }

   private FootstepPlanningToolboxOutputStatus setupForFootstepTest() throws IOException
   {
      new MultiStageFootstepPlanningModule(robotModel, null, false, PubSubImplementation.INTRAPROCESS);

      ros2Node = new Ros2Node(PubSubImplementation.INTRAPROCESS, getClass().getSimpleName());

      footstepDataListPublisher = ROS2Tools
            .createPublisher(ros2Node, ROS2Tools.newMessageInstance(FootstepDataListCommand.class).getMessageClass(),
                             ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotModel.getSimpleRobotName()));

      abortPublisher = ROS2Tools
            .createPublisher(ros2Node, ROS2Tools.newMessageInstance(AbortWalkingCommand.class).getMessageClass(),
                             ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotModel.getSimpleRobotName()));

      RemoteSyncedHumanoidFrames remoteSyncedHumanoidFrames = new RemoteSyncedHumanoidFrames(robotModel, ros2Node);
      RemoteFootstepPlannerInterface remoteFootstepPlannerInterface = new RemoteFootstepPlannerInterface(ros2Node, robotModel);

      AtlasTestScripts.wait(conductor, variables, 0.25);  // allows to update frames

      FramePose3D midFeetZUpPose = new FramePose3D(remoteSyncedHumanoidFrames.pollHumanoidReferenceFrames().getMidFeetZUpFrame());
      LogTools.debug("MidFeetZUp = {}", midFeetZUpPose);

      FramePose3D currentGoalWaypoint = new FramePose3D();
      currentGoalWaypoint.prependTranslation(2.0, 0.0, 0.0);
      FootstepPlanningToolboxOutputStatus output = remoteFootstepPlannerInterface.requestPlanBlocking(midFeetZUpPose,
                                                                                                      currentGoalWaypoint,
                                                                                                      null);

      LogTools.info("Received footstep planning status: {}", output);

      boolean optimal = output.getFootstepPlanningResult() == FootstepPlanningToolboxOutputStatus.FOOTSTEP_PLANNING_RESULT_OPTIMAL_SOLUTION;
      boolean subOptimal = output.getFootstepPlanningResult() == FootstepPlanningToolboxOutputStatus.FOOTSTEP_PLANNING_RESULT_SUB_OPTIMAL_SOLUTION;
      assertTrue(optimal || subOptimal, "Solution failed");
      return output;
   }

   @AfterEach
   public void destroySimulationAndRecycleMemory()
   {
      conductor.concludeTesting();
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @BeforeEach
   public void setUp()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
      robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.SCS, false);
      SimulationConstructionSet scs = AtlasBehaviorSimulation.createForAutomatedTest(robotModel, new FlatGroundEnvironment());
      variables = new AtlasBehaviorTestYoVariables(scs);
      conductor = new GoalOrientedTestConductor(scs, simulationTestingParameters);
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      AtlasTestScripts.standUp(conductor, variables);
   }

   @AfterAll
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(AtlasFootstepPlanBehaviorTest.class + " after class.");
   }
}
