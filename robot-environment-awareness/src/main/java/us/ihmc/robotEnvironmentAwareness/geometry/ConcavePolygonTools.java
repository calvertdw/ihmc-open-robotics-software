package us.ihmc.robotEnvironmentAwareness.geometry;

import us.ihmc.commons.FormattingTools;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Line2DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryPolygonTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Vector2DReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.EuclidCoreMissingTools;
import us.ihmc.robotics.geometry.ConvexPolygonCropResult;
import us.ihmc.robotics.geometry.ConvexPolygonTools;

import java.util.*;

public class ConcavePolygonTools
{

   public static List<ConcaveHull> cropPolygonToAboveLine(ConcaveHull concaveHullToCrop,
                                                          Line2DReadOnly cuttingLine,
                                                          Vector2DReadOnly upDirection)
   {
      ArrayList<ConcaveHull> resultingConcaveHulls = new ArrayList<>();
      if (concaveHullToCrop.getNumberOfVertices() < 5)
      {
         // must be convex, revert to convex polygon crop
         ConvexPolygon2D convexPolygonToCrop = new ConvexPolygon2D();
         for (Point2D concaveHullVertex : concaveHullToCrop.getConcaveHullVertices())
         {
            convexPolygonToCrop.addVertex(concaveHullVertex);
         }
         convexPolygonToCrop.update();
         ConvexPolygon2D croppedPolygonToPack = new ConvexPolygon2D();
         ConvexPolygonCropResult result = ConvexPolygonTools.cropPolygonToAboveLine(convexPolygonToCrop,
                                                                                    cuttingLine,
                                                                                    upDirection,
                                                                                    croppedPolygonToPack);
         if (result != ConvexPolygonCropResult.REMOVE_ALL)
         {
            ConcaveHull concaveHullToReturn = new ConcaveHull(croppedPolygonToPack.getVertexBufferView());
            resultingConcaveHulls.add(concaveHullToReturn);
         }
         return resultingConcaveHulls;
      }

      // assert vertices 5 or greater
      if (concaveHullToCrop.getNumberOfVertices() < 5)
         throw new RuntimeException("This polygon must be convex and shouldn't have gotten this far.");

      // find intersections (number of intersections can be as high as n-1)
      Map<Integer, Point2D> intersections = new HashMap<>(); // index after intersection; intersection point
      List<Point2D> concaveHullVertices = concaveHullToCrop.getConcaveHullVertices();
      for (int i = 0; i < concaveHullVertices.size(); i++)
      {
         int nextVertex = EuclidGeometryPolygonTools.next(i, concaveHullVertices.size());

         /**
          * Do a super low level intersection to make obvious all potential cases. This may not be necessary,
          * but until that's for sure it helps to see these potential corner cases laid out.
          */
         double lineSegmentDirectionX = concaveHullVertices.get(nextVertex).getX() - concaveHullVertices.get(i).getX();
         double lineSegmentDirectionY = concaveHullVertices.get(nextVertex).getY() - concaveHullVertices.get(i).getY();
         double percentage = EuclidCoreMissingTools.percentageOfIntersectionBetweenTwoLine2DsInfCase(concaveHullVertices.get(i).getX(),
                                                                                                     concaveHullVertices.get(i).getY(),
                                                                                                     lineSegmentDirectionX,
                                                                                                     lineSegmentDirectionY,
                                                                                                     cuttingLine.getPoint().getX(),
                                                                                                     cuttingLine.getPoint().getY(),
                                                                                                     cuttingLine.getDirection().getX(),
                                                                                                     cuttingLine.getDirection().getY());

         if (percentage == Double.NaN) // non-intersecting parallel
         {
            // do nothing
         }
         else if (percentage == Double.POSITIVE_INFINITY) // colinear edge intersection, store both vertices as intersections
         {

         }
         else // normal intersection
         {
            // check if contained by line segment

            if (percentage > 0.0 && percentage < 1.0)
            {
               Point2D intersection = new Point2D();
               intersection.interpolate(concaveHullVertices.get(i), concaveHullVertices.get(nextVertex), percentage);
               // TODO maybe discritize?
               intersection.setX(MathTools.roundToPrecision(intersection.getX(), 1e-6));
               intersection.setY(MathTools.roundToPrecision(intersection.getY(), 1e-6));
               intersections.put(nextVertex, intersection);
            }
         }
      }

      // filter out colinear edge intersections? did we just do this above?

      boolean vertex0IsAbove = EuclidGeometryTools.isPoint2DInFrontOfRay2D(concaveHullToCrop.getVertex(0), cuttingLine.getPoint(), upDirection);
      LogTools.debug("Intersection count: {} vertex(0) is above line: {}", intersections.size(), vertex0IsAbove);

      if (intersections.size() == 0) // first edge case: no intersections: keep all or remove all
      {
         if (vertex0IsAbove)
         {
            resultingConcaveHulls.add(new ConcaveHull(concaveHullToCrop));
            return resultingConcaveHulls;
         }
         else
         {
            return resultingConcaveHulls;
         }
      }
      else if (intersections.size() == 1) // second edge case: point intersection: keep all or remove all
      {
         // firstIntersectionToPack is packed with only intersection
         if (concaveHullToCrop.getNumberOfVertices() > 1)
         {
            // isPoint2DInFrontOfRay2D returns true for on as well. Check any two vertices. One is on the line.
            boolean isOnOrAboveTwo = EuclidGeometryTools.isPoint2DInFrontOfRay2D(concaveHullToCrop.getVertex(1), cuttingLine.getPoint(), upDirection);

            if (vertex0IsAbove && isOnOrAboveTwo)
            {
               resultingConcaveHulls.add(new ConcaveHull(concaveHullToCrop));
               return resultingConcaveHulls;
            }
            else
            {
               return resultingConcaveHulls;
            }
         }
         else
         {
            return resultingConcaveHulls;
         }
      }
      else
      {
         // TODO handle colinear intersections

         ConcaveHullCutModel model = new ConcaveHullCutModel(concaveHullVertices, cuttingLine, upDirection, intersections);

         // while all intersections not visited
         while (!model.allIntersectionsVisited())
         {
            // find intersection to start at; find first unvisited intersection
            int firstUnvisitedVertex = model.indexOfFirstUnvisitedIntersection();

            ConcaveHull currentResultingConcaveHull = new ConcaveHull();
            // always add the first point
            currentResultingConcaveHull.addVertex(new Point2D(model.getPoints().get(firstUnvisitedVertex).getPoint()));
            model.getPoints().get(firstUnvisitedVertex).setVisited(true);

            int travellingIndex = firstUnvisitedVertex;

            final int ALONG_HULL = 5;
            final int TRAVERSE_CUT_LINE = 6;

            int drawState = ALONG_HULL;

            // while it's not back to the start
            do
            {
               if (drawState == ALONG_HULL) // if we are along hull, is next point intersection?
               {
                  travellingIndex = model.getPoints().getNextIndex(travellingIndex);

                  boolean nextPointIsIntersection = model.getPoints().get(travellingIndex).isIntersection();

                  if (nextPointIsIntersection)
                  {
                     drawState = TRAVERSE_CUT_LINE;
                  }
               }
               else // if (drawState == TRAVERSE_CUT_LINE)
               {
                  travellingIndex = model.indexOfIntersectionToLeft(travellingIndex);

                  drawState = ALONG_HULL;
               }

               if (travellingIndex != firstUnvisitedVertex)
               {
                  currentResultingConcaveHull.addVertex(new Point2D(model.getPoints().get(travellingIndex).getPoint()));
                  model.getPoints().get(travellingIndex).setVisited(true);
               }
            }
            while (travellingIndex != firstUnvisitedVertex);

            // finish up current resulting concave hull
            resultingConcaveHulls.add(currentResultingConcaveHull);
         }

         // number of returned hulls may be as high as (n-1)/2
         return resultingConcaveHulls;
      }
   }
}
