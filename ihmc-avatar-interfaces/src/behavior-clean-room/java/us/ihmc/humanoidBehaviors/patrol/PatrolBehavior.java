package us.ihmc.humanoidBehaviors.patrol;

import com.google.common.collect.Lists;
import controller_msgs.msg.dds.FootstepPlanningToolboxOutputStatus;
import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.WalkingStatusMessage;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.footstepPlanning.MultiStageFootstepPlanningModule;
import us.ihmc.commons.thread.Notification;
import us.ihmc.communication.ROS2Input;
import us.ihmc.communication.ROS2ModuleIdentifier;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.footstepPlanning.FootstepDataMessageConverter;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.humanoidBehaviors.tools.RemoteFootstepPlannerInterface;
import us.ihmc.humanoidBehaviors.tools.RemoteRobotControllerInterface;
import us.ihmc.humanoidBehaviors.tools.RemoteSyncedHumanoidFrames;
import us.ihmc.humanoidBehaviors.tools.state.EnhancedStateMachineFactory;
import us.ihmc.humanoidBehaviors.tools.thread.ExceptionPrintingThreadScheduler;
import us.ihmc.humanoidBehaviors.tools.thread.TypedNotification;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelControllerName;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.MessagerAPIFactory.Category;
import us.ihmc.messager.MessagerAPIFactory.CategoryTheme;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.robotEnvironmentAwareness.updaters.LIDARBasedREAModule;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.stateMachine.core.State;
import us.ihmc.robotics.stateMachine.core.StateMachine;
import us.ihmc.ros2.Ros2Node;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static us.ihmc.humanoidBehaviors.patrol.PatrolBehavior.PatrolBehaviorState.*;

/**
 * Walk through a list of waypoints in order, looping forever.
 */
public class PatrolBehavior
{
   enum PatrolBehaviorState
   {
      /** Stop state that waits for or is triggered by a GoToWaypoint message */
      STOP,
      /** Request and wait for footstep planner result */
      PLAN,
      /** Walking towards goal waypoint */
      WALK
   }

   private final Messager messager;
   private final StateMachine<PatrolBehaviorState, State> stateMachine;

   private final ROS2Input<PlanarRegionsListMessage> planarRegionsList;
   private final RemoteRobotControllerInterface remoteRobotControllerInterface;
   private final RemoteSyncedHumanoidFrames remoteSyncedHumanoidFrames;
   private final RemoteFootstepPlannerInterface remoteFootstepPlannerInterface;

   private final Notification stopNotification = new Notification();
   private final Notification overrideGoToWaypointNotification = new Notification();

   private final AtomicInteger goalWaypointIndex = new AtomicInteger();

   private TypedNotification<FootstepPlanningToolboxOutputStatus> footstepPlanResultNotification;
   private TypedNotification<WalkingStatusMessage> walkingCompleted;

   private final AtomicReference<ArrayList<Pose3D>> waypoints;
   private final AtomicReference<Boolean> loop;

   public PatrolBehavior(Messager messager, Ros2Node ros2Node, DRCRobotModel robotModel)
   {
      this.messager = messager;

      LogTools.debug("Initializing patrol behavior");

      EnhancedStateMachineFactory<PatrolBehaviorState> factory = new EnhancedStateMachineFactory<>(PatrolBehaviorState.class);
      factory.getStateMap().get(STOP).setOnEntry(this::onStopStateEntry);
      factory.getStateMap().get(STOP).setDoAction(this::doStopStateAction);
      factory.getFactory().addTransition(STOP, PLAN, this::transitionFromStopToPlan);
      factory.getStateMap().get(PLAN).setOnEntry(this::onPlanStateEntry);
      factory.getStateMap().get(PLAN).setDoAction(this::doPlanStateAction);
      factory.addTransition(PLAN, Lists.newArrayList(WALK, STOP, PLAN), this::transitionFromPlan);
      factory.getStateMap().get(WALK).setOnEntry(this::onWalkStateEntry);
      factory.getStateMap().get(WALK).setDoAction(this::doWalkStateAction);
      factory.getStateMap().get(WALK).setOnExit(this::onWalkStateExit);
      factory.addTransition(WALK, Lists.newArrayList(PLAN, STOP), this::transitionFromWalk);
      stateMachine = factory.getFactory().build(STOP);

      planarRegionsList = new ROS2Input<>(ros2Node, PlanarRegionsListMessage.class, null, LIDARBasedREAModule.ROS2_ID);
      remoteRobotControllerInterface = new RemoteRobotControllerInterface(ros2Node, robotModel);
      remoteSyncedHumanoidFrames = new RemoteSyncedHumanoidFrames(robotModel, ros2Node);
      remoteFootstepPlannerInterface = new RemoteFootstepPlannerInterface(ros2Node, robotModel);

      messager.registerTopicListener(API.Stop, object -> stopNotification.set());
      messager.registerTopicListener(API.GoToWaypoint, goToWaypointIndex -> {
         goalWaypointIndex.set(goToWaypointIndex);
         LogTools.info("Interrupted with GO_TO_WAYPOINT {}", goalWaypointIndex.get());
         overrideGoToWaypointNotification.set();
      });

      waypoints = messager.createInput(API.Waypoints);
      loop = messager.createInput(API.Loop, false);

      ExceptionPrintingThreadScheduler patrolThread = new ExceptionPrintingThreadScheduler(getClass().getSimpleName());
      patrolThread.schedule(this::patrolThread, 2, TimeUnit.MILLISECONDS); // TODO tune this up, 500Hz is probably too much
   }

   private void patrolThread()   // pretty much just updating whichever state is active
   {
      stateMachine.doActionAndTransition();
   }

   private void onStopStateEntry()
   {
      messager.submitMessage(API.CurrentState, STOP.name());

      remoteRobotControllerInterface.pauseWalking();
   }

   private void doStopStateAction(double timeInState)
   {
      pollInterrupts();
   }

   private boolean transitionFromStopToPlan(double timeInState)
   {
      boolean transition = overrideGoToWaypointNotification.read() && goalWaypointInBounds();
      if (transition)
      {
         LogTools.debug("STOP -> PLAN");
      }
      return transition;
   }

   private void onPlanStateEntry()
   {
      messager.submitMessage(API.CurrentState, PLAN.name());

      remoteFootstepPlannerInterface.abortPlanning();

      FramePose3D midFeetZUpPose = new FramePose3D();
      // prevent frame from continuing to change
      midFeetZUpPose.setFromReferenceFrame(remoteSyncedHumanoidFrames.pollHumanoidReferenceFrames().getMidFeetZUpFrame());
      int index = goalWaypointIndex.get();
      messager.submitMessage(API.CurrentWaypointIndexStatus, index);
      FramePose3D currentGoalWaypoint = new FramePose3D(waypoints.get().get(index));

      footstepPlanResultNotification = remoteFootstepPlannerInterface.requestPlan(midFeetZUpPose, currentGoalWaypoint, planarRegionsList.getLatest());
   }

   private void doPlanStateAction(double timeInState)
   {
      pollInterrupts();
      footstepPlanResultNotification.poll();
   }

   private PatrolBehaviorState transitionFromPlan(double timeInState)
   {
      if (stopNotification.read())
      {
         return STOP;
      }
      else if (overrideGoToWaypointNotification.read())
      {
         if (goalWaypointInBounds())
         {
            return PLAN;
         }
         else
         {
            return STOP;
         }
      }
      else if (footstepPlanResultNotification.hasNext())
      {
         if (FootstepPlanningResult.fromByte(footstepPlanResultNotification.read().getFootstepPlanningResult()).validForExecution())
         {
            return WALK;
         }
         else
         {
            return PLAN;
         }
      }

      return null;
   }

   private void onWalkStateEntry()
   {
      messager.submitMessage(API.CurrentState, WALK.name());
      reduceAndSendFootstepsForVisualization(footstepPlanResultNotification.read());
      walkingCompleted = remoteRobotControllerInterface.requestWalk(footstepPlanResultNotification.read(),
                                                                    remoteSyncedHumanoidFrames.pollHumanoidReferenceFrames());
   }

   private void doWalkStateAction(double timeInState)
   {
      pollInterrupts();
      walkingCompleted.poll();
   }

   private PatrolBehaviorState transitionFromWalk(double timeInState)
   {
      if (stopNotification.read())
      {
         return STOP;
      }
      else if (overrideGoToWaypointNotification.read())
      {
         if (goalWaypointInBounds())
         {
            return PLAN;
         }
         else
         {
            return STOP;
         }
      }
      else if (walkingCompleted.hasNext()) // TODO handle robot fell and more
      {
         if (!loop.get() && goalWaypointIndex.get() + 1 >= waypoints.get().size())
         {
            return STOP;
         }
         else
         {
            return PLAN;
         }
      }

      return null;
   }

   private void onWalkStateExit()
   {
      if (!stopNotification.read() && !overrideGoToWaypointNotification.read()) // only increment if WALK -> PLAN
      {
         ArrayList<Pose3D> latestWaypoints = waypoints.get();      // access and store these early
         int nextGoalWaypointIndex = goalWaypointIndex.get() + 1;  // to make thread-safe
         if (nextGoalWaypointIndex >= latestWaypoints.size())
         {
            nextGoalWaypointIndex = 0;
         }
         goalWaypointIndex.set(nextGoalWaypointIndex);
      }
   }

   private void pollInterrupts()
   {
      if (remoteRobotControllerInterface.latestControllerState() != HighLevelControllerName.WALKING) // STOP if robot falls
      {
         stopNotification.set();
      }

      stopNotification.poll();         // poll both at the same time to handle race condition
      overrideGoToWaypointNotification.poll();
   }

   private void reduceAndSendFootstepsForVisualization(FootstepPlanningToolboxOutputStatus footstepPlanningOutput)
   {
      FootstepPlan footstepPlan = FootstepDataMessageConverter.convertToFootstepPlan(footstepPlanningOutput.getFootstepDataList());
      ArrayList<Pair<RobotSide, Pose3D>> footstepLocations = new ArrayList<>();
      for (int i = 0; i < footstepPlan.getNumberOfSteps(); i++)  // this code makes the message smaller to send over the network, TODO investigate
      {
         FramePose3D soleFramePoseToPack = new FramePose3D();
         footstepPlan.getFootstep(i).getSoleFramePose(soleFramePoseToPack);
         footstepLocations.add(new MutablePair<>(footstepPlan.getFootstep(i).getRobotSide(), new Pose3D(soleFramePoseToPack)));
      }
      messager.submitMessage(API.CurrentFootstepPlan, footstepLocations);
   }

   private boolean goalWaypointInBounds()
   {
      ArrayList<Pose3D> latestWaypoints = waypoints.get();     // access and store these early
      int currentGoalWaypointIndex = goalWaypointIndex.get();  // to make thread-safe

      boolean indexInBounds = latestWaypoints != null && currentGoalWaypointIndex >= 0 && currentGoalWaypointIndex < latestWaypoints.size();
      return indexInBounds;
   }

   public static class API
   {
      private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
      private static final Category Root = apiFactory.createRootCategory("PatrolBehavior");
      private static final CategoryTheme Patrol = apiFactory.createCategoryTheme("Patrol");

      /** Input: Update the waypoints */
      public static final Topic<ArrayList<Pose3D>> Waypoints = Root.child(Patrol).topic(apiFactory.createTypedTopicTheme("Waypoints"));

      /** Input: Robot stops and immediately goes to this waypoint. The "start" or "reset" command.  */
      public static final Topic<Integer> GoToWaypoint = Root.child(Patrol).topic(apiFactory.createTypedTopicTheme("GoToWaypoint"));

      /** Input: When received, the robot stops walking and waits forever. */
      public static final Topic<Object> Stop = Root.child(Patrol).topic(apiFactory.createTypedTopicTheme("Stop"));

      /** Input: Toggle looping through waypoints. */
      public static final Topic<Boolean> Loop = Root.child(Patrol).topic(apiFactory.createTypedTopicTheme("Loop"));

      /** Output: to visualize the current robot path plan. */
      public static final Topic<ArrayList<Pair<RobotSide, Pose3D>>> CurrentFootstepPlan = Root.child(Patrol)
                                                                                              .topic(apiFactory.createTypedTopicTheme("CurrentFootstepPlan"));

      /** Output: to visualize the current state. */
      public static final Topic<String> CurrentState = Root.child(Patrol).topic(apiFactory.createTypedTopicTheme("CurrentState"));

      /** Output: to visualize the current waypoint status. TODO clean me up */
      public static final Topic<Integer> CurrentWaypointIndexStatus = Root.child(Patrol).topic(apiFactory.createTypedTopicTheme("CurrentWaypointIndexStatus"));

      public static final MessagerAPI create()
      {
         return apiFactory.getAPIAndCloseFactory();
      }
   }
}
