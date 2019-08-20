package us.ihmc.footstepPlanning.graphSearch.heuristics;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNodeTools;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.yoVariables.providers.DoubleProvider;

public class DistanceAndYawBasedHeuristics extends CostToGoHeuristics
{
   private final FootstepPlannerParametersReadOnly parameters;
   private final FootstepNodeSnapper snapper;

   public DistanceAndYawBasedHeuristics(FootstepNodeSnapper snapper, DoubleProvider weight, FootstepPlannerParametersReadOnly parameters)
   {
      super(weight);
      this.snapper = snapper;
      this.parameters = parameters;
   }

   @Override
   protected double computeHeuristics(FootstepNode node, FootstepNode goalNode)
   {
      Point2D goalPoint = goalNode.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth());
      Point2D nodeMidFootPoint = node.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth());

      double euclideanDistance = nodeMidFootPoint.distance(goalPoint);

      double referenceYaw = computeReferenceYaw(node, goalNode);
      double yaw = AngleTools.computeAngleDifferenceMinusPiToPi(node.getYaw(), referenceYaw);

      RigidBodyTransform nodeSnapTransform = snapper.snapFootstepNode(node).getSnapTransform();
      RigidBodyTransform goalNodeSnapTransform = snapper.snapFootstepNode(goalNode).getSnapTransform();

      double heightCost = 0.0;

      if (!nodeSnapTransform.containsNaN() && !goalNodeSnapTransform.containsNaN())
      {
         double heightChange = goalNodeSnapTransform.getTranslationVector().getZ() - nodeSnapTransform.getTranslationVector().getZ();

         // add a two times multiplier because both feet have to move
         if (heightChange > 0)
            heightCost = parameters.getStepUpWeight() * 2.0 * heightChange;
         else
            heightCost = -parameters.getStepDownWeight() * 2.0 * heightChange;
      }

      double minSteps = euclideanDistance / parameters.getMaximumStepReach() + Math.abs(yaw) / (0.5 * parameters.getMaximumStepYaw());
      return euclideanDistance + parameters.getYawWeight() * Math.abs(yaw) + heightCost + parameters.getCostPerStep() * minSteps;
   }

   private double computeReferenceYaw(FootstepNode node, FootstepNode goalNode)
   {
      double distanceToGoal = node.euclideanDistance(goalNode);
      double finalTurnProximity = parameters.getFinalTurnProximity();

      double minimumBlendDistance = 0.75 * finalTurnProximity;
      double maximumBlendDistance = 1.25 * finalTurnProximity;

      double pathHeading = Math.atan2(goalNode.getY() - node.getY(), goalNode.getX() - node.getX());
      pathHeading = AngleTools.trimAngleMinusPiToPi(pathHeading);

      double yawMultiplier;
      if(distanceToGoal < minimumBlendDistance)
         yawMultiplier = 0.0;
      else if(distanceToGoal > maximumBlendDistance)
         yawMultiplier = 1.0;
      else
         yawMultiplier = (distanceToGoal - minimumBlendDistance) / (maximumBlendDistance - minimumBlendDistance);

      double referenceHeading = yawMultiplier * pathHeading;
      referenceHeading += (1.0 - yawMultiplier) * goalNode.getYaw();
      return AngleTools.trimAngleMinusPiToPi(referenceHeading);
   }
}
