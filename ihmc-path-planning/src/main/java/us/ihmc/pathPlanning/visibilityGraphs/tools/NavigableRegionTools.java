package us.ihmc.pathPlanning.visibilityGraphs.tools;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.BoundingBox2D;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.NavigableRegions;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionTools;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class NavigableRegionTools
{
   public static NavigableRegion getNavigableRegionContainingThisPoint(Point3DReadOnly point, NavigableRegions navigableRegions, double ceilingHeight)
   {
      return getNavigableRegionContainingThisPoint(point, navigableRegions, ceilingHeight, 0.0);
   }

   public static NavigableRegion getNavigableRegionContainingThisPoint(Point3DReadOnly point, NavigableRegions navigableRegions, double ceilingHeight, double epsilon)
   {
      List<NavigableRegion> navigableRegionsList = navigableRegions.getNavigableRegionsList();
      if (navigableRegionsList == null)
         return null;

      List<NavigableRegion> containers = navigableRegionsList.stream().filter(navigableRegion -> {
         if (computeMinHeightOfNavigableRegionAbovePoint(navigableRegion, point) > ceilingHeight)
            return false;
         return isPointInWorldInsideNavigableRegion(navigableRegion, point, epsilon);
      }).collect(Collectors.toList());

      if (containers.isEmpty())
         return null;
      if (containers.size() == 1)
         return containers.get(0);

      Point3D pointOnRegion = new Point3D();

      NavigableRegion closestContainer = containers.get(0);
      Vector3D regionNormal = closestContainer.getHomePlanarRegion().getNormal();
      closestContainer.getHomePlanarRegion().getPointInRegion(pointOnRegion);
      double minDistance = EuclidGeometryTools.distanceFromPoint3DToPlane3D(point, pointOnRegion, regionNormal);

      for (int i = 1; i < containers.size(); i++)
      {
         NavigableRegion candidate = containers.get(i);
         candidate.getHomePlanarRegion().getNormal(regionNormal);
         candidate.getHomePlanarRegion().getPointInRegion(pointOnRegion);
         double distance = EuclidGeometryTools.distanceFromPoint3DToPlane3D(point, pointOnRegion, regionNormal);
         if (distance < minDistance)
         {
            closestContainer = candidate;
            minDistance = distance;
         }
      }


      return closestContainer;
   }

   private static double computeMinHeightOfNavigableRegionAbovePoint(NavigableRegion navigableRegion, Point3DReadOnly pointInWorldToCheck)
   {
      return navigableRegion.getHomePlanarRegion().getBoundingBox3dInWorld().getMinZ() - pointInWorldToCheck.getZ();
   }

   private static boolean isPointInWorldInsideNavigableRegion(NavigableRegion navigableRegion, Point3DReadOnly pointInWorldToCheck, double epsilon)
   {
      Point2D pointInLocalToCheck = new Point2D(pointInWorldToCheck);
      pointInLocalToCheck.applyTransform(navigableRegion.getTransformFromWorldToLocal(), false);
      return isPointInLocalInsideNavigableRegion(navigableRegion, pointInLocalToCheck, epsilon);
   }

   //TODO: Unit tests for all of this.
   private static boolean isPointInLocalInsideNavigableRegion(NavigableRegion navigableRegion, Point2DReadOnly pointInLocalToCheck, double epsilon)
   {
      PlanarRegion planarRegion = navigableRegion.getHomePlanarRegion();

      if (!PlanarRegionTools.isPointInLocalInsidePlanarRegion(planarRegion, pointInLocalToCheck, epsilon))
         return false;

      return navigableRegion.getObstacleClusters().stream().noneMatch(obstacleCluster -> obstacleCluster.isInsideNonNavigableZone(pointInLocalToCheck));
   }

   /**
    * Return true if the given point is contained inside the boundary.
    * https://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
    *
    * Also check https://en.wikipedia.org/wiki/Point_in_polygon.
    *
    * @param test The point to check
    * @return true if the point is inside the boundary, false otherwise
    *
    */
   public static boolean isPointInsideNavigableRegion(NavigableRegion navigableRegion, Point2DReadOnly test)
   {
      return isPointInsideClosedConcaveHullOfCluster(navigableRegion.getHomeRegionCluster(), test);
   }

   public static boolean isPointInsideClosedConcaveHullOfCluster(Cluster cluster, Point2DReadOnly test)
   {
      return PlanarRegionTools.isPointInsideConcaveHull(cluster.getNonNavigableExtrusionsInLocal().getPoints(), test);
   }
}
