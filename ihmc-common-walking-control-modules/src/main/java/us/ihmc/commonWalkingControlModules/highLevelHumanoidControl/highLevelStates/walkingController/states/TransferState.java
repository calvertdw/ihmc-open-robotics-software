package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.walkingController.states;

import us.ihmc.commonWalkingControlModules.capturePoint.BalanceManager;
import us.ihmc.commonWalkingControlModules.capturePoint.CenterOfMassHeightManager;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.WalkingFailureDetectionControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.WrenchCommand;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelControlManagerFactory;
import us.ihmc.commonWalkingControlModules.messageHandlers.WalkingMessageHandler;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.referenceFrame.FrameConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint3DBasics;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public abstract class TransferState extends WalkingState
{
   private static final boolean ENABLE_TOUCHDOWN_STATE = true;
   protected final RobotSide transferToSide;

   protected final WalkingMessageHandler walkingMessageHandler;
   protected final HighLevelHumanoidControllerToolbox controllerToolbox;
   protected final WalkingFailureDetectionControlModule failureDetectionControlModule;

   protected final CenterOfMassHeightManager comHeightManager;
   protected final BalanceManager balanceManager;
   protected final PelvisOrientationManager pelvisOrientationManager;
   protected final FeetManager feetManager;

   private final FramePoint2D desiredICPLocal = new FramePoint2D();
   private final FramePoint2D capturePoint2d = new FramePoint2D();
   private final FramePoint2D desiredCMP = new FramePoint2D();

   private final FramePoint2D filteredDesiredCoP = new FramePoint2D();
   private final FramePoint2D desiredCoP = new FramePoint2D();
   private final FramePoint3D nextExitCMP = new FramePoint3D();

   private final YoBoolean touchdownIsEnabled;
   private final YoBoolean isInTouchdown;
   private final YoDouble touchdownDuration;
   private final YoDouble icpErrorThresholdToAbortTouchdown;
   private final FootstepTiming stepTiming = new FootstepTiming();

   private final Footstep nextFootstep = new Footstep();

   private final WrenchCommand unloadingWrenchCommand;
   private final YoBoolean isUnloading;
   private final DoubleProvider unloadFraction;
   private final YoDouble maxZForce;
   private final double robotWeight;

   public TransferState(WalkingStateEnum transferStateEnum, WalkingControllerParameters walkingControllerParameters,
                        WalkingMessageHandler walkingMessageHandler, HighLevelHumanoidControllerToolbox controllerToolbox,
                        HighLevelControlManagerFactory managerFactory, WalkingFailureDetectionControlModule failureDetectionControlModule,
                        DoubleProvider unloadFraction, YoVariableRegistry parentRegistry)
   {
      super(transferStateEnum, parentRegistry);
      this.transferToSide = transferStateEnum.getTransferToSide();
      this.walkingMessageHandler = walkingMessageHandler;
      this.failureDetectionControlModule = failureDetectionControlModule;
      this.controllerToolbox = controllerToolbox;
      this.unloadFraction = unloadFraction;

      robotWeight = controllerToolbox.getFullRobotModel().getTotalMass() * controllerToolbox.getGravityZ();

      comHeightManager = managerFactory.getOrCreateCenterOfMassHeightManager();
      balanceManager = managerFactory.getOrCreateBalanceManager();
      pelvisOrientationManager = managerFactory.getOrCreatePelvisOrientationManager();
      feetManager = managerFactory.getOrCreateFeetManager();
      touchdownDuration = new YoDouble("touchdownDuration", registry);
      icpErrorThresholdToAbortTouchdown = new YoDouble("icpErrorThresholdToAbortTouchdown", registry);
      icpErrorThresholdToAbortTouchdown.set(walkingControllerParameters.getICPErrorThresholdToSpeedUpSwing());
      isInTouchdown = new YoBoolean("isInTouchdown", registry);
      touchdownIsEnabled = new YoBoolean("touchdownIsEnabled", registry);
      touchdownIsEnabled.set(ENABLE_TOUCHDOWN_STATE);

      if (unloadFraction != null)
      {
         isUnloading = new YoBoolean(transferToSide.getOppositeSide().getLowerCaseName() + "FootIsUnloading", registry);
         maxZForce = new YoDouble(transferToSide.getOppositeSide().getLowerCaseName() + "MaxZForce", registry);
         RigidBody footToUnload = controllerToolbox.getFullRobotModel().getFoot(transferToSide.getOppositeSide());
         unloadingWrenchCommand = new WrenchCommand();
         unloadingWrenchCommand.setRigidBody(footToUnload);
         unloadingWrenchCommand.getSelectionMatrix().clearSelection();
         unloadingWrenchCommand.getSelectionMatrix().setSelectionFrame(ReferenceFrame.getWorldFrame());
         unloadingWrenchCommand.getSelectionMatrix().selectLinearZ(true);
         unloadingWrenchCommand.getWrench().setToZero(footToUnload.getBodyFixedFrame(), ReferenceFrame.getWorldFrame());
         unloadingWrenchCommand.setConstraintType(us.ihmc.commonWalkingControlModules.controllerCore.command.ConstraintType.LEQ_INEQUALITY);
      }
      else
      {
         isUnloading = null;
         maxZForce = null;
         unloadingWrenchCommand = null;
      }
   }

   @Override
   public RobotSide getTransferToSide()
   {
      return transferToSide;
   }

   @Override
   public void doAction(double timeInState)
   {
      boolean touchdownTimeElapsed = timeInState > touchdownDuration.getDoubleValue();
      boolean icpErrorTooGreat = balanceManager.getICPErrorMagnitude() > icpErrorThresholdToAbortTouchdown.getDoubleValue();

      if (isInTouchdown.getBooleanValue() && (touchdownTimeElapsed || icpErrorTooGreat))
      {
         feetManager.initializeContactStatesForDoubleSupport(transferToSide);
         updateICPPlan();
         isInTouchdown.set(false);
      }

      if (!isInTouchdown.getBooleanValue())
      {
         feetManager.updateContactStatesInDoubleSupport(transferToSide);
      }

      switchToToeOffIfPossible();

      // Always do this so that when a foot slips or is loaded in the air, the height gets adjusted.
      comHeightManager.setSupportLeg(transferToSide);

      if (isUnloading != null && unloadFraction.getValue() > 0.0)
      {
         double percentInTransfer = MathTools.clamp(timeInState / stepTiming.getTransferTime(), 0.0, 1.0);
         isUnloading.set(percentInTransfer > unloadFraction.getValue());
         if (isUnloading.getValue())
         {
            double percentInUnloading = (percentInTransfer - unloadFraction.getValue()) / (1.0 - unloadFraction.getValue());
            maxZForce.set(robotWeight * (1.0 - percentInUnloading));
            unloadingWrenchCommand.getWrench().setLinearPartZ(maxZForce.getValue());
            feetManager.addCommand(unloadingWrenchCommand);
         }
         else
         {
            maxZForce.setToNaN();
         }
      }
   }

   @Override
   public boolean isDone(double timeInState)
   {
      if (isInTouchdown.getBooleanValue())
      {
         return false;
      }

      //If we're using a precomputed icp trajectory we can't rely on the icp planner's state to dictate when to exit transfer.
      boolean transferTimeElapsedUnderPrecomputedICPPlan = false;
      if (balanceManager.isPrecomputedICPPlannerActive())
      {
         transferTimeElapsedUnderPrecomputedICPPlan = timeInState > (walkingMessageHandler.getNextTransferTime() + touchdownDuration.getDoubleValue());
      }

      if (balanceManager.isICPPlanDone() || transferTimeElapsedUnderPrecomputedICPPlan)
      {
         balanceManager.getCapturePoint(capturePoint2d);
         FrameConvexPolygon2D supportPolygonInWorld = controllerToolbox.getBipedSupportPolygons().getSupportPolygonInWorld();
         boolean isICPInsideSupportPolygon = supportPolygonInWorld.isPointInside(capturePoint2d);

         if (!isICPInsideSupportPolygon)
            return true;
         else
            return balanceManager.isTransitionToSingleSupportSafe(transferToSide);
      }

      return false;
   }

   public void switchToToeOffIfPossible()
   {
      RobotSide trailingLeg = transferToSide.getOppositeSide();

      boolean shouldComputeToeLineContact = feetManager.shouldComputeToeLineContact();
      boolean shouldComputeToePointContact = feetManager.shouldComputeToePointContact();

      if (shouldComputeToeLineContact || shouldComputeToePointContact)
      {
         balanceManager.getDesiredCMP(desiredCMP);
         balanceManager.getDesiredICP(desiredICPLocal);
         balanceManager.getCapturePoint(capturePoint2d);
         balanceManager.getNextExitCMP(nextExitCMP);

         controllerToolbox.getFilteredDesiredCenterOfPressure(controllerToolbox.getContactableFeet().get(trailingLeg), filteredDesiredCoP);
         controllerToolbox.getDesiredCenterOfPressure(controllerToolbox.getContactableFeet().get(trailingLeg), desiredCoP);

         feetManager.updateToeOffStatusDoubleSupport(trailingLeg, nextFootstep, nextExitCMP, desiredCMP, desiredCoP, desiredICPLocal, capturePoint2d);

         if (feetManager.okForPointToeOff() && shouldComputeToePointContact)
            feetManager.requestPointToeOff(trailingLeg, nextExitCMP, filteredDesiredCoP);
         else if (feetManager.okForLineToeOff() && shouldComputeToeLineContact)
            feetManager.requestLineToeOff(trailingLeg, nextExitCMP, filteredDesiredCoP);
      }
   }

   @Override
   public void onEntry()
   {
      if (walkingMessageHandler.hasUpcomingFootsteps())
      {
         walkingMessageHandler.peekTiming(0, stepTiming);
      }
      else
      {
         stepTiming.setTimings(Double.NaN, Double.NaN, Double.NaN);
      }

      adjustTouchdownDuration();
      touchdownDuration.set(walkingMessageHandler.getNextTouchdownDuration());
      boolean supportFootWasSwinging = feetManager.getCurrentConstraintType(transferToSide) == ConstraintType.SWING;
      if (supportFootWasSwinging && touchdownDuration.getDoubleValue() > controllerToolbox.getControlDT() && touchdownIsEnabled.getBooleanValue())
      {
         feetManager.initializeContactStatesForTouchdown(transferToSide);
         isInTouchdown.set(true);
      }
      else
      {
         feetManager.initializeContactStatesForDoubleSupport(transferToSide);
         isInTouchdown.set(false);
         updateICPPlan();
      }

      double extraToeOffHeight = 0.0;
      RobotSide swingSide = transferToSide.getOppositeSide();
      if (feetManager.canDoDoubleSupportToeOff(nextFootstep, swingSide))
         extraToeOffHeight = feetManager.getToeOffManager().getExtraCoMMaxHeightWithToes();

      Footstep footstep = walkingMessageHandler.getFootstepAtCurrentLocation(transferToSide);
      FixedFramePoint3DBasics transferFootPosition = footstep.getFootstepPose().getPosition();
      double transferTime = walkingMessageHandler.getNextTransferTime();
      comHeightManager.transfer(transferFootPosition, transferTime, swingSide, extraToeOffHeight);
   }

   /**
    * If we're using absolute timings and the swing was too long, we should reduce the touchdown
    * duration
    */
   private void adjustTouchdownDuration()
   {
      if (!walkingMessageHandler.isNextFootstepUsingAbsoluteTiming())
         return;

      double originalSwingTime = stepTiming.getSwingTime();
      double currentTime = controllerToolbox.getYoTime().getDoubleValue();
      double timeInFootstepPlan = currentTime - stepTiming.getExecutionStartTime();
      double adjustedTransferTime = stepTiming.getSwingStartTime() - timeInFootstepPlan;
      double percentageToShrinkTouchdown = MathTools.clamp(adjustedTransferTime / stepTiming.getTransferTime(), 0.0, 1.0);
      double touchdownDuration = stepTiming.getTouchdownDuration();

      if (Double.isFinite(touchdownDuration))
      {
         touchdownDuration = Math.max(0.0, touchdownDuration * percentageToShrinkTouchdown);
      }

      stepTiming.setTimings(originalSwingTime, touchdownDuration, adjustedTransferTime);
      walkingMessageHandler.adjustTimings(0, stepTiming.getSwingTime(), touchdownDuration, stepTiming.getTransferTime());
   }

   protected void updateICPPlan()
   {
      balanceManager.clearICPPlan();
      controllerToolbox.updateBipedSupportPolygons(); // need to always update biped support polygons after a change to the contact states

      if (walkingMessageHandler.hasUpcomingFootsteps())
      {
         walkingMessageHandler.peekFootstep(0, nextFootstep);
         failureDetectionControlModule.setNextFootstep(nextFootstep);
         balanceManager.setUpcomingFootstep(nextFootstep);
      }
      else
      {
         failureDetectionControlModule.setNextFootstep(null);
         balanceManager.setUpcomingFootstep(null);
      }

      balanceManager.resetPushRecovery();

      double transferTime = walkingMessageHandler.getNextTransferTime();
      pelvisOrientationManager.setTrajectoryTime(transferTime);
   }

   public boolean isInitialTransfer()
   {
      return getPreviousWalkingStateEnum() == WalkingStateEnum.STANDING;
   }

   @Override
   public void onExit()
   {
      if (isUnloading != null)
      {
         isUnloading.set(false);
      }
      feetManager.reset();
   }

   public boolean isInTouchdown()
   {
      return isInTouchdown.getBooleanValue();
   }
}