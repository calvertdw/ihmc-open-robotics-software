package us.ihmc.humanoidBehaviors.lookAndStep;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.commons.thread.TypedNotification;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.communication.util.Timer;
import us.ihmc.communication.util.TimerSnapshotWithExpiration;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.*;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.footstepPlanning.log.FootstepPlannerLogger;
import us.ihmc.footstepPlanning.swing.SwingPlannerParametersReadOnly;
import us.ihmc.footstepPlanning.swing.SwingPlannerType;
import us.ihmc.humanoidBehaviors.tools.RemoteSyncedRobotModel;
import us.ihmc.humanoidBehaviors.tools.footstepPlanner.FootstepForUI;
import us.ihmc.humanoidBehaviors.tools.interfaces.StatusLogger;
import us.ihmc.humanoidBehaviors.tools.interfaces.UIPublisher;
import us.ihmc.humanoidBehaviors.tools.walkingController.ControllerStatusTracker;
import us.ihmc.pathPlanning.bodyPathPlanner.BodyPathPlannerTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehaviorAPI.*;

public class LookAndStepFootstepPlanningTask
{
   protected StatusLogger statusLogger;
   protected LookAndStepBehaviorParametersReadOnly lookAndStepBehaviorParameters;
   protected FootstepPlannerParametersReadOnly footstepPlannerParameters;
   protected SwingPlannerParametersReadOnly swingPlannerParameters;
   protected UIPublisher uiPublisher;
   protected FootstepPlanningModule footstepPlanningModule;
   protected Supplier<Boolean> operatorReviewEnabledSupplier;
   protected RemoteSyncedRobotModel syncedRobot;
   protected LookAndStepReview<FootstepPlan> review = new LookAndStepReview<>();
   protected Consumer<FootstepPlan> autonomousOutput;
   protected Runnable planningFailedNotifier;
   protected BehaviorStateReference<LookAndStepBehavior.State> behaviorStateReference;
   protected AtomicReference<RobotSide> lastStanceSideReference;

   public static class LookAndStepFootstepPlanning extends LookAndStepFootstepPlanningTask
   {
      // instance variables
      private SingleThreadSizeOneQueueExecutor executor;
      private ControllerStatusTracker controllerStatusTracker;

      private final TypedInput<LookAndStepLocalizationResult> localizationResultInput = new TypedInput<>();
      private final TypedInput<PlanarRegionsList> planarRegionsInput = new TypedInput<>();
      private final Input footstepCompletedInput = new Input();
      private final Timer planarRegionsExpirationTimer = new Timer();
      private final Timer planningFailedTimer = new Timer();
      private BehaviorTaskSuppressor suppressor;

      public void initialize(StatusLogger statusLogger,
                             LookAndStepBehaviorParametersReadOnly lookAndStepBehaviorParameters,
                             FootstepPlannerParametersReadOnly footstepPlannerParameters,
                             SwingPlannerParametersReadOnly swingPlannerParameters,
                             UIPublisher uiPublisher,
                             SideDependentList<PlannedFootstepReadOnly> lastCommandedFootsteps,
                             FootstepPlanningModule footstepPlanningModule,
                             AtomicReference<RobotSide> lastStanceSideReference,
                             Supplier<Boolean> operatorReviewEnabledSupplier,
                             RemoteSyncedRobotModel syncedRobot,
                             BehaviorStateReference<LookAndStepBehavior.State> behaviorStateReference,
                             ControllerStatusTracker controllerStatusTracker,
                             Consumer<FootstepPlan> autonomousOutput,
                             TypedNotification<Boolean> approvalNotification)
      {
         this.statusLogger = statusLogger;
         this.lookAndStepBehaviorParameters = lookAndStepBehaviorParameters;
         this.footstepPlannerParameters = footstepPlannerParameters;
         this.swingPlannerParameters = swingPlannerParameters;
         this.uiPublisher = uiPublisher;
         this.footstepPlanningModule = footstepPlanningModule;
         this.lastStanceSideReference = lastStanceSideReference;
         this.operatorReviewEnabledSupplier = operatorReviewEnabledSupplier;
         this.behaviorStateReference = behaviorStateReference;
         this.controllerStatusTracker = controllerStatusTracker;
         this.autonomousOutput = autonomousOutput;
         this.syncedRobot = syncedRobot;

         review.initialize(
               statusLogger,
               "footstep plan",
               approvalNotification,
               footstepPlan ->
               {
                  behaviorStateReference.set(LookAndStepBehavior.State.STEPPING);
                  autonomousOutput.accept(footstepPlan);
               }
         );

         planningFailedNotifier = planningFailedTimer::reset;

         executor = new SingleThreadSizeOneQueueExecutor(getClass().getSimpleName());

         localizationResultInput.addCallback(data -> executor.queueExecution(this::evaluateAndRun));
         planarRegionsInput.addCallback(data -> executor.queueExecution(this::evaluateAndRun));
         footstepCompletedInput.addCallback(() -> executor.queueExecution(this::evaluateAndRun));

         suppressor = new BehaviorTaskSuppressor(statusLogger, "Footstep planning");
         suppressor.addCondition("Not in footstep planning state", () -> !behaviorState.equals(LookAndStepBehavior.State.FOOTSTEP_PLANNING));
         suppressor.addCondition(() -> "Regions expired. haveReceivedAny: " + planarRegionReceptionTimerSnapshot.hasBeenSet()
                                       + " timeSinceLastUpdate: " + planarRegionReceptionTimerSnapshot.getTimePassedSinceReset(),
                                 () -> planarRegionReceptionTimerSnapshot.isExpired());
         suppressor.addCondition(() -> "No regions. "
                                       + (planarRegions == null ? null : (" isEmpty: " + planarRegions.isEmpty())),
                                 () -> !(planarRegions != null && !planarRegions.isEmpty()));
         suppressor.addCondition("Planning failed recently", () -> planningFailureTimerSnapshot.isRunning());
         suppressor.addCondition("Plan being reviewed", review::isBeingReviewed);
         suppressor.addCondition("Robot disconnected", () -> robotDataReceptionTimerSnaphot.isExpired());
         suppressor.addCondition("Robot not in walking state", () -> !controllerStatusTracker.isInWalkingState());
         suppressor.addCondition(() -> "numberOfIncompleteFootsteps " + numberOfIncompleteFootsteps
                                       + " > " + lookAndStepBehaviorParameters.getAcceptableIncompleteFootsteps(),
                                 () -> numberOfIncompleteFootsteps > lookAndStepBehaviorParameters.getAcceptableIncompleteFootsteps());
      }

      public void acceptFootstepCompleted()
      {
         footstepCompletedInput.set();
      }

      public void acceptPlanarRegions(PlanarRegionsListMessage planarRegionsListMessage)
      {
         planarRegionsInput.set(PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsListMessage));
         planarRegionsExpirationTimer.reset();
      }

      public void acceptLocalizationResult(LookAndStepLocalizationResult localizationResult)
      {
         localizationResultInput.set(localizationResult);
      }

      private void evaluateAndRun()
      {
         planarRegions = planarRegionsInput.getLatest();
         planarRegionReceptionTimerSnapshot = planarRegionsExpirationTimer.createSnapshot(lookAndStepBehaviorParameters.getPlanarRegionsExpiration());
         planningFailureTimerSnapshot = planningFailedTimer.createSnapshot(lookAndStepBehaviorParameters.getWaitTimeAfterPlanFailed());
         localizationResult = localizationResultInput.getLatest();
         syncedRobot.update();
         robotDataReceptionTimerSnaphot = syncedRobot.getDataReceptionTimerSnapshot()
                                                     .withExpiration(lookAndStepBehaviorParameters.getRobotConfigurationDataExpiration());
         lastStanceSide = lastStanceSideReference.get();
         behaviorState = behaviorStateReference.get();
         numberOfIncompleteFootsteps = controllerStatusTracker.getFootstepTracker().getNumberOfIncompleteFootsteps();

         if (suppressor.evaulateShouldAccept())
         {
            performTask();
         }
      }
   }

   // snapshot data
   protected LookAndStepLocalizationResult localizationResult;
   protected PlanarRegionsList planarRegions;
   protected TimerSnapshotWithExpiration planarRegionReceptionTimerSnapshot;
   protected TimerSnapshotWithExpiration planningFailureTimerSnapshot;
   protected TimerSnapshotWithExpiration robotDataReceptionTimerSnaphot;
   protected RobotSide lastStanceSide;
   protected LookAndStepBehavior.State behaviorState;
   protected int numberOfIncompleteFootsteps;

   protected void performTask()
   {
      uiPublisher.publishToUI(PlanarRegionsForUI, planarRegions);

      Point3D closestPointAlongPath = localizationResult.getClosestPointAlongPath();
      int closestSegmentIndex = localizationResult.getClosestSegmentIndex();
      FramePose3D subGoalPoseBetweenFeet = localizationResult.getSubGoalPoseBetweenFeet();
      List<? extends Pose3DReadOnly> bodyPathPlan = localizationResult.getBodyPathPlan();
      SideDependentList<FramePose3DReadOnly> startFootPoses = localizationResult.getStanceForPlanning();

      // move point along body path plan by plan horizon
      Point3D subGoalPoint = new Point3D();
      int segmentIndexOfGoal = BodyPathPlannerTools.movePointAlongBodyPath(bodyPathPlan,
                                                                           closestPointAlongPath,
                                                                           subGoalPoint,
                                                                           closestSegmentIndex,
                                                                           lookAndStepBehaviorParameters.getPlanHorizon());

      statusLogger.info("Found next sub goal: {}", subGoalPoint);
      subGoalPoseBetweenFeet.getPosition().set(subGoalPoint);
      subGoalPoseBetweenFeet.getOrientation().set(bodyPathPlan.get(segmentIndexOfGoal + 1).getOrientation());

      // update last stepped poses to plan from; initialize to current poses
      ArrayList<FootstepForUI> startFootPosesForUI = new ArrayList<>();
      for (RobotSide side : RobotSide.values)
      {
         startFootPosesForUI.add(new FootstepForUI(side, new Pose3D(startFootPoses.get(side)), side.getPascalCaseName() + " Start"));
      }
      uiPublisher.publishToUI(StartAndGoalFootPosesForUI, startFootPosesForUI);

      RobotSide stanceSide;
      if (lastStanceSide != null)
      {
         stanceSide = lastStanceSide.getOppositeSide(); // TODO: Instead, maybe just wait for more perception data? Seems drastic
      }
      else // if first step, step with furthest foot from the goal
      {
         if (startFootPoses.get(RobotSide.LEFT) .getPosition().distance(subGoalPoseBetweenFeet.getPosition())
             <= startFootPoses.get(RobotSide.RIGHT).getPosition().distance(subGoalPoseBetweenFeet.getPosition()))
         {
            stanceSide = RobotSide.LEFT;
         }
         else
         {
            stanceSide = RobotSide.RIGHT;
         }
      }

      lastStanceSideReference.set(stanceSide);

      uiPublisher.publishToUI(SubGoalForUI, new Pose3D(subGoalPoseBetweenFeet));

      FootstepPlannerRequest footstepPlannerRequest = new FootstepPlannerRequest();
      footstepPlannerRequest.setPlanBodyPath(false);
      footstepPlannerRequest.setRequestedInitialStanceSide(stanceSide);
      footstepPlannerRequest.setStartFootPoses(startFootPoses.get(RobotSide.LEFT), startFootPoses.get(RobotSide.RIGHT));
      footstepPlannerRequest.setGoalFootPoses(footstepPlannerParameters.getIdealFootstepWidth(), subGoalPoseBetweenFeet);
      footstepPlannerRequest.setPlanarRegionsList(planarRegions);
      footstepPlannerRequest.setTimeout(lookAndStepBehaviorParameters.getFootstepPlannerTimeout());
      footstepPlannerRequest.setPerformPositionBasedSplitFractionCalculation(true);
      footstepPlannerRequest.setSwingPlannerType(SwingPlannerType.PROPORTION);

      footstepPlanningModule.getFootstepPlannerParameters().set(footstepPlannerParameters);
      footstepPlanningModule.getPostProcessHandler().getSwingPlannerParameters().set(swingPlannerParameters);
      footstepPlanningModule.addCustomTerminationCondition((plannerTime, iterations, bestPathFinalStep, bestPathSize) -> bestPathSize >= lookAndStepBehaviorParameters.getMinimumNumberOfPlannedSteps());

      statusLogger.info("Footstep planner started...");
      FootstepPlannerOutput footstepPlannerOutput = footstepPlanningModule.handleRequest(footstepPlannerRequest);
      statusLogger.info("Footstep planner completed with {}, {} step(s)",
                        footstepPlannerOutput.getFootstepPlanningResult(),
                        footstepPlannerOutput.getFootstepPlan().getNumberOfSteps());

      // print log duration?
      FootstepPlannerLogger footstepPlannerLogger = new FootstepPlannerLogger(footstepPlanningModule);
      footstepPlannerLogger.logSession();
      ThreadTools.startAThread(() -> FootstepPlannerLogger.deleteOldLogs(30), "FootstepPlanLogDeletion");

      if (footstepPlannerOutput.getFootstepPlan().getNumberOfSteps() < 1) // failed
      {
         planningFailedNotifier.run();
      }
      else
      {
         uiPublisher.publishToUI(FootstepPlanForUI, FootstepForUI.reduceFootstepPlanForUIMessager(footstepPlannerOutput.getFootstepPlan(), "Planned"));
      }

      if (operatorReviewEnabledSupplier.get())
      {
         review.review(footstepPlannerOutput.getFootstepPlan());
      }
      else
      {
         behaviorStateReference.set(LookAndStepBehavior.State.STEPPING);
         autonomousOutput.accept(footstepPlannerOutput.getFootstepPlan());
      }
   }
}
