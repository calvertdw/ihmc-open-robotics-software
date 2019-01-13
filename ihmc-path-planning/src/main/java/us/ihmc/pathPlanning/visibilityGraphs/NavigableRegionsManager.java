package us.ihmc.pathPlanning.visibilityGraphs;

import java.util.*;

import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.*;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsCostParameters;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsParameters;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pathPlanning.visibilityGraphs.tools.ClusterTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.OcclusionTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityTools;
import us.ihmc.robotics.geometry.PlanarRegion;

public class NavigableRegionsManager
{
   private final static boolean debug = false;

   private final static double bodyWidth = 1.1;
   private final static double rotationWeight = 2.0;

   private final VisibilityGraphsParameters parameters;
   private final VisibilityGraphsCostParameters costParameters;

   private final VisibilityMapSolution visibilityMapSolution = new VisibilityMapSolution();

   private VisibilityGraph visibilityGraph;
   private VisibilityGraphNode startNode;
   private VisibilityGraphNode goalNode;
   private PriorityQueue<VisibilityGraphNode> stack;
   private HashSet<VisibilityGraphNode> expandedNodes;

   public NavigableRegionsManager()
   {
      this(null, null);
   }

   public NavigableRegionsManager(VisibilityGraphsParameters parameters)
   {
      this(parameters, null);
   }

   public NavigableRegionsManager(List<PlanarRegion> regions)
   {
      this(null, regions);
   }

   public NavigableRegionsManager(VisibilityGraphsParameters parameters, List<PlanarRegion> regions)
   {
      visibilityMapSolution.setNavigableRegions(new NavigableRegions(parameters, regions));
      this.parameters = parameters == null ? new DefaultVisibilityGraphParameters() : parameters;
      this.costParameters = new DefaultVisibilityGraphsCostParameters();
   }

   private static ArrayList<VisibilityMapWithNavigableRegion> createListOfVisibilityMapsWithNavigableRegions(NavigableRegions navigableRegions)
   {
      ArrayList<VisibilityMapWithNavigableRegion> list = new ArrayList<>();

      List<NavigableRegion> naviableRegionsList = navigableRegions.getNaviableRegionsList();

      for (NavigableRegion navigableRegion : naviableRegionsList)
      {
         VisibilityMapWithNavigableRegion visibilityMapWithNavigableRegion = new VisibilityMapWithNavigableRegion(navigableRegion);
         list.add(visibilityMapWithNavigableRegion);
      }

      return list;
   }

   public List<VisibilityMapWithNavigableRegion> getNavigableRegionsList()
   {
      return visibilityMapSolution.getVisibilityMapsWithNavigableRegions();
   }

   public void setPlanarRegions(List<PlanarRegion> planarRegions)
   {
      visibilityMapSolution.getNavigableRegions().setPlanarRegions(planarRegions);
   }

   public List<Point3DReadOnly> calculateBodyPath(final Point3DReadOnly start, final Point3DReadOnly goal)
   {
      boolean fullyExpandVisibilityGraph = false;
      return calculateBodyPath(start, goal, fullyExpandVisibilityGraph);
   }

   public List<Point3DReadOnly> calculateBodyPath(final Point3DReadOnly start, final Point3DReadOnly goal, boolean fullyExpandVisibilityGraph)
   {
      return calculateVisibilityMapWhileFindingPath(start, goal, fullyExpandVisibilityGraph);
   }

   private List<Point3DReadOnly> calculateVisibilityMapWhileFindingPath(Point3DReadOnly startInWorld, Point3DReadOnly goalInWorld,
                                                                        boolean fullyExpandVisibilityGraph)
   {
      if (!initialize(startInWorld, goalInWorld, fullyExpandVisibilityGraph))
         return null;

      return planInternal();
   }

   private boolean initialize(Point3DReadOnly startInWorld, Point3DReadOnly goalInWorld, boolean fullyExpandVisibilityGraph)
   {
      if (!checkIfStartAndGoalAreValid(startInWorld, goalInWorld))
         return false;

      NavigableRegions navigableRegions = visibilityMapSolution.getNavigableRegions();
      navigableRegions.filterPlanarRegionsWithBoundingCapsule(startInWorld, goalInWorld, parameters.getExplorationDistanceFromStartGoal());

      navigableRegions.createNavigableRegions();

      visibilityGraph = new VisibilityGraph(navigableRegions, parameters.getInterRegionConnectionFilter());

      if (fullyExpandVisibilityGraph)
         visibilityGraph.fullyExpandVisibilityGraph();

      double searchHostEpsilon = parameters.getSearchHostRegionEpsilon();
      startNode = visibilityGraph.setStart(startInWorld, searchHostEpsilon);
      goalNode = visibilityGraph.setGoal(goalInWorld, searchHostEpsilon);


      PathNodeComparator comparator = new PathNodeComparator();
      stack = new PriorityQueue<>(comparator);

      startNode.setEdgesHaveBeenDetermined(true);
      startNode.setCostFromStart(0.0, null);
      startNode.setEstimatedCostToGoal(startInWorld.distanceXY(goalInWorld));
      stack.add(startNode);
      expandedNodes = new HashSet<>();

      return true;
   }

   private List<Point3DReadOnly> planInternal()
   {
      long startBodyPathComputation = System.currentTimeMillis();
      long expandedNodesCount = 0;
      long iterations = 0;
      Point3DReadOnly goalInWorld = goalNode.getPointInWorld();

      while (!stack.isEmpty())
      {
         iterations++;

         VisibilityGraphNode nodeToExpand = stack.poll();
         if (expandedNodes.contains(nodeToExpand))
            continue;
         expandedNodes.add(nodeToExpand);

         if (checkAndHandleNodeAtGoal(nodeToExpand))
            break;

         List<VisibilityGraphEdge> neighboringEdges = expandNode(visibilityGraph, nodeToExpand);
         expandedNodesCount += neighboringEdges.size();

         for (VisibilityGraphEdge neighboringEdge : neighboringEdges)
         {
            VisibilityGraphNode neighbor = getNeighborNode(nodeToExpand, neighboringEdge);

            double connectionCost = computeEdgeCost(nodeToExpand, neighbor);
            double newCostFromStart = nodeToExpand.getCostFromStart() + connectionCost;

            double currentCostFromStart = neighbor.getCostFromStart();

            if (Double.isNaN(currentCostFromStart) || (newCostFromStart < currentCostFromStart))
            {
               neighbor.setCostFromStart(newCostFromStart, nodeToExpand);

               double heuristicCost = costParameters.getHeuristicWeight() * neighbor.getPointInWorld().distanceXY(goalInWorld);
               neighbor.setEstimatedCostToGoal(heuristicCost);

               stack.remove(neighbor);
               stack.add(neighbor);
            }
         }

         nodeToExpand.setHasBeenExpanded(true);
      }

      VisibilityMapSolution visibilityMapSolutionFromNewVisibilityGraph = visibilityGraph.createVisibilityMapSolution();

      visibilityMapSolution.setVisibilityMapsWithNavigableRegions(visibilityMapSolutionFromNewVisibilityGraph.getVisibilityMapsWithNavigableRegions());
      visibilityMapSolution.setInterRegionVisibilityMap(visibilityMapSolutionFromNewVisibilityGraph.getInterRegionVisibilityMap());

      visibilityMapSolution.setStartMap(visibilityMapSolutionFromNewVisibilityGraph.getStartMap());
      visibilityMapSolution.setGoalMap(visibilityMapSolutionFromNewVisibilityGraph.getGoalMap());

      List<Point3DReadOnly> path = new ArrayList<>();
      VisibilityGraphNode nodeWalkingBack = goalNode;

      while (nodeWalkingBack != null)
      {
         path.add(nodeWalkingBack.getPointInWorld());
         nodeWalkingBack = nodeWalkingBack.getBestParentNode();
      }
      Collections.reverse(path);

      printResults(startBodyPathComputation, expandedNodesCount, iterations, path);
      return path;
   }


   private List<VisibilityGraphEdge> expandNode(VisibilityGraph visibilityGraph, VisibilityGraphNode nodeToExpand)
   {
      if (nodeToExpand.getHasBeenExpanded())
      {
         throw new RuntimeException("Node has already been expanded!!");
      }

      if (!nodeToExpand.getEdgesHaveBeenDetermined())
      {
         visibilityGraph.computeInnerAndInterEdges(nodeToExpand);
      }

      return nodeToExpand.getEdges();
   }

   private double computeEdgeCost(VisibilityGraphNode nodeToExpandInWorld, VisibilityGraphNode nextNodeInWorld)
   {
      Point3DReadOnly originPointInWorld = nodeToExpandInWorld.getPointInWorld();
      Point3DReadOnly nextPointInWorld = nextNodeInWorld.getPointInWorld();
      Point2DReadOnly originPointInWorld2D = new Point2D(originPointInWorld);
      Point2DReadOnly nextPointInWorld2D = new Point2D(nextPointInWorld);;

      double horizontalDistance = originPointInWorld.distanceXY(nextPointInWorld);
      if (horizontalDistance <= 0.0)
         return 0.0;

      double verticalDistance = Math.abs(nextPointInWorld.getZ() - originPointInWorld.getZ());

      double distanceToCluster = Double.POSITIVE_INFINITY;
      Point2D closestPointToCluster = new Point2D();
      for (VisibilityGraphNavigableRegion navigableRegion : visibilityGraph.getVisibilityGraphNavigableRegions())
      {
         for (Cluster cluster : navigableRegion.getNavigableRegion().getObstacleClusters())
         {
            Point2D tempPoint = new Point2D();
            double distance = VisibilityTools.distanceToCluster(originPointInWorld2D, nextPointInWorld2D, cluster.getNonNavigableExtrusionsInWorld2D(),
                                                                tempPoint, cluster.isClosed());
            if (distance < distanceToCluster)
            {
               distanceToCluster = distance;
               closestPointToCluster.set(tempPoint);
            }
         }
      }

      double angle = Math.atan(verticalDistance / horizontalDistance);

      double distanceCost = costParameters.getDistanceWeight() * horizontalDistance;
      double elevationCost = costParameters.getElevationWeight() * 2.0 * angle / Math.PI;

      double distanceFromExtrusionForNoCost = bodyWidth / 2.0 - parameters.getObstacleExtrusionDistance();
      double rotationCost = 0.0;
      if (distanceToCluster < distanceFromExtrusionForNoCost)// && distanceToCluster > 0.0)
      {
         rotationCost = rotationWeight * (1.0 - distanceToCluster / distanceFromExtrusionForNoCost);
         if (!closestPointToCluster.geometricallyEquals(originPointInWorld2D, 1e-4) && !closestPointToCluster.geometricallyEquals(nextPointInWorld2D, 1e-4))
         {
            Point2D pointInLocal = new Point2D();
         }
      }

      return distanceCost + elevationCost + rotationCost;
   }

   private boolean checkIfStartAndGoalAreValid(Point3DReadOnly start, Point3DReadOnly goal)
   {
      boolean areStartAndGoalValid = true;
      if (start == null)
      {
         LogTools.error("Start is null!");
         areStartAndGoalValid = false;
      }

      if (goal == null)
      {
         LogTools.error("Goal is null!");
         areStartAndGoalValid = false;
      }

      if (debug)
         LogTools.info("Starting to calculate body path");

      return areStartAndGoalValid;
   }

   private boolean checkAndHandleNodeAtGoal(VisibilityGraphNode nodeToExpand)
   {
      return nodeToExpand == goalNode;
   }

   private VisibilityGraphNode getNeighborNode(VisibilityGraphNode nodeToExpand, VisibilityGraphEdge neighboringEdge)
   {
      VisibilityGraphNode nextNode = null;

      VisibilityGraphNode sourceNode = neighboringEdge.getSourceNode();
      VisibilityGraphNode targetNode = neighboringEdge.getTargetNode();

      if (nodeToExpand == sourceNode)
      {
         nextNode = targetNode;
      }
      else if (nodeToExpand == targetNode)
      {
         nextNode = sourceNode;
      }

      return nextNode;
   }

   private void printResults(long startBodyPathComputation, long expandedNodesCount, long iterationCount, List<Point3DReadOnly> path)
   {
      if (debug)
      {
         if (path != null)
         {
            LogTools.info("Total time to find solution was: " + (System.currentTimeMillis() - startBodyPathComputation) + "ms");
         }
         else
         {
            LogTools.info("NO BODY PATH SOLUTION WAS FOUND!" + (System.currentTimeMillis() - startBodyPathComputation) + "ms");
         }
         LogTools.info("Number of iterations: " + iterationCount);
         LogTools.info("Number of nodes expanded: " + expandedNodesCount);
      }
   }

   public List<Point3DReadOnly> calculateBodyPathWithOcclusions(Point3DReadOnly start, Point3DReadOnly goal)
   {
      List<Point3DReadOnly> path = calculateBodyPath(start, goal);

      if (path == null)
      {
         NavigableRegions navigableRegions = visibilityMapSolution.getNavigableRegions();
         ArrayList<VisibilityMapWithNavigableRegion> visibilityMapsWithNavigableRegions = createListOfVisibilityMapsWithNavigableRegions(navigableRegions);

         if (!OcclusionTools.isTheGoalIntersectingAnyObstacles(visibilityMapsWithNavigableRegions.get(0), start, goal))
         {
            if (debug)
            {
               LogTools.info("StraightLine available");
            }

            path = new ArrayList<>();
            path.add(start);
            path.add(goal);

            return path;
         }

         NavigableRegion regionContainingPoint = PlanarRegionTools.getNavigableRegionContainingThisPoint(start, navigableRegions);
         List<Cluster> intersectingClusters = OcclusionTools.getListOfIntersectingObstacles(regionContainingPoint.getObstacleClusters(), start, goal);
         Cluster closestCluster = ClusterTools.getTheClosestCluster(start, intersectingClusters);
         Point3D closestExtrusion = ClusterTools.getTheClosestVisibleExtrusionPoint(1.0, start, goal, closestCluster.getNavigableExtrusionsInWorld(),
                                                                                    regionContainingPoint.getHomePlanarRegion());

         path = calculateBodyPath(start, closestExtrusion);
         path.add(goal);

         return path;
      }
      else
      {
         return path;
      }
   }

   public VisibilityMapSolution getVisibilityMapSolution()
   {
      return visibilityMapSolution;
   }

   public VisibilityMapHolder getStartMap()
   {
      return visibilityMapSolution.getStartMap();
   }

   public VisibilityMapHolder getGoalMap()
   {
      return visibilityMapSolution.getGoalMap();
   }

   public InterRegionVisibilityMap getInterRegionConnections()
   {
      return visibilityMapSolution.getInterRegionVisibilityMap();
   }

}
