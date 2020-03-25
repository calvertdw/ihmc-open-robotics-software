package us.ihmc.footstepPlanning;

import us.ihmc.commons.MathTools;
import us.ihmc.commons.time.Stopwatch;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapAndWiggler;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnappingTools;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.SimplePlanarRegionFootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.heuristics.CostToGoHeuristics;
import us.ihmc.footstepPlanning.graphSearch.heuristics.DistanceAndYawBasedHeuristics;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.FootstepNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.FootstepNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.IdealStepCalculator;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.ParameterBasedNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersBasics;
import us.ihmc.footstepPlanning.graphSearch.stepCost.FootstepCostCalculator;
import us.ihmc.footstepPlanning.log.FootstepPlannerEdgeData;
import us.ihmc.footstepPlanning.log.FootstepPlannerIterationData;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.humanoidRobotics.footstep.SimpleFootstep;
import us.ihmc.pathPlanning.bodyPathPlanner.WaypointDefinedBodyPathPlanHolder;
import us.ihmc.pathPlanning.graph.search.AStarIterationData;
import us.ihmc.pathPlanning.graph.search.AStarPathPlanner;
import us.ihmc.pathPlanning.graph.structure.GraphEdge;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class AStarFootstepPlanner
{
   private static final double defaultStatusPublishPeriod = 1.0;

   private final AStarPathPlanner<FootstepNode> footstepPlanner;
   private final FootstepPlannerParametersBasics footstepPlannerParameters;
   private final SimplePlanarRegionFootstepNodeSnapper snapper;
   private final FootstepNodeSnapAndWiggler snapAndWiggler;
   private final FootstepNodeChecker checker;
   private final CostToGoHeuristics distanceAndYawHeuristics;
   private final IdealStepCalculator idealStepCalculator;
   private final FootstepPlannerCompletionChecker completionChecker;
   private final WaypointDefinedBodyPathPlanHolder bodyPathPlanHolder;

   private final FootstepPlannerEdgeData edgeData = new FootstepPlannerEdgeData();
   private final HashMap<GraphEdge<FootstepNode>, FootstepPlannerEdgeData> edgeDataMap = new HashMap<>();
   private final List<FootstepPlannerIterationData> iterationData = new ArrayList<>();
   private double statusPublishPeriod = defaultStatusPublishPeriod;

   private final FramePose3D goalMidFootPose = new FramePose3D();
   private final AtomicBoolean haltRequested = new AtomicBoolean();

   private Consumer<FootstepPlannerOutput> statusCallback = result -> {};
   private Consumer<AStarIterationData<FootstepNode>> iterationCallback = iterationData -> {};

   private final Stopwatch stopwatch = new Stopwatch();
   private int iterations = 0;
   private FootstepPlanningResult result = null;

   public AStarFootstepPlanner(FootstepPlannerParametersBasics footstepPlannerParameters,
                               SideDependentList<ConvexPolygon2D> footPolygons,
                               WaypointDefinedBodyPathPlanHolder bodyPathPlanHolder)
   {
      this.footstepPlannerParameters = footstepPlannerParameters;
      this.bodyPathPlanHolder = bodyPathPlanHolder;
      this.snapper = new SimplePlanarRegionFootstepNodeSnapper(footPolygons);
      this.snapAndWiggler = new FootstepNodeSnapAndWiggler(footPolygons, footstepPlannerParameters);

      FootstepNodeExpansion expansion = new ParameterBasedNodeExpansion(footstepPlannerParameters);
      this.checker = new FootstepNodeChecker(footstepPlannerParameters, footPolygons, snapper, edgeData);
      this.idealStepCalculator = new IdealStepCalculator(footstepPlannerParameters, checker::isNodeValid, bodyPathPlanHolder);

      this.distanceAndYawHeuristics = new DistanceAndYawBasedHeuristics(snapper, footstepPlannerParameters.getAStarHeuristicsWeight(), footstepPlannerParameters, bodyPathPlanHolder, edgeData);
      FootstepCostCalculator stepCostCalculator = new FootstepCostCalculator(footstepPlannerParameters, snapper, idealStepCalculator::computeIdealStep, distanceAndYawHeuristics::compute, footPolygons, edgeData);

      this.footstepPlanner = new AStarPathPlanner<>(expansion::expandNode, checker::isNodeValid, stepCostCalculator::computeCost, distanceAndYawHeuristics::compute);
      checker.setParentNodeSupplier(node -> footstepPlanner.getGraph().getParentNode(node));
      footstepPlanner.getGraph().setGraphExpansionCallback(edge ->
                                                           {
                                                              edgeData.setCostFromStart(footstepPlanner.getGraph().getCostFromStart(edge.getEndNode()));
                                                              edgeDataMap.put(edge, edgeData.getCopyAndClear());
                                                           });
      this.completionChecker = new FootstepPlannerCompletionChecker(footstepPlannerParameters, footstepPlanner, distanceAndYawHeuristics);
   }

   public void handleRequest(FootstepPlannerRequest request, FootstepPlannerOutput outputToPack)
   {
      iterations = 0;
      stopwatch.start();

      // Reset logged variables
      edgeData.clear();
      edgeDataMap.clear();
      iterationData.clear();

      haltRequested.set(false);
      result = FootstepPlanningResult.SOLUTION_DOES_NOT_REACH_GOAL;
      outputToPack.setPlanId(request.getRequestId());

      // Update planar regions
      boolean flatGroundMode = request.getAssumeFlatGround() || request.getPlanarRegionsList() == null || request.getPlanarRegionsList().isEmpty();
      PlanarRegionsList planarRegionsList = flatGroundMode ? null : request.getPlanarRegionsList();

      snapper.setPlanarRegions(planarRegionsList);
      snapAndWiggler.setPlanarRegions(planarRegionsList);
      checker.setPlanarRegions(planarRegionsList);
      idealStepCalculator.setPlanarRegionsList(planarRegionsList);

      boolean horizonLengthImposed =
            request.getPlanBodyPath() && MathTools.intervalContains(bodyPathPlanHolder.computePathLength(0.0), 0.0, request.getHorizonLength());
      SideDependentList<FootstepNode> goalNodes;
      if (horizonLengthImposed)
      {
         bodyPathPlanHolder.getPointAlongPath(1.0, goalMidFootPose);
         SideDependentList<Pose3D> goalSteps = PlannerTools.createSquaredUpFootsteps(goalMidFootPose, footstepPlannerParameters.getIdealFootstepWidth());
         goalNodes = createGoalNodes(goalSteps::get);
      }
      else
      {
         goalNodes = createGoalNodes(request.getGoalFootPoses()::get);
      }

      // Setup footstep planner
      FootstepNode startNode = createStartNode(request);
      goalMidFootPose.interpolate(request.getGoalFootPoses().get(RobotSide.LEFT), request.getGoalFootPoses().get(RobotSide.RIGHT), 0.5);
      addFootPosesToSnapper(request);
      footstepPlanner.initialize(startNode);
      distanceAndYawHeuristics.setGoalPose(goalMidFootPose);
      idealStepCalculator.initialize(goalNodes);
      completionChecker.initialize(startNode, goalNodes, request.getGoalDistanceProximity(), request.getGoalYawProximity());

      // Check valid goal
      if (!snapAndCheckGoalNodes(goalNodes, horizonLengthImposed, request))
      {
         result = FootstepPlanningResult.INVALID_GOAL;
         reportStatus(request, outputToPack);
         return;
      }

      // Start planning loop
      while (true)
      {
         if (stopwatch.totalElapsed() >= request.getTimeout() || haltRequested.get())
         {
            result = FootstepPlanningResult.TIMED_OUT_BEFORE_SOLUTION;
            break;
         }
         if (request.getMaximumIterations() > 0 && iterations > request.getMaximumIterations())
         {
            result = FootstepPlanningResult.MAXIMUM_ITERATIONS_REACHED;
            break;
         }

         AStarIterationData<FootstepNode> iterationData = footstepPlanner.doPlanningIteration();
         recordIterationData(iterationData);
         iterationCallback.accept(iterationData);
         iterations++;

         if (iterationData.getParentNode() == null)
         {
            result = FootstepPlanningResult.NO_PATH_EXISTS;
            break;
         }
         if (completionChecker.checkIfGoalIsReached(iterationData))
         {
            // the final graph expansion is handled manually
            AStarIterationData<FootstepNode> finalIterationData = footstepPlanner.getIterationData();
            recordIterationData(finalIterationData);
            iterationCallback.accept(finalIterationData);

            result = FootstepPlanningResult.SUB_OPTIMAL_SOLUTION;
            break;
         }
         if (stopwatch.lapElapsed() > statusPublishPeriod && !MathTools.epsilonEquals(stopwatch.totalElapsed(), request.getTimeout(), 0.1))
         {
            reportStatus(request, outputToPack);
            stopwatch.lap();
         }
      }

      markSolutionEdges();
      reportStatus(request, outputToPack);
   }

   private void reportStatus(FootstepPlannerRequest request, FootstepPlannerOutput outputToPack)
   {
      outputToPack.setPlanId(request.getRequestId());
      outputToPack.setResult(result);

      // Pack solution path
      outputToPack.getFootstepPlan().clear();
      List<FootstepNode> path = footstepPlanner.getGraph().getPathFromStart(completionChecker.getEndNode());
      for (int i = 1; i < path.size(); i++)
      {
         SimpleFootstep footstep = new SimpleFootstep();

         footstep.setRobotSide(path.get(i).getRobotSide());

         RigidBodyTransform footstepPose = new RigidBodyTransform();
         footstepPose.setRotationYawAndZeroTranslation(path.get(i).getYaw());
         footstepPose.setTranslationX(path.get(i).getX());
         footstepPose.setTranslationY(path.get(i).getY());

         FootstepNodeSnapData snapData;
         if (footstepPlannerParameters.getMaximumXYWiggleDistance() > 0.0 || footstepPlannerParameters.getMaximumYawWiggle() > 0.0)
         {
            snapData = snapAndWiggler.snapFootstepNode(path.get(i));
         }
         else
         {
            snapData = snapper.snapFootstepNode(path.get(i));
         }

         RigidBodyTransform snapTransform = snapData.getSnapTransform();
         snapTransform.transform(footstepPose);
         footstep.getSoleFramePose().set(footstepPose);

         if (request.getAssumeFlatGround() || request.getPlanarRegionsList() == null || request.getPlanarRegionsList().isEmpty())
         {
            double flatGroundHeight = 0.5 * (request.getStartFootPoses().get(RobotSide.LEFT).getZ() + request.getStartFootPoses().get(RobotSide.RIGHT).getZ());
            footstep.getSoleFramePose().setZ(flatGroundHeight);
         }

         footstep.setFoothold(snapData.getCroppedFoothold());
         outputToPack.getFootstepPlan().addFootstep(footstep);
      }

      BodyPathPlan bodyPathPlan = bodyPathPlanHolder.getPlan();
      if (bodyPathPlan.getNumberOfWaypoints() > 0)
      {
         outputToPack.getBodyPath().clear();
         for (int i = 0; i < bodyPathPlan.getNumberOfWaypoints(); i++)
         {
            outputToPack.getBodyPath().add(new Pose3D(bodyPathPlan.getWaypoint(i)));
         }

         outputToPack.getLowLevelGoal().set(goalMidFootPose);
      }

      outputToPack.setPlanarRegionsList(request.getPlanarRegionsList());
      statusCallback.accept(outputToPack);
   }

   private void markSolutionEdges()
   {
      List<FootstepNode> path = footstepPlanner.getGraph().getPathFromStart(completionChecker.getEndNode());
      for (int i = 1; i < path.size(); i++)
      {
         edgeDataMap.get(new GraphEdge<>(path.get(i - 1), path.get(i))).setSolutionEdge(true);
      }
   }

   private void recordIterationData(AStarIterationData<FootstepNode> iterationData)
   {
      if (iterationData.getParentNode() == null)
      {
         return;
      }

      FootstepPlannerIterationData loggedData = new FootstepPlannerIterationData();
      loggedData.setStanceNode(iterationData.getParentNode());
      iterationData.getValidChildNodes().forEach(loggedData::addChildNode);
      iterationData.getInvalidChildNodes().forEach(loggedData::addChildNode);
      loggedData.setIdealStep(idealStepCalculator.computeIdealStep(iterationData.getParentNode()));
      loggedData.setStanceNodeSnapData(snapper.getSnapData(iterationData.getParentNode()));
      this.iterationData.add(loggedData);
   }

   public void setStatusCallback(Consumer<FootstepPlannerOutput> statusCallback)
   {
      this.statusCallback = statusCallback;
   }

   public void addIterationCallback(Consumer<AStarIterationData<FootstepNode>> callback)
   {
      iterationCallback = iterationCallback.andThen(callback);
   }

   private boolean snapAndCheckGoalNodes(SideDependentList<FootstepNode> goalNodes, boolean horizonLengthImposed, FootstepPlannerRequest request)
   {
      FootstepNodeSnapData leftGoalStepSnapData, rightGoalStepSnapData;
      boolean snapGoalSteps = horizonLengthImposed || request.getSnapGoalSteps();

      if (snapGoalSteps)
      {
         leftGoalStepSnapData = snapper.snapFootstepNode(goalNodes.get(RobotSide.LEFT));
         rightGoalStepSnapData = snapper.snapFootstepNode(goalNodes.get(RobotSide.RIGHT));
      }
      else
      {
         addSnapData(request.getGoalFootPoses().get(RobotSide.LEFT), RobotSide.LEFT);
         addSnapData(request.getGoalFootPoses().get(RobotSide.RIGHT), RobotSide.RIGHT);
         return true;
      }

      if (request.getAbortIfGoalStepSnappingFails())
      {
         return !leftGoalStepSnapData.getSnapTransform().containsNaN() && !rightGoalStepSnapData.getSnapTransform().containsNaN();
      }
      else
      {
         return true;
      }
   }

   public void setStatusPublishPeriod(double statusPublishPeriod)
   {
      this.statusPublishPeriod = statusPublishPeriod;
   }

   public void halt()
   {
      haltRequested.set(true);
   }

   private void addFootPosesToSnapper(FootstepPlannerRequest request)
   {
      addSnapData(request.getStartFootPoses().get(RobotSide.LEFT), RobotSide.LEFT);
      addSnapData(request.getStartFootPoses().get(RobotSide.RIGHT), RobotSide.RIGHT);
   }

   private void addSnapData(Pose3D footstepPose, RobotSide side)
   {
      FootstepNode footstepNode = new FootstepNode(footstepPose.getX(), footstepPose.getY(), footstepPose.getYaw(), side);
      FootstepNodeSnapData snapData = new FootstepNodeSnapData(FootstepNodeSnappingTools.computeSnapTransform(footstepNode, footstepPose));
      snapper.addSnapData(footstepNode, snapData);
      snapAndWiggler.addSnapData(footstepNode, snapData);
   }

   private static FootstepNode createStartNode(FootstepPlannerRequest request)
   {
      RobotSide robotSide = request.getRequestedInitialStanceSide();
      Pose3D startFootPose = request.getStartFootPoses().get(robotSide);
      return new FootstepNode(startFootPose.getX(), startFootPose.getY(), startFootPose.getYaw(), robotSide);
   }

   private static SideDependentList<FootstepNode> createGoalNodes(Function<RobotSide, Pose3D> poses)
   {
      return new SideDependentList<>(side ->
                                     {
                                        Pose3DReadOnly goalFootPose = poses.apply(side);
                                        return new FootstepNode(goalFootPose.getX(), goalFootPose.getY(), goalFootPose.getYaw(), side);
                                     });
   }

   public SimplePlanarRegionFootstepNodeSnapper getSnapper()
   {
      return snapper;
   }

   public FootstepNodeChecker getChecker()
   {
      return checker;
   }

   public AStarPathPlanner<FootstepNode> getLowLevelStepPlanner()
   {
      return footstepPlanner;
   }

   public FootstepNode getEndNode()
   {
      return completionChecker.getEndNode();
   }

   public HashMap<GraphEdge<FootstepNode>, FootstepPlannerEdgeData> getEdgeDataMap()
   {
      return edgeDataMap;
   }

   public List<FootstepPlannerIterationData> getIterationData()
   {
      return iterationData;
   }
}
