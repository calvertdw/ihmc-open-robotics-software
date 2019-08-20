package us.ihmc.footstepPlanning.graphSearch.heuristics;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.pathPlanning.bodyPathPlanner.BodyPathPlanner;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.yoVariables.providers.DoubleProvider;

public class BodyPathHeuristics extends CostToGoHeuristics
{
   //TODO: Make these parameters or something, rather than hardcoded here.
   private static final double pathViolationWeight = 30.0;
   private static final double distanceFromPathTolerance = 0.2;
   private static final double deltaYawFromPathTolerance = 0.2;

   private final BodyPathPlanner bodyPath;
   private final FootstepPlannerParametersReadOnly parameters;

   private double goalAlpha = 1.0;

   public BodyPathHeuristics(DoubleProvider weight, FootstepPlannerParametersReadOnly parameters, BodyPathPlanner bodyPath)
   {
      super(weight);

      this.bodyPath = bodyPath;
      this.parameters = parameters;
   }

   @Override
   protected double computeHeuristics(FootstepNode node, FootstepNode goalNode)
   {
      Point2D midFootPoint = node.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth());
      Pose2D closestPointOnPath = new Pose2D();

      double alpha = bodyPath.getClosestPoint(midFootPoint, closestPointOnPath);
      alpha = MathTools.clamp(alpha, 0.0, goalAlpha);
      bodyPath.getPointAlongPath(alpha, closestPointOnPath);

      double distanceToPath = closestPointOnPath.getPosition().distance(midFootPoint);
      double croppedDistanceToPath = Math.max(0.0, distanceToPath - distanceFromPathTolerance);

      double pathLength = bodyPath.computePathLength(alpha) - bodyPath.computePathLength(goalAlpha);
      double remainingDistance = pathLength + distanceToPath - croppedDistanceToPath;;
      double pathDistanceViolationCost = pathViolationWeight * croppedDistanceToPath;

      double referenceYaw = closestPointOnPath.getYaw();
      double yawDifferenceFromPathYaw = Math.abs(AngleTools.computeAngleDifferenceMinusPiToPi(node.getYaw(), referenceYaw));
      double remainingYawToGoal =  Math.abs(AngleTools.computeAngleDifferenceMinusPiToPi(goalNode.getYaw(), referenceYaw));

      double croppedYawDifferenceFromPathYaw = Math.max(0.0, yawDifferenceFromPathYaw - deltaYawFromPathTolerance);

      double remainingYaw = remainingYawToGoal + yawDifferenceFromPathYaw - croppedYawDifferenceFromPathYaw;
      double pathYawViolationCost = pathViolationWeight * croppedYawDifferenceFromPathYaw;

      double minSteps = remainingDistance / parameters.getMaximumStepReach() + Math.abs(yawDifferenceFromPathYaw + remainingYawToGoal) / (0.5 * parameters.getMaximumStepYaw());
      return remainingDistance + pathDistanceViolationCost + pathYawViolationCost + parameters.getYawWeight() * remainingYaw + parameters.getCostPerStep() * minSteps;
   }

   public void setGoalAlpha(double alpha)
   {
      goalAlpha = alpha;
   }
}
