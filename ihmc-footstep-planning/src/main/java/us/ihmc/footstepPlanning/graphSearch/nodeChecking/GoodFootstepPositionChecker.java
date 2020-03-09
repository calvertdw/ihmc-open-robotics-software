package us.ihmc.footstepPlanning.graphSearch.nodeChecking;

import us.ihmc.commons.InterpolationTools;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.BipedalFootstepPlannerNodeRejectionReason;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.referenceFrames.TransformReferenceFrame;
import us.ihmc.robotics.referenceFrames.ZUpFrame;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.function.UnaryOperator;

public class GoodFootstepPositionChecker
{
   private final FootstepPlannerParametersReadOnly parameters;
   private final FootstepNodeSnapper snapper;

   private final TransformReferenceFrame startOfSwingFrame = new TransformReferenceFrame("startOfSwingFrame", ReferenceFrame.getWorldFrame());
   private final TransformReferenceFrame stanceFootFrame = new TransformReferenceFrame("stanceFootFrame", ReferenceFrame.getWorldFrame());
   private final TransformReferenceFrame candidateFootFrame = new TransformReferenceFrame("candidateFootFrame", ReferenceFrame.getWorldFrame());
   private final ZUpFrame startOfSwingZUpFrame = new ZUpFrame(ReferenceFrame.getWorldFrame(), startOfSwingFrame, "startOfSwingZUpFrame");
   private final ZUpFrame stanceFootZUpFrame = new ZUpFrame(ReferenceFrame.getWorldFrame(), stanceFootFrame, "stanceFootZUpFrame");
   private final FramePose3D stanceFootPose = new FramePose3D();
   private final FramePose3D candidateFootPose = new FramePose3D();

   private BipedalFootstepPlannerNodeRejectionReason rejectionReason;
   private UnaryOperator<FootstepNode> parentNodeSupplier;

   public GoodFootstepPositionChecker(FootstepPlannerParametersReadOnly parameters, FootstepNodeSnapper snapper)
   {
      this.parameters = parameters;
      this.snapper = snapper;
   }

   public void setParentNodeSupplier(UnaryOperator<FootstepNode> parentNodeSupplier)
   {
      this.parentNodeSupplier = parentNodeSupplier;
   }

   public boolean isNodeValid(FootstepNode candidateNode, FootstepNode stanceNode)
   {
      RobotSide stepSide = candidateNode.getRobotSide();

      FootstepNodeSnapData candidateNodeSnapData = snapper.snapFootstepNode(candidateNode);
      FootstepNodeSnapData stanceNodeSnapData = snapper.snapFootstepNode(stanceNode);

      candidateFootFrame.setTransformAndUpdate(candidateNodeSnapData.getOrComputeSnappedNodeTransform(candidateNode));
      stanceFootFrame.setTransformAndUpdate(stanceNodeSnapData.getOrComputeSnappedNodeTransform(stanceNode));
      stanceFootZUpFrame.update();

      candidateFootPose.setToZero(candidateFootFrame);
      candidateFootPose.changeFrame(stanceFootZUpFrame);

      stanceFootPose.setToZero(stanceFootFrame);
      stanceFootPose.changeFrame(ReferenceFrame.getWorldFrame());

      double stepLength = candidateFootPose.getX();
      double stepWidth = stepSide.negateIfRightSide(candidateFootPose.getY());
      double stepReachXY = EuclidGeometryTools.pythagorasGetHypotenuse(Math.abs(candidateFootPose.getX()),
                                                                       Math.abs(stepWidth - parameters.getIdealFootstepWidth()));
      double stepHeight = candidateFootPose.getZ();

      if (stepWidth < parameters.getMinimumStepWidth())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_NOT_WIDE_ENOUGH;
         return false;
      }
      else if (stepWidth > parameters.getMaximumStepWidth())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE;
         return false;
      }
      else if (stepLength < parameters.getMinimumStepLength())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_NOT_LONG_ENOUGH;
         return false;
      }
      else if (Math.abs(stepHeight) > parameters.getMaximumStepZ())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_HIGH_OR_LOW;
         return false;
      }

      double alpha = Math.max(0.0, - stanceFootPose.getPitch() / parameters.getMinimumSurfaceInclineRadians());
      double minZ = InterpolationTools.linearInterpolate(Math.abs(parameters.getMaximumStepZ()), Math.abs(parameters.getMinimumStepZWhenFullyPitched()), alpha);
      double minX = InterpolationTools.linearInterpolate(Math.abs(parameters.getMaximumStepReach()), parameters.getMaximumStepXWhenFullyPitched(), alpha);
      double stepDownFraction = - stepHeight / minZ;
      double stepForwardFraction = stepLength / minX;

      // TODO eliminate the 1.5, and look at the actual max step z and max step reach to ensure those are valid if there's not any pitching
      if (stepDownFraction > 1.0 || stepForwardFraction > 1.0 || (stepDownFraction + stepForwardFraction > 1.5))
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_LOW_AND_FORWARD_WHEN_PITCHED;
         return false;
      }

      double maxReach = parameters.getMaximumStepReach();
      if (stepHeight < -Math.abs(parameters.getMaximumStepZWhenForwardAndDown()))
      {
         if (stepLength > parameters.getMaximumStepXWhenForwardAndDown())
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FORWARD_AND_DOWN;
            return false;
         }

         if (stepWidth > parameters.getMaximumStepYWhenForwardAndDown())
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE_AND_DOWN;
            return false;
         }

         maxReach = EuclidCoreTools.norm(parameters.getMaximumStepXWhenForwardAndDown(), parameters.getMaximumStepYWhenForwardAndDown() - parameters.getIdealFootstepWidth());
      }

      if (stepReachXY > parameters.getMaximumStepReach())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR;
         return false;
      }

      if (stepHeight > parameters.getMaximumStepZWhenSteppingUp())
      {
         if (stepReachXY > parameters.getMaximumStepReachWhenSteppingUp())
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_AND_HIGH;
            return false;
         }
         if (stepWidth > parameters.getMaximumStepWidthWhenSteppingUp())
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE_AND_HIGH;
            return false;
         }

         maxReach = parameters.getMaximumStepReachWhenSteppingUp();
      }

      double stepReach3D = EuclidCoreTools.norm(stepReachXY, stepHeight);
      double maxInterpolationFactor = Math.max(stepReach3D / maxReach, Math.abs(stepHeight / parameters.getMaximumStepZ()));
      maxInterpolationFactor = Math.min(maxInterpolationFactor, 1.0);
      double maxYaw = InterpolationTools.linearInterpolate(parameters.getMaximumStepYaw(), (1.0 - parameters.getStepYawReductionFactorAtMaxReach()) * parameters.getMaximumStepYaw(),
                                                           maxInterpolationFactor);
      double minYaw = InterpolationTools.linearInterpolate(parameters.getMinimumStepYaw(), (1.0 - parameters.getStepYawReductionFactorAtMaxReach()) * parameters.getMinimumStepYaw(),
                                                           maxInterpolationFactor);
      double yawDelta = AngleTools.computeAngleDifferenceMinusPiToPi(candidateNode.getYaw(), stanceNode.getYaw());
      if (!MathTools.intervalContains(stepSide.negateIfRightSide(yawDelta), minYaw, maxYaw))
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_YAWS_TOO_MUCH;
         return false;
      }

      // Check reach from start of swing
      FootstepNode grandParentNode;
      FootstepNodeSnapData grandparentNodeSnapData;
      double alphaSoS = parameters.getTranslationScaleFromGrandparentNode();
      if (alphaSoS > 0.0 && parentNodeSupplier != null && (grandParentNode = parentNodeSupplier.apply(stanceNode)) != null
          && (grandparentNodeSnapData = snapper.snapFootstepNode(grandParentNode)) != null)
      {
         startOfSwingFrame.setTransformAndUpdate(grandparentNodeSnapData.getOrComputeSnappedNodeTransform(grandParentNode));
         startOfSwingZUpFrame.update();
         candidateFootPose.changeFrame(startOfSwingZUpFrame);
         double swingHeight = candidateFootPose.getZ();
         double swingReach = EuclidGeometryTools.pythagorasGetHypotenuse(Math.abs(candidateFootPose.getX()), Math.abs(candidateFootPose.getY()));

         if (swingHeight > parameters.getMaximumStepZWhenSteppingUp())
         {
            if (swingReach > alphaSoS * parameters.getMaximumStepReachWhenSteppingUp())
            {
               rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_AND_HIGH;
               return false;
            }
         }
      }

      return true;
   }

   public BipedalFootstepPlannerNodeRejectionReason getRejectionReason()
   {
      return rejectionReason;
   }
}
