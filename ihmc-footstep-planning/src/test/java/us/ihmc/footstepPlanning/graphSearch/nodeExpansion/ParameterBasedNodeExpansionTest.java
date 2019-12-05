package us.ihmc.footstepPlanning.graphSearch.nodeExpansion;

import org.junit.jupiter.api.Test;
import us.ihmc.commons.InterpolationTools;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.LatticeNode;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlannerParameters;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

import static us.ihmc.robotics.Assert.assertTrue;

public class ParameterBasedNodeExpansionTest
{
   private static final double epsilon = 1e-6;
   @Test
   public void testExpansionAlongBoundsFromOrigin()
   {
      DefaultFootstepPlannerParameters parameters = new DefaultFootstepPlannerParameters();
      ParameterBasedNodeExpansion expansion = new ParameterBasedNodeExpansion(parameters);

      double maxYaw = 1.2;
      double minYaw = -0.5;
      double yawReduction = 0.5;
      parameters.setMaximumStepYaw(maxYaw);
      parameters.setMinimumStepYaw(minYaw);
      parameters.setStepYawReductionFactorAtMaxReach(yawReduction);

      double maxYawAtFullLength = yawReduction * maxYaw;
      double minYawAtFullLength = yawReduction * minYaw;

      Set<FootstepNode> childNodes = expansion.expandNode(new FootstepNode(0.0, 0.0, 0.0, RobotSide.LEFT));
      FootstepNode mostForward = getExtremumNode(childNodes, Comparator.comparingDouble(node -> node.getX()));
      FootstepNode furthestReach = getExtremumNode(childNodes, Comparator.comparingDouble(node -> getReachAtNode(node, parameters.getIdealFootstepWidth())));
      FootstepNode mostBackward = getExtremumNode(childNodes, Comparator.comparingDouble(node -> -node.getX()));
      FootstepNode mostInward = getExtremumNode(childNodes, Comparator.comparingDouble(node -> node.getY()));
      FootstepNode mostOutward = getExtremumNode(childNodes, Comparator.comparingDouble(node -> -node.getY()));
      FootstepNode mostOutwardYawed = getExtremumNode(childNodes, Comparator.comparingDouble(node -> -snapToCircle(node.getYaw())));
      FootstepNode mostInwardYawed = getExtremumNode(childNodes, Comparator.comparingDouble(node -> snapToCircle(node.getYaw())));

      assertTrue(mostForward.getX() < parameters.getMaximumStepReach() + epsilon);
      assertTrue(mostBackward.getX() > parameters.getMinimumStepLength() - epsilon);
      assertTrue(mostInward.getY() < -parameters.getMinimumStepWidth() + epsilon);
      assertTrue(mostOutward.getY() > -parameters.getMaximumStepWidth() - epsilon);

      double mostOutwardYawedReach = getReachAtNode(mostOutwardYawed, parameters.getIdealFootstepWidth());
      double mostInwardYawedReach = getReachAtNode(mostOutwardYawed, parameters.getIdealFootstepWidth());
      double mostOutwardYawMax = InterpolationTools.linearInterpolate(maxYaw, maxYawAtFullLength, mostOutwardYawedReach / parameters.getMaximumStepReach());
      double mostInwardYawMin = InterpolationTools.linearInterpolate(minYaw, minYawAtFullLength, mostInwardYawedReach / parameters.getMaximumStepReach());
      double minOutwardYaw = snapToYawGrid(-mostOutwardYawMax - epsilon);
      double maxInwardYaw = snapToYawGrid(-mostInwardYawMin + epsilon);
      assertTrue(mostOutwardYawed.getYaw() > minOutwardYaw);
      assertTrue(mostInwardYawed.getYaw() < maxInwardYaw + epsilon);
      assertTrue(getReachAtNode(furthestReach, parameters.getIdealFootstepWidth()) < parameters.getMaximumStepReach());
   }

   private static double getReachAtNode(FootstepNode node, double idealWidth)
   {
      double relativeYToIdeal = node.getY() - node.getRobotSide().negateIfRightSide(idealWidth);
      return EuclidCoreTools.normSquared(node.getX(), relativeYToIdeal);
   }

   private static double snapToCircle(double yaw)
   {
      if (yaw < Math.PI)
         return yaw;
      else
         return -(2.0 * Math.PI - yaw);
   }

   private static double snapToYawGrid(double yaw)
   {
      return LatticeNode.gridSizeYaw * Math.floorMod((int) (Math.round((yaw) / LatticeNode.gridSizeYaw)), LatticeNode.yawDivisions);
   }

   private FootstepNode getExtremumNode(Collection<FootstepNode> nodes, Comparator<FootstepNode> comparator)
   {
      FootstepNode extremumNode = null;
      for (FootstepNode node : nodes)
      {
         if (extremumNode == null)
            extremumNode = node;
         else if (comparator.compare(node, extremumNode) == 1)
            extremumNode = node;
      }

      return extremumNode;
   }
}
