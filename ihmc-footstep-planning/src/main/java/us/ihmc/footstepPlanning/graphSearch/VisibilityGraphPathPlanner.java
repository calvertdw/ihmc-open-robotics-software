package us.ihmc.footstepPlanning.graphSearch;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.pathPlanning.bodyPathPlanner.BodyPathPlannerTools;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphHolder;
import us.ihmc.pathPlanning.visibilityGraphs.NavigableRegionsManager;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapWithNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pathPlanning.visibilityGraphs.postProcessing.BodyPathPostProcessor;
import us.ihmc.pathPlanning.visibilityGraphs.postProcessing.PathOrientationCalculator;
import us.ihmc.robotics.geometry.PlanarRegionTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoEnum;

public class VisibilityGraphPathPlanner
{
   private static final boolean debug = false;

   private final NavigableRegionsManager navigableRegionsManager;
   private final PathOrientationCalculator pathOrientationCalculator;
   private final YoEnum<FootstepPlanningResult> yoResult;

   private final FootstepPlannerParametersReadOnly parameters;
   private final FramePose3D bodyStartPose = new FramePose3D();
   private final FramePose3D bodyGoalPose = new FramePose3D();

   private final List<Pose3DReadOnly> waypoints = new ArrayList<>();

   private PlanarRegionsList planarRegionsList;

   private final VisibilityGraphHolder visibilityGraphHolder = new VisibilityGraphHolder();

   public VisibilityGraphPathPlanner(FootstepPlannerParametersReadOnly footstepPlannerParameters,
                                     VisibilityGraphsParametersReadOnly visibilityGraphsParameters,
                                     BodyPathPostProcessor pathPostProcessor, YoVariableRegistry parentRegistry)
   {
      this("", footstepPlannerParameters, visibilityGraphsParameters, pathPostProcessor, parentRegistry);
   }

   public VisibilityGraphPathPlanner(String prefix, FootstepPlannerParametersReadOnly footstepPlannerParameters,
                                     VisibilityGraphsParametersReadOnly visibilityGraphsParameters,
                                     BodyPathPostProcessor postProcessor, YoVariableRegistry parentRegistry)
   {
      this.parameters = footstepPlannerParameters;

      yoResult = new YoEnum<>(prefix + "PathPlanningResult", parentRegistry, FootstepPlanningResult.class);
      this.navigableRegionsManager = new NavigableRegionsManager(visibilityGraphsParameters, null, postProcessor);
      this.pathOrientationCalculator = new PathOrientationCalculator(visibilityGraphsParameters);
   }

   public FootstepPlanningResult planWaypoints()
   {
      waypoints.clear();

      if (planarRegionsList == null)
      {
         waypoints.add(new Pose3D(bodyStartPose));
         waypoints.add(new Pose3D(bodyGoalPose));
      }
      else
      {
         Point3DReadOnly startPosition = PlanarRegionTools.projectPointToPlanesVertically(bodyStartPose.getPosition(), planarRegionsList);
         Point3DReadOnly goalPosition = PlanarRegionTools.projectPointToPlanesVertically(bodyGoalPose.getPosition(), planarRegionsList);

         if (startPosition == null)
            startPosition = new Point3D(bodyStartPose.getX(), bodyStartPose.getY(), 0.0);
         if (goalPosition == null)
            goalPosition = new Point3D(bodyGoalPose.getX(), bodyGoalPose.getY(), 0.0);

         navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

         if (debug)
         {
            LogTools.info("Starting to plan using " + getClass().getSimpleName());
            LogTools.info("Body start pose: " + startPosition);
            LogTools.info("Body goal pose:  " + goalPosition);
         }

         try
         {
            List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(startPosition, goalPosition);
            List<? extends Pose3DReadOnly> posePath = pathOrientationCalculator.computePosesFromPath(path, navigableRegionsManager.getVisibilityMapSolution(),
                                                                                                     bodyStartPose.getOrientation(), bodyGoalPose.getOrientation());

            waypoints.addAll(posePath);
         }
         catch (Exception e)
         {
            e.printStackTrace();
            yoResult.set(FootstepPlanningResult.PLANNER_FAILED);
            return yoResult.getEnumValue();
         }
      }

      yoResult.set(FootstepPlanningResult.SUB_OPTIMAL_SOLUTION);
      return yoResult.getEnumValue();
   }

   public VisibilityGraphHolder getVisibilityGraphHolder()
   {
      packGraph(visibilityGraphHolder);
      return visibilityGraphHolder;
   }

   public void setStanceFootPoses(Pose3DReadOnly leftFootPose, Pose3DReadOnly rightFootPose)
   {
      bodyStartPose.interpolate(leftFootPose, rightFootPose, 0.5);
   }

   public void setGoal(Pose3DReadOnly goalPose)
   {
      bodyGoalPose.set(goalPose);
   }

   public void setPlanarRegionsList(PlanarRegionsList planarRegionsList)
   {
      this.planarRegionsList = planarRegionsList;
   }

   public void computeBestEffortPlan(double horizonLength)
   {
      Vector2D goalDirection = new Vector2D(bodyGoalPose.getPosition());
      goalDirection.sub(bodyStartPose.getX(), bodyStartPose.getY());
      goalDirection.scale(horizonLength / goalDirection.length());
      Point3D waypointPosition = new Point3D(bodyStartPose.getPosition());
      waypointPosition.add(goalDirection.getX(), goalDirection.getY(), 0.0);
      Quaternion waypointOrientation = new Quaternion(BodyPathPlannerTools.calculateHeading(goalDirection), 0.0, 0.0);
      waypoints.add(new Pose3D(waypointPosition, waypointOrientation));
   }

   public List<Pose3DReadOnly> getWaypoints()
   {
      return waypoints;
   }

   public void packGraph(VisibilityGraphHolder visibilityGraphHolder)
   {
      visibilityGraphHolder.clear();

      VisibilityMapHolder startMap = navigableRegionsManager.getStartMap();
      VisibilityMapHolder goalMap = navigableRegionsManager.getGoalMap();
      VisibilityMapHolder interRegionsMap = navigableRegionsManager.getInterRegionConnections();
      List<VisibilityMapWithNavigableRegion> visibilityMapWithNavigableRegions = navigableRegionsManager.getNavigableRegionsList();

      if (startMap != null)
         visibilityGraphHolder.setStartVisibilityMapInWorld(startMap.getMapId(), startMap.getVisibilityMapInWorld());
      if (goalMap != null)
         visibilityGraphHolder.setGoalVisibilityMapInWorld(goalMap.getMapId(), goalMap.getVisibilityMapInWorld());
      if (interRegionsMap != null)
         visibilityGraphHolder.setInterRegionsVisibilityMapInWorld(interRegionsMap.getMapId(), interRegionsMap.getVisibilityMapInWorld());
      if (visibilityMapWithNavigableRegions != null)
         visibilityGraphHolder.addNavigableRegions(visibilityMapWithNavigableRegions);
   }
}
