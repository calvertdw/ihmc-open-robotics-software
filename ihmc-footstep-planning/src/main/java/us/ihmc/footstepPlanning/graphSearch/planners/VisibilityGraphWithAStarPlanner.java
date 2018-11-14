package us.ihmc.footstepPlanning.graphSearch.planners;

import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.pathPlanners.VisibilityGraphPathPlanner;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsParameters;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class VisibilityGraphWithAStarPlanner extends BodyPathAndFootstepPlannerWrapper
{
   public VisibilityGraphWithAStarPlanner(FootstepPlannerParameters parameters, VisibilityGraphsParameters visibilityGraphsParameters,
                                          SideDependentList<ConvexPolygon2D> footPolygons, YoGraphicsListRegistry graphicsListRegistry,
                                          YoVariableRegistry parentRegistry)
   {
      this("", parameters, visibilityGraphsParameters, footPolygons, graphicsListRegistry, parentRegistry);
   }

   public VisibilityGraphWithAStarPlanner(String prefix, FootstepPlannerParameters parameters, VisibilityGraphsParameters visibilityGraphsParameters,
                                          SideDependentList<ConvexPolygon2D> footPolygons, YoGraphicsListRegistry graphicsListRegistry,
                                          YoVariableRegistry parentRegistry)
   {
      super(prefix, parameters, parentRegistry, graphicsListRegistry);

      waypointPathPlanner = new VisibilityGraphPathPlanner(parameters, visibilityGraphsParameters, parentRegistry);
      footstepPlanner = new BodyPathBasedAStarPlanner(bodyPathPlanner, parameters, footPolygons, parameters.getCostParameters().getAStarHeuristicsWeight(),
                                                      registry);
   }
}
