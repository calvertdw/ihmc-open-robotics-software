package us.ihmc.pathPlanning.visibilityGraphs;

import org.junit.jupiter.api.*;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.commons.ContinuousIntegrationTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.pathPlanning.DataSet;
import us.ihmc.pathPlanning.bodyPathPlanner.WaypointDefinedBodyPathPlanner;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapWithNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.NavigableExtrusionDistanceCalculator;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.PlanarRegionFilter;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.DefaultVisibilityGraphParameters;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.postProcessing.ObstacleAndCliffAvoidanceProcessor;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityTools;
import us.ihmc.pathPlanning.visibilityGraphs.ui.messager.UIVisibilityGraphsTopics;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.graphics.Graphics3DObjectTools;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static us.ihmc.robotics.Assert.*;

public class NavigableRegionsManagerTest
{
   private static boolean visualize = true;
   private static final double epsilon = 1e-4;
   private static final long timeout = 30000 * 100;

   // For enabling helpful prints.
   private static boolean DEBUG = false;

   private static VisibilityGraphsTestVisualizerApplication visualizerApplication = null;
   // Because we use JavaFX, there will be two instance of VisibilityGraphsFrameworkTest, one for running the test and one starting the ui. The messager has to be static so both the ui and test use the same instance.
   private static JavaFXMessager messager = null;

   // The following are used for collision checks.
   private static final double walkerOffsetHeight = 0.75;
//   private static final Vector3D walkerRadii = new Vector3D(0.25, 0.25, 0.5);

   private static final double obstacleExtrusionDistance = 0.2;
   private static final double preferredObstacleExtrusionDistance = 1.0;
   private static final Vector3D walkerBox = new Vector3D(2.0 * obstacleExtrusionDistance, 2.0 * preferredObstacleExtrusionDistance, 1.0);


   @BeforeEach
   public void setup()
   {
      visualize = visualize && !ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer();
      DEBUG = (visualize || (DEBUG && !ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer()));

      if (visualize)
      {
         visualizerApplication = new VisibilityGraphsTestVisualizerApplication();
         visualizerApplication.startOnAThread();

         messager = visualizerApplication.getMessager();

         messager.submitMessage(UIVisibilityGraphsTopics.EnableWalkerAnimation, false);
         messager.submitMessage(UIVisibilityGraphsTopics.WalkerOffsetHeight, walkerOffsetHeight);
         messager.submitMessage(UIVisibilityGraphsTopics.WalkerSize, new Vector3D());
         messager.submitMessage(UIVisibilityGraphsTopics.WalkerBoxSize, walkerBox);
      }
   }

   @AfterEach
   public void tearDown() throws Exception
   {
      if (visualize)
      {
         visualizerApplication.stop();
         visualizerApplication = null;
         messager = null;
      }
   }

   @Test
   public void testFlatGroundWithWallInlineWithWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithWallOnOppositeSidesOfWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, 1.0, 0.0);
      Point3D goal = new Point3D(-5.0, 1.0, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithWallStraightShotButVeryNearWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());

   }

   @Test
   public void testFlatGroundWithWallStraightShotButNearWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithWallAlmostStraightShot()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithWallStraightShot()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithBoxInlineWithWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithBoxOnOppositeSidesOfWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, 1.0, 0.0);
      Point3D goal = new Point3D(-5.0, 1.0, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithBoxStraightShotButVeryNearWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithBoxStraightShotButNearWall()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithBoxAlmostStraightShot()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithBoxStraightShot()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundWithTwoDifferentWalls()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundTwoDifferentWidthWallsEnvironment());

      // test straight shot, initially going to one of the nodes
      Point3D start = new Point3D(-15.0, -0.5 * parameters.getObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.5 * parameters.getObstacleExtrusionDistance(), 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      /*
      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }
      */

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());

      start = new Point3D(-15.0, 1.0 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundBetweenWallOpening()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallOpeningEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 1.5, 0.0);
      Point3D goal = new Point3D(-5.0, 1.5, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundBetweenWallOpeningStraightShot()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallOpeningEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), postProcessor);
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());

   }

   @Test
   public void testFlatGroundBetweenAwkwardWallOpening()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallAwkwardOpeningEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 1.5, 0.0);
      Point3D goal = new Point3D(-5.0, 1.5, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), new ObstacleAndCliffAvoidanceProcessor(parameters));
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundBetweenBoxesOpening()
   {
      /*
      This test sets up an environment where the first, aggressive pass will hug the left of the opening. It is then further away from the other side than
      the preferred distance, so it may not queue that up as a distance it should consider.
       */
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxesEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 1.5, 0.0);
      Point3D goal = new Point3D(-5.0, 1.5, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), new ObstacleAndCliffAvoidanceProcessor(parameters));
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundBetweenBoxesOpeningStraightShot()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxesEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      ObstacleAndCliffAvoidanceProcessor postProcessor = new ObstacleAndCliffAvoidanceProcessor(parameters);
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), new ObstacleAndCliffAvoidanceProcessor(parameters));
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters,  planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   @Test
   public void testFlatGroundBetweenBoxInMiddle()
   {
      VisibilityGraphsParametersReadOnly parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxInMiddleEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), new ObstacleAndCliffAvoidanceProcessor(parameters));
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPath(start, goal);

      if (visualize)
      {
         visualize(path, parameters, planarRegionsList, start, goal, navigableRegionsManager.getNavigableRegionsList());
      }

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegionsManager.getNavigableRegionsList());
   }

   private static void checkPath(List<Point3DReadOnly> path, Point3DReadOnly start, Point3DReadOnly goal, VisibilityGraphsParametersReadOnly parameters,
                                 PlanarRegionsList planarRegionsList, List<VisibilityMapWithNavigableRegion> navigableRegionsList)
   {
      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList(), null);

      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());
      List<Point3DReadOnly> originalPath = navigableRegionsManager.calculateBodyPath(start, goal);

      int numberOfPoints = path.size();
      assertTrue(numberOfPoints >= originalPath.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(numberOfPoints - 1), epsilon);

      for (Point3DReadOnly point : path)
      {
         assertFalse(point.containsNaN());
      }

      WaypointDefinedBodyPathPlanner calculatedPath = new WaypointDefinedBodyPathPlanner();
      calculatedPath.setWaypoints(path);

      WaypointDefinedBodyPathPlanner expectedPathNoAvoidance = new WaypointDefinedBodyPathPlanner();
      expectedPathNoAvoidance.setWaypoints(originalPath);

      double distanceAlongExpectedPath = 0.0;

      for (double alpha = 0.05; alpha < 1.0; alpha += 0.001)
      {
         Pose2D expectedPose = new Pose2D();
         Pose2D actualPose = new Pose2D();

         expectedPathNoAvoidance.getPointAlongPath(alpha, expectedPose);
         calculatedPath.getPointAlongPath(alpha, actualPose);

         // assert it doesn't deviate too much
         EuclidCoreTestTools
               .assertPoint2DGeometricallyEquals("alpha = " + alpha, expectedPose.getPosition(), actualPose.getPosition(), preferredObstacleExtrusionDistance);

         if (visualize)
         {
            Point3DReadOnly position3D = PlanarRegionTools.projectPointToPlanesVertically(new Point3D(actualPose.getPosition()), planarRegionsList);
            Quaternion orientation = new Quaternion(actualPose.getYaw(), 0.0, 0.0);
            messager.submitMessage(UIVisibilityGraphsTopics.WalkerPosition, new Point3D(position3D));
            messager.submitMessage(UIVisibilityGraphsTopics.WalkerOrientation, orientation);

            ThreadTools.sleep(10);
         }

         // check that it's always moving along
         Point2D pointAlongExpectedPath = new Point2D();
         double newDistanceAlongExpectedPath = expectedPathNoAvoidance.getClosestPoint(pointAlongExpectedPath, actualPose);
         assertTrue(newDistanceAlongExpectedPath >= distanceAlongExpectedPath);
         distanceAlongExpectedPath = newDistanceAlongExpectedPath;

         // check that it doesn't get too close to an obstacle
         double distanceToObstacles = Double.MAX_VALUE;
         Point2D closestPointOverall = new Point2D();
         for (VisibilityMapWithNavigableRegion navigableRegion : navigableRegionsList)
         {
            for (Cluster obstacleCluster : navigableRegion.getObstacleClusters())
            {
               List<Point2DReadOnly> clusterPolygon = obstacleCluster.getNonNavigableExtrusionsInWorld2D();
               Point2D closestPointOnCluster = new Point2D();
               double distanceToCluster = VisibilityTools.distanceToCluster(actualPose.getPosition(), clusterPolygon, closestPointOnCluster, null);
               if (distanceToCluster < distanceToObstacles)
               {
                  distanceToObstacles = distanceToCluster;
                  closestPointOverall = closestPointOnCluster;
               }
            }
         }

         if (visualize && distanceToObstacles < 0.95 * preferredObstacleExtrusionDistance)
         {
            Point3DReadOnly collision = PlanarRegionTools.projectPointToPlanesVertically(new Point3D(closestPointOverall), planarRegionsList);
            List<Point3D> collisions = new ArrayList<>();
            collisions.add(new Point3D(collision));
            messager.submitMessage(UIVisibilityGraphsTopics.WalkerCollisionLocations, collisions);
         }
         assertTrue(distanceToObstacles > 0.95 * preferredObstacleExtrusionDistance);
      }
   }

   private static List<PlanarRegion> createFlatGroundWithWallEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D wallPointA = new Point2D(2.0, 0.0);
      Point2D wallPointB = new Point2D(0.0, 0.0);
      Point2D wallPointC = new Point2D(2.0, 5.0);
      Point2D wallPointD = new Point2D(0.0, 5.0);

      RigidBodyTransform wallTransform = new RigidBodyTransform();
      wallTransform.setTranslation(-10.0, 0.0, 0.0);
      wallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion wallRegion = new PlanarRegion(wallTransform,
                                                 new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(wallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithWallOpeningEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D wallPointA = new Point2D(2.0, 0.0);
      Point2D wallPointB = new Point2D(0.0, 0.0);
      Point2D wallPointC = new Point2D(2.0, 4.5);
      Point2D wallPointD = new Point2D(0.0, 4.5);

      RigidBodyTransform leftWallTransform = new RigidBodyTransform();
      leftWallTransform.setTranslation(-10.0, 0.5, 0.0);
      leftWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion leftWallRegion = new PlanarRegion(leftWallTransform,
                                                     new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      RigidBodyTransform rightWallTransform = new RigidBodyTransform();
      rightWallTransform.setTranslation(-10.0, -5.0, 0.0);
      rightWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion rightWallRegion = new PlanarRegion(rightWallTransform,
                                                      new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(leftWallRegion);
      planarRegions.add(rightWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithWallAwkwardOpeningEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D wallPointA = new Point2D(2.0, 0.0);
      Point2D wallPointB = new Point2D(0.0, 0.0);
      Point2D wallPointC = new Point2D(2.0, 4.25);
      Point2D wallPointD = new Point2D(0.0, 4.25);

      RigidBodyTransform leftWallTransform = new RigidBodyTransform();
      leftWallTransform.setTranslation(-10.0, 0.75, 0.0);
      leftWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion leftWallRegion = new PlanarRegion(leftWallTransform,
                                                     new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      RigidBodyTransform rightWallTransform = new RigidBodyTransform();
      rightWallTransform.setTranslation(-10.0, -5.0, 0.0);
      rightWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion rightWallRegion = new PlanarRegion(rightWallTransform,
                                                      new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(leftWallRegion);
      planarRegions.add(rightWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithBoxEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D frontWallPointA = new Point2D(2.0, 0.0);
      Point2D frontWallPointB = new Point2D(0.0, 0.0);
      Point2D frontWallPointC = new Point2D(2.0, 5.0);
      Point2D frontWallPointD = new Point2D(0.0, 5.0);

      RigidBodyTransform frontWallTransform = new RigidBodyTransform();
      frontWallTransform.setTranslation(-11.5, 0.0, 0.0);
      frontWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion frontWallRegion = new PlanarRegion(frontWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      RigidBodyTransform backWallTransform = new RigidBodyTransform();
      backWallTransform.setTranslation(-8.5, 0.0, 0.0);
      backWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion backWallRegion = new PlanarRegion(backWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      Point2D sideWallPointA = new Point2D(2.0, 0.0);
      Point2D sideWallPointB = new Point2D(0.0, 0.0);
      Point2D sideWallPointC = new Point2D(2.0, 3.0);
      Point2D sideWallPointD = new Point2D(0.0, 3.0);

      RigidBodyTransform sideWallTransform = new RigidBodyTransform();
      sideWallTransform.setTranslation(-11.5, 0.0, 0.0);
      sideWallTransform.setRotationYawPitchRoll(-Math.PI / 2.0, -Math.PI / 2.0, 0.0);
      PlanarRegion sideWallRegion = new PlanarRegion(sideWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(sideWallPointA, sideWallPointB, sideWallPointC, sideWallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(frontWallRegion);
      planarRegions.add(backWallRegion);
      planarRegions.add(sideWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithBoxInMiddleEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D frontWallPointA = new Point2D(2.0, 0.0);
      Point2D frontWallPointB = new Point2D(0.0, 0.0);
      Point2D frontWallPointC = new Point2D(2.0, 9.0);
      Point2D frontWallPointD = new Point2D(0.0, 9.0);

      RigidBodyTransform frontWallTransform = new RigidBodyTransform();
      frontWallTransform.setTranslation(-11.5, -4.5, 0.0);
      frontWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion frontWallRegion = new PlanarRegion(frontWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      RigidBodyTransform backWallTransform = new RigidBodyTransform();
      backWallTransform.setTranslation(-8.5, -4.5, 0.0);
      backWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion backWallRegion = new PlanarRegion(backWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      Point2D sideWallPointA = new Point2D(2.0, 0.0);
      Point2D sideWallPointB = new Point2D(0.0, 0.0);
      Point2D sideWallPointC = new Point2D(2.0, 3.0);
      Point2D sideWallPointD = new Point2D(0.0, 3.0);

      RigidBodyTransform leftSideWall = new RigidBodyTransform();
      leftSideWall.setTranslation(-11.5, 4.5, 0.0);
      leftSideWall.setRotationYawPitchRoll(-Math.PI / 2.0, -Math.PI / 2.0, 0.0);
      PlanarRegion leftSideWallRegion = new PlanarRegion(leftSideWall, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(sideWallPointA, sideWallPointB, sideWallPointC, sideWallPointD)));

      RigidBodyTransform rightSideWall = new RigidBodyTransform();
      rightSideWall.setTranslation(-11.5, -4.5, 0.0);
      rightSideWall.setRotationYawPitchRoll(-Math.PI / 2.0, -Math.PI / 2.0, 0.0);
      PlanarRegion rightSideWallRegion = new PlanarRegion(rightSideWall, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(sideWallPointA, sideWallPointB, sideWallPointC, sideWallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(frontWallRegion);
      planarRegions.add(backWallRegion);
      planarRegions.add(leftSideWallRegion);
      planarRegions.add(rightSideWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithBoxesEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D frontWallPointA = new Point2D(2.0, 0.0);
      Point2D frontWallPointB = new Point2D(0.0, 0.0);
      Point2D frontWallPointC = new Point2D(2.0, 4.5);
      Point2D frontWallPointD = new Point2D(0.0, 4.5);

      RigidBodyTransform frontLeftWallTransform = new RigidBodyTransform();
      frontLeftWallTransform.setTranslation(-11.5, 0.5, 0.0);
      frontLeftWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion frontLeftWallRegion = new PlanarRegion(frontLeftWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      RigidBodyTransform frontRightWallTransform = new RigidBodyTransform();
      frontRightWallTransform.setTranslation(-11.5, -5.0, 0.0);
      frontRightWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion frontRightWallRegion = new PlanarRegion(frontRightWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      // set up wall, 5x2
      Point2D backWallPointA = new Point2D(2.0, 0.0);
      Point2D backWallPointB = new Point2D(0.0, 0.0);
      Point2D backWallPointC = new Point2D(2.0, 4.25);
      Point2D backWallPointD = new Point2D(0.0, 4.25);

      RigidBodyTransform backLeftWallTransform = new RigidBodyTransform();
      backLeftWallTransform.setTranslation(-8.5, 0.75, 0.0);
      backLeftWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion backLeftWallRegion = new PlanarRegion(backLeftWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(backWallPointA, backWallPointB, backWallPointC, backWallPointD)));

      RigidBodyTransform backRightWallTransform = new RigidBodyTransform();
      backRightWallTransform.setTranslation(-8.5, -5.0, 0.0);
      backRightWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion backRightWallRegion = new PlanarRegion(backRightWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(backWallPointA, backWallPointB, backWallPointC, backWallPointD)));

      Point2D sideWallPointA = new Point2D(2.0, 0.0);
      Point2D sideWallPointB = new Point2D(0.0, 0.0);
      Point2D sideWallPointC = new Point2D(2.0, 3.0);
      Point2D sideWallPointD = new Point2D(0.0, 3.0);

      RigidBodyTransform leftSideWallTransform = new RigidBodyTransform();
      leftSideWallTransform.setTranslation(-11.5, 0.625, 0.0);
      leftSideWallTransform.setRotationYawPitchRoll(-Math.PI / 2.0, -Math.PI / 2.0, 0.0);
      PlanarRegion leftSideWallRegion = new PlanarRegion(leftSideWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(sideWallPointA, sideWallPointB, sideWallPointC, sideWallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(frontLeftWallRegion);
      planarRegions.add(backLeftWallRegion);
      planarRegions.add(frontRightWallRegion);
      planarRegions.add(backRightWallRegion);
      //      planarRegions.add(leftSideWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundTwoDifferentWidthWallsEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D wallPointA = new Point2D(2.0, 0.0);
      Point2D wallPointB = new Point2D(0.0, 0.0);
      Point2D wallPointC = new Point2D(2.0, 5.0);
      Point2D wallPointD = new Point2D(0.0, 5.0);

      RigidBodyTransform wallTransform = new RigidBodyTransform();
      wallTransform.setTranslation(-8.5, 0.0, 0.0);
      wallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion wallRegion = new PlanarRegion(wallTransform,
                                                 new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      Point2D otherWallPointA = new Point2D(2.0, 0.0);
      Point2D otherWallPointB = new Point2D(0.0, 0.0);
      Point2D otherWallPointC = new Point2D(2.0, 4.5);
      Point2D otherWallPointD = new Point2D(0.0, 4.5);

      RigidBodyTransform otherWallTransform = new RigidBodyTransform();
      otherWallTransform.setTranslation(-11.5, 0.5, 0.0);
      otherWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion otherWallRegion = new PlanarRegion(otherWallTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(otherWallPointA, otherWallPointB, otherWallPointC, otherWallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(wallRegion);
      planarRegions.add(otherWallRegion);

      return planarRegions;
   }

   private static void visualize(List<Point3DReadOnly> path, VisibilityGraphsParameters parameters, PlanarRegionsList planarRegionsList, Point3D start, Point3D goal, List<VisibilityMapWithNavigableRegion> navigableRegions)
   {
      Random random = new Random(324);
      planarRegionsList.getPlanarRegionsAsList().forEach(region -> region.setRegionId(random.nextInt()));

      visualizerApplication.submitPlanarRegionsListToVisualizer(planarRegionsList);
      visualizerApplication.submitGoalToVisualizer(goal);
      visualizerApplication.submitStartToVisualizer(start);
      visualizerApplication.submitNavigableRegionsToVisualizer(navigableRegions);
      messager.submitMessage(UIVisibilityGraphsTopics.BodyPathData, path);

      checkPath(path, start, goal, parameters, planarRegionsList, navigableRegions);


      ThreadTools.sleepForever();
   }

   private VisibilityGraphsParametersReadOnly createVisibilityGraphParametersForTest()
   {
      return new DefaultVisibilityGraphParameters()
      {
         @Override
         public PlanarRegionFilter getPlanarRegionFilter()
         {
            return new PlanarRegionFilter()
            {
               @Override
               public boolean isPlanarRegionRelevant(PlanarRegion region)
               {
                  return true;
               }
            };

         }

         @Override
         public double getObstacleExtrusionDistance()
         {
            return obstacleExtrusionDistance;
         }

         @Override
         public double getPreferredObstacleExtrusionDistance()
         {
            return preferredObstacleExtrusionDistance;
         }

         @Override
         public double getClusterResolution()
         {
            return 0.501;
         }

         @Override
         public NavigableExtrusionDistanceCalculator getNavigableExtrusionDistanceCalculator()
         {
            return new NavigableExtrusionDistanceCalculator()
            {
               @Override
               public double computeNavigableExtrusionDistance(PlanarRegion navigableRegionToBeExtruded)
               {
                  return 0.01;
               }
            };
         }
      };
   }

}
