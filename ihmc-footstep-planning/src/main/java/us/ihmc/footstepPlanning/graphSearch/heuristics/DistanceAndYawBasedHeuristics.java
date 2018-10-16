package us.ihmc.footstepPlanning.graphSearch.heuristics;

import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class DistanceAndYawBasedHeuristics extends CostToGoHeuristics
{
   private final FootstepPlannerParameters parameters;

   public DistanceAndYawBasedHeuristics(FootstepPlannerParameters parameters, YoVariableRegistry registry)
   {
      super(registry);
      this.parameters = parameters;
   }

   @Override
   protected double computeHeuristics(FootstepNode node, FootstepNode goalNode)
   {
      Point2D goalPoint = goalNode.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth());
      Point2D nodeMidFootPoint = node.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth());
      double euclideanDistance = nodeMidFootPoint.distance(goalPoint);
      double yaw = AngleTools.computeAngleDifferenceMinusPiToPi(node.getYaw(), goalNode.getYaw());
      double minSteps = euclideanDistance / parameters.getMaximumStepReach();
      return euclideanDistance + parameters.getYawWeight() * Math.abs(yaw) + parameters.getCostPerStep() * minSteps;
   }
}
