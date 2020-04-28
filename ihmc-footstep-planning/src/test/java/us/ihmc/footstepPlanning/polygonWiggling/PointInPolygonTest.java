package us.ihmc.footstepPlanning.polygonWiggling;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import us.ihmc.commonWalkingControlModules.polygonWiggling.ConcavePolygonWiggler;
import us.ihmc.commonWalkingControlModules.polygonWiggling.PointInPolygonSolver;
import us.ihmc.commonWalkingControlModules.polygonWiggling.WiggleParameters;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactLineSegment2d;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactPosition;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFrameLineSegment2D;
import us.ihmc.yoVariables.variable.YoFramePoint2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static us.ihmc.footstepPlanning.polygonWiggling.ConcavePolygonWigglerTest.addLineSegments;
import static us.ihmc.footstepPlanning.polygonWiggling.PolygonWigglingTest.showPlotterAndSleep;

public class PointInPolygonTest
{
   private static final boolean visualize = true;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ArtifactList artifacts = new ArtifactList(getClass().getSimpleName());

   @Test
   @Disabled
   public void queryAndShowForRandomPolygon()
   {
      List<Point2D> vertexList = new ArrayList<>();
      vertexList.add(new Point2D(0.0, 0.0));
      vertexList.add(new Point2D(0.0, 0.6));
      vertexList.add(new Point2D(0.05, 0.65));
      vertexList.add(new Point2D(0.25, 0.2));
      vertexList.add(new Point2D(0.1, 0.65));
      vertexList.add(new Point2D(0.7, 0.75));
      vertexList.add(new Point2D(0.2, 0.6));
      vertexList.add(new Point2D(0.4, 0.6));
      vertexList.add(new Point2D(0.35, 0.5));
      vertexList.add(new Point2D(0.4, 0.4));
      vertexList.add(new Point2D(0.35, 0.3));
      vertexList.add(new Point2D(0.4, 0.2));
      vertexList.add(new Point2D(0.35, 0.1));
      vertexList.add(new Point2D(0.4, 0.0));
      Vertex2DSupplier polygon = Vertex2DSupplier.asVertex2DSupplier(vertexList);

      int numPoints = 10000;
      Random random = new Random();
      for (int i = 0; i < numPoints; i++)
      {
         Point2D queryPoint = new Point2D();
         queryPoint.setX(EuclidCoreRandomTools.nextDouble(random, -0.2, 0.8));
         queryPoint.setY(EuclidCoreRandomTools.nextDouble(random, -0.2, 0.8));
         boolean pointInsidePolygon = PointInPolygonSolver.isPointInsidePolygon(polygon, queryPoint);
         Color color = pointInsidePolygon ? Color.GREEN.darker() : Color.BLUE;
         addVertex("v" + i, queryPoint, color, artifacts, registry);
      }

      if (visualize)
      {
         addLineSegments("Plane", polygon, Color.BLACK, artifacts, registry);
         showPlotterAndSleep(artifacts);
      }
   }

   static void addVertex(String name, Point2D vertex, Color color, ArtifactList artifacts, YoVariableRegistry registry)
   {
      YoFramePoint2D framePoint2D = new YoFramePoint2D(name + "Point", worldFrame, registry);
      framePoint2D.set(vertex);
      artifacts.add(new YoArtifactPosition(name, framePoint2D, GraphicType.BALL, color, 0.002));
   }
}