package us.ihmc.pathPlanning.visibilityGraphs.postProcessing;

import us.ihmc.commons.InterpolationTools;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple2D.interfaces.Vector2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.pathPlanning.bodyPathPlanner.BodyPathPlanner;
import us.ihmc.pathPlanning.bodyPathPlanner.BodyPathPlannerTools;
import us.ihmc.pathPlanning.visibilityGraphs.NavigableRegions;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphNode;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapSolution;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityTools;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.geometry.PlanarRegion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ObstacleAndCliffAvoidanceProcessor
{
   private static final boolean includeMidpoints = true;
   private static final boolean adjustWaypoints = true;
   private static final boolean adjustMidpoints = true;

   private static final double minDistanceToMove = 0.01;
   private static final double cliffHeightToAvoid = 0.10;
   private static final double samePointEpsilon = 0.01;

   private final double desiredDistanceFromObstacleCluster;
   private final double minimumDistanceFromObstacleCluster;
   private final double desiredDistanceFromCliff;
   private final double minimumDistanceFromCliff; // FIXME this is currently unused
   private final double maxInterRegionConnectionLength;
   private final double waypointResolution;
   private final IntermediateComparator comparator = new IntermediateComparator();

   private final VisibilityGraphsParametersReadOnly parameters;

   public ObstacleAndCliffAvoidanceProcessor(VisibilityGraphsParametersReadOnly parameters)
   {
      this.parameters = parameters;

      desiredDistanceFromObstacleCluster = parameters.getPreferredObstacleExtrusionDistance() - parameters.getObstacleExtrusionDistance();
      maxInterRegionConnectionLength = parameters.getMaxInterRegionConnectionLength();
      desiredDistanceFromCliff = parameters.getPreferredObstacleExtrusionDistance() - parameters.getNavigableExtrusionDistance();
      //      desiredDistanceFromCliff = 0.5;
      minimumDistanceFromCliff = parameters.getObstacleExtrusionDistance();
      minimumDistanceFromObstacleCluster = 0.0;
      waypointResolution = 0.1;
   }

   public List<Pose3DReadOnly> computePathFromNodes(List<VisibilityGraphNode> nodePath, VisibilityMapSolution visibilityMapSolution)
   {
      List<Point3D> newPathPositions = nodePath.parallelStream().map(node -> new Point3D(node.getPointInWorld())).collect(Collectors.toList());

      int pathNodeIndex = 0;
      int waypointIndex = 0;
      // don't do the goal node
      while (pathNodeIndex < nodePath.size() - 1)
      {
         int nextPathNodeIndex = pathNodeIndex + 1;
         int nextWaypointIndex = waypointIndex + 1;

         Point3D startPointInWorld = newPathPositions.get(waypointIndex);
         Point3D endPointInWorld = newPathPositions.get(nextWaypointIndex);

         VisibilityGraphNode startVisGraphNode = nodePath.get(pathNodeIndex);
         VisibilityGraphNode endVisGraphNode = nodePath.get(nextPathNodeIndex);

         boolean isGoalNode = pathNodeIndex > nodePath.size() - 2;

         NavigableRegion startingRegion = startVisGraphNode.getVisibilityGraphNavigableRegion().getNavigableRegion();
         NavigableRegion endingRegion = endVisGraphNode.getVisibilityGraphNavigableRegion().getNavigableRegion();
         NavigableRegions allNavigableRegions = visibilityMapSolution.getNavigableRegions();

         if (!isGoalNode && adjustWaypoints)
         {
            adjustGoalNodePositionToAvoidObstaclesAndCliffs(endPointInWorld, startingRegion, endingRegion, allNavigableRegions);
         }

         if (includeMidpoints)
         {
            List<Point3D> intermediateWaypointsToAdd = computeIntermediateWaypointsToAddToAvoidObstacles(new Point2D(startPointInWorld),
                                                                                                         new Point2D(endPointInWorld), startVisGraphNode,
                                                                                                         endVisGraphNode);
            removeDuplicated3DPointsFromList(intermediateWaypointsToAdd, waypointResolution);
            removeDuplicateStartOrEndPointsFromList(intermediateWaypointsToAdd, startPointInWorld, endPointInWorld, waypointResolution);

            // shift all the points around
            if (adjustMidpoints)// && pathNodeIndex < 3 )
            {
               for (Point3D intermediateWaypointToAdd : intermediateWaypointsToAdd)
               {
                  adjustGoalNodePositionToAvoidObstaclesAndCliffs(intermediateWaypointToAdd, startingRegion, endingRegion, allNavigableRegions);
               }
            }

            // prune duplicated points
            removeDuplicated3DPointsFromList(intermediateWaypointsToAdd, waypointResolution);
            removeDuplicateStartOrEndPointsFromList(intermediateWaypointsToAdd, startPointInWorld, endPointInWorld, waypointResolution);

            for (Point3D intermediateWaypointToAdd : intermediateWaypointsToAdd)
            {
               waypointIndex++;
               newPathPositions.add(waypointIndex, intermediateWaypointToAdd);
            }
         }

         waypointIndex++;
         pathNodeIndex++;
      }


      List<Pose3D> newPathPoses = new ArrayList<>();
      List<Cluster> allObstacleClusters = new ArrayList<>();
      visibilityMapSolution.getNavigableRegions().getNaviableRegionsList().forEach(region -> allObstacleClusters.addAll(region.getObstacleClusters()));

      int size = newPathPositions.size();
      double startHeading = BodyPathPlannerTools.calculateHeading(newPathPositions.get(0), newPathPositions.get(1));

      newPathPoses.add(new Pose3D(newPathPositions.get(0), new Quaternion(startHeading, 0.0, 0.0)));
      for (int i = 1; i < newPathPositions.size() - 1; i++)
      {
         Point3D previousPosition = newPathPositions.get(i - 1);
         Point3D currentPosition = newPathPositions.get(i);
         Point3D nextPosition = newPathPositions.get(i + 1);

         Point2D currentPosition2D = new Point2D(currentPosition);

         double previousHeading = BodyPathPlannerTools.calculateHeading(previousPosition, currentPosition);
         double nextHeading = BodyPathPlannerTools.calculateHeading(currentPosition, nextPosition);
         double desiredOrientation = InterpolationTools.linearInterpolate(previousHeading, nextHeading, 0.5);

         Point2D closestObstaclePoint = new Point2D();
         double distanceToClosestPoint = Double.POSITIVE_INFINITY;
         for (Cluster cluster : allObstacleClusters)
         {
            Point2D closestPointInCluster = new Point2D();
            double distance = VisibilityTools.distanceToCluster(currentPosition2D, cluster.getNonNavigableExtrusionsInLocal(), closestPointInCluster, null);
            if (distance < distanceToClosestPoint)
            {
               distanceToClosestPoint = distance;
               closestObstaclePoint = closestPointInCluster;
            }
         }

         Vector2D vectorToObstacle = new Vector2D();
         vectorToObstacle.sub(closestObstaclePoint, currentPosition2D);

         desiredOrientation = getHeadingToAvoidObstacles(desiredOrientation, vectorToObstacle);
         newPathPoses.add(new Pose3D(newPathPositions.get(i), new Quaternion(desiredOrientation, 0.0, 0.0)));
      }

      double endHeading = BodyPathPlannerTools.calculateHeading(newPathPositions.get(size - 2), newPathPositions.get(size - 1));

      newPathPoses.add(new Pose3D(newPathPositions.get(size - 1), new Quaternion(endHeading, 0.0, 0.0)));


      return newPathPoses.parallelStream().map(Pose3D::new).collect(Collectors.toList());
   }

   private void adjustGoalNodePositionToAvoidObstaclesAndCliffs(Point3D nodeLocationToPack, NavigableRegion startRegion, NavigableRegion endRegion,
                                                                NavigableRegions allNavigableRegions)
   {
      Point2D nextPointInWorld2D = new Point2D(nodeLocationToPack);

      List<Cluster> obstacleClusters = new ArrayList<>(startRegion.getObstacleClusters());
      if (!startRegion.equals(endRegion))
         obstacleClusters.addAll(endRegion.getObstacleClusters());

      List<Point2DReadOnly> closestObstacleClusterPoints = getClosestPointsOnClusters(nextPointInWorld2D, obstacleClusters);
      Vector2DReadOnly nodeShiftToAvoidObstacles = PointWiggler.computeBestShiftVectorToAvoidPoints(nextPointInWorld2D, closestObstacleClusterPoints,
                                                                                                    desiredDistanceFromObstacleCluster, minimumDistanceFromObstacleCluster);

      Point2D shiftedPoint = new Point2D(nodeLocationToPack);
      shiftedPoint.add(nodeShiftToAvoidObstacles);

      List<NavigableRegion> bothRegions = new ArrayList<>();
      bothRegions.add(startRegion);
      bothRegions.add(endRegion);

      // FIXME should be both regions
      boolean isShiftedPointNearACliff = isNearCliff(shiftedPoint, maxInterRegionConnectionLength, cliffHeightToAvoid, endRegion,
                                                     allNavigableRegions.getNaviableRegionsList());

      Vector2D nodeShift = new Vector2D();

      if (isShiftedPointNearACliff)
      {
         List<Cluster> homeRegionClusters = new ArrayList<>();
         homeRegionClusters.add(startRegion.getHomeRegionCluster());
         if (!startRegion.equals(endRegion))
            homeRegionClusters.add(endRegion.getHomeRegionCluster());

         List<Point2DReadOnly> closestCliffObstacleClusterPoints = getClosestPointsOnClusters(nextPointInWorld2D, homeRegionClusters);
         nodeShift.set(PointWiggler.computeBestShiftVectorToAvoidPoints(nextPointInWorld2D, closestObstacleClusterPoints, closestCliffObstacleClusterPoints,
                                                                        desiredDistanceFromObstacleCluster, desiredDistanceFromCliff, minimumDistanceFromObstacleCluster, minimumDistanceFromCliff));
      }
      else
      {
         nodeShift.set(nodeShiftToAvoidObstacles);
      }

      if (nodeShift.length() > minDistanceToMove)
      {
         nextPointInWorld2D.add(nodeShift);
         nodeLocationToPack.set(nextPointInWorld2D, findHeightOfPoint(nextPointInWorld2D, bothRegions));
      }
   }

   private List<Point3D> computeIntermediateWaypointsToAddToAvoidObstacles(Point2DReadOnly originPointInWorld, Point2DReadOnly nextPointInWorld,
                                                                           VisibilityGraphNode connectionStartNode, VisibilityGraphNode connectionEndNode)
   {
      List<NavigableRegion> navigableRegionsToSearch = new ArrayList<>();
      NavigableRegion startRegion = connectionStartNode.getVisibilityGraphNavigableRegion().getNavigableRegion();
      NavigableRegion endRegion = connectionEndNode.getVisibilityGraphNavigableRegion().getNavigableRegion();
      navigableRegionsToSearch.add(startRegion);
      if (!startRegion.equals(endRegion))
         navigableRegionsToSearch.add(endRegion);

      return computeIntermediateWaypointsToAddToAvoidObstacles(originPointInWorld, nextPointInWorld, navigableRegionsToSearch);
   }

   private List<Point3D> computeIntermediateWaypointsToAddToAvoidObstacles(Point2DReadOnly originPointInWorld2D, Point2DReadOnly nextPointInWorld2D,
                                                                           List<NavigableRegion> navigableRegionsToSearch)
   {
      List<Point2D> intermediateWaypointsToAdd = new ArrayList<>();

      for (NavigableRegion navigableRegion : navigableRegionsToSearch)
      {
         for (Cluster cluster : navigableRegion.getObstacleClusters())
         {
            List<Point2DReadOnly> clusterPolygon = cluster.getNonNavigableExtrusionsInWorld2D();
            boolean isClosed = cluster.isClosed();

            Point2D closestPointInCluster = new Point2D();
            Point2D closestPointOnConnection = new Point2D();

            double connectionDistanceToObstacle = VisibilityTools
                  .distanceToCluster(originPointInWorld2D, nextPointInWorld2D, clusterPolygon, closestPointOnConnection, closestPointInCluster, null, isClosed);

            // only add the point if it's close to the obstacle cluster, and don't add if it's already been added.
            if (connectionDistanceToObstacle < desiredDistanceFromObstacleCluster && !intermediateWaypointsToAdd.contains(closestPointOnConnection))
               intermediateWaypointsToAdd.add(closestPointOnConnection);
         }
      }

      // sort the points by their percentage along the line segment to make sure they get added in order.
      comparator.setStartPoint(originPointInWorld2D);
      comparator.setEndPoint(nextPointInWorld2D);
      intermediateWaypointsToAdd.sort(comparator);

      // collapse intermediate waypoints
      int intermediateWaypointIndex = 0;
      while (intermediateWaypointIndex < intermediateWaypointsToAdd.size() - 1)
      {
         Point2D thisWaypoint = intermediateWaypointsToAdd.get(intermediateWaypointIndex);
         Point2D nextWaypoint = intermediateWaypointsToAdd.get(intermediateWaypointIndex + 1);
         if (thisWaypoint.distance(nextWaypoint) < waypointResolution)
         { // collapse with the next one
            thisWaypoint.interpolate(thisWaypoint, nextWaypoint, 0.5);
            intermediateWaypointsToAdd.remove(intermediateWaypointIndex + 1);
         }
         else
         {
            intermediateWaypointIndex++;
         }
      }

      List<Point3D> intermediateWaypoints3DToAdd = new ArrayList<>();
      for (Point2D intermediateWaypoint : intermediateWaypointsToAdd)
      {
         double heightOfPoint = findHeightOfPoint(intermediateWaypoint, navigableRegionsToSearch);
         Point3D pointIn3D = new Point3D(intermediateWaypoint.getX(), intermediateWaypoint.getY(), heightOfPoint);
         intermediateWaypoints3DToAdd.add(pointIn3D);
      }

      return intermediateWaypoints3DToAdd;
   }

   private static List<Point2DReadOnly> getClosestPointsOnClusters(Point2DReadOnly pointInWorld, List<Cluster> clustersInWorld)
   {
      List<Point2DReadOnly> closestClusterPoints = new ArrayList<>();

      for (Cluster cluster : clustersInWorld)
      {
         List<Point2DReadOnly> clusterPolygon = cluster.getNonNavigableExtrusionsInWorld2D();

         Point2D closestPointInCluster = new Point2D();
         VisibilityTools.distanceToCluster(pointInWorld, clusterPolygon, closestPointInCluster, null);
         closestClusterPoints.add(closestPointInCluster);
      }

      removeDuplicated2DPointsFromList(closestClusterPoints, samePointEpsilon);

      return closestClusterPoints;
   }


   static void removeDuplicated2DPointsFromList(List<? extends Point2DReadOnly> listOfPoints, double samePointEpsilon)
   {
      int pointIndex = 0;
      while (pointIndex < listOfPoints.size() - 1)
      {
         int otherPointIndex = pointIndex + 1;
         while (otherPointIndex < listOfPoints.size())
         {
            if (listOfPoints.get(pointIndex).distance(listOfPoints.get(otherPointIndex)) < samePointEpsilon)
               listOfPoints.remove(otherPointIndex);
            else
               otherPointIndex++;
         }
         pointIndex++;
      }
   }

   static void removeDuplicated3DPointsFromList(List<? extends Point3DReadOnly> listOfPoints, double samePointEpsilon)
   {
      int pointIndex = 0;
      while (pointIndex < listOfPoints.size() - 1)
      {
         int otherPointIndex = pointIndex + 1;
         while (otherPointIndex < listOfPoints.size())
         {
            if (listOfPoints.get(pointIndex).distance(listOfPoints.get(otherPointIndex)) < samePointEpsilon)
               listOfPoints.remove(otherPointIndex);
            else
               otherPointIndex++;
         }
         pointIndex++;
      }
   }

   static void removeDuplicateStartOrEndPointsFromList(List<? extends Point3DReadOnly> listOfPoints, Point3DReadOnly startPoint, Point3DReadOnly endPoint,
                                                       double samePointEpsilon)
   {
      int pointIndex = 0;
      while (pointIndex < listOfPoints.size())
      {
         Point3DReadOnly pointToCheck = listOfPoints.get(pointIndex);
         if (pointToCheck.distance(startPoint) < samePointEpsilon || pointToCheck.distance(endPoint) < samePointEpsilon)
            listOfPoints.remove(pointIndex);
         else
            pointIndex++;
      }
   }

   private double getHeadingToAvoidObstacles(double nominalHeading, Vector2DReadOnly headingToClosestObstacle)
   {
      double headingToObstacle = BodyPathPlannerTools.calculateHeading(headingToClosestObstacle);
      double distanceToObstacle = headingToClosestObstacle.length();

      if (distanceToObstacle > parameters.getPreferredObstacleExtrusionDistance())
         return nominalHeading;

      double rotationForAvoidance;
      if (distanceToObstacle < parameters.getObstacleExtrusionDistance())
         rotationForAvoidance = Math.PI / 2.0;
      else
         rotationForAvoidance = Math.asin(parameters.getObstacleExtrusionDistance() / distanceToObstacle);

      double obstaclePlusRotation = headingToObstacle + rotationForAvoidance;
      double obstacleMinusRotation = headingToObstacle - rotationForAvoidance;

      if (AngleTools.computeAngleDifferenceMinusPiToPi(nominalHeading, obstaclePlusRotation) < AngleTools.computeAngleDifferenceMinusPiToPi(nominalHeading, obstacleMinusRotation))
         return obstaclePlusRotation;
      else
         return obstacleMinusRotation;
   }

   private static double findHeightOfPoint(Point2DReadOnly pointInWorld, List<NavigableRegion> navigableRegions)
   {
      return findHeightOfPoint(pointInWorld.getX(), pointInWorld.getY(), navigableRegions);
   }

   private static double findHeightOfPoint(double pointX, double pointY, List<NavigableRegion> navigableRegions)
   {
      double maxHeight = Double.NEGATIVE_INFINITY;
      for (NavigableRegion navigableRegion : navigableRegions)
      {
         PlanarRegion planarRegion = navigableRegion.getHomePlanarRegion();
         if (planarRegion.isPointInWorld2DInside(new Point3D(pointX, pointY, 0.0)))
         {
            double height = planarRegion.getPlaneZGivenXY(pointX, pointY);
            if (height > maxHeight)
               maxHeight = height;
         }
      }

      return maxHeight;
   }

   private boolean isNearCliff(Point2DReadOnly point, double maxConnectionDistance, double maxHeightDelta, NavigableRegion homeRegion,
                               List<NavigableRegion> navigableRegions)
   {
      // if point is sufficiently inside, it is not near a cliff
      Point2D closestPointToThrowAway = new Point2D();
      double distanceToContainingCluster = VisibilityTools
            .distanceToCluster(point, homeRegion.getHomeRegionCluster().getNavigableExtrusionsInWorld2D(), closestPointToThrowAway, null);
      if (distanceToContainingCluster < -desiredDistanceFromCliff)
         return false;

      List<NavigableRegion> nearbyRegions = filterNavigableRegionsWithBoundingCircle(point, maxConnectionDistance + desiredDistanceFromCliff, navigableRegions);
      List<NavigableRegion> closeEnoughRegions = filterNavigableRegionsConnectionWithDistanceAndHeightChange(homeRegion, nearbyRegions, maxConnectionDistance,
                                                                                                             maxHeightDelta);

      if (closeEnoughRegions.contains(homeRegion))
         return closeEnoughRegions.size() < 2;
      else
         return closeEnoughRegions.size() < 1;
   }

   private static List<NavigableRegion> filterNavigableRegionsConnectionWithDistanceAndHeightChange(NavigableRegion homeRegion,
                                                                                                    List<NavigableRegion> navigableRegions,
                                                                                                    double maxConnectionDistance, double maxHeightDelta)
   {
      return navigableRegions.stream().filter(
            otherRegion -> isOtherNavigableRegionWithinDistanceAndHeightDifference(homeRegion, otherRegion, maxConnectionDistance, maxHeightDelta))
                             .collect(Collectors.toList());
   }

   private static boolean isOtherNavigableRegionWithinDistanceAndHeightDifference(NavigableRegion regionA, NavigableRegion regionB,
                                                                                  double maxConnectionDistance, double maxHeightDelta)
   {
      for (Point3DReadOnly pointA : regionA.getHomeRegionCluster().getNavigableExtrusionsInWorld())
      {
         for (Point3DReadOnly pointB : regionB.getHomeRegionCluster().getNavigableExtrusionsInWorld())
         {
            if (pointA.distance(pointB) < maxConnectionDistance && Math.abs(pointA.getZ() - pointB.getZ()) < maxHeightDelta)
               return true;
         }
      }

      return false;
   }

   private static List<NavigableRegion> filterNavigableRegionsWithBoundingCircle(Point2DReadOnly circleOrigin, double circleRadius,
                                                                                 List<NavigableRegion> navigableRegions)
   {
      if (!Double.isFinite(circleRadius) || circleRadius < 0.0)
         return navigableRegions;

      return navigableRegions.stream().filter(
            navigableRegion -> PlanarRegionTools.isPlanarRegionIntersectingWithCircle(circleOrigin, circleRadius, navigableRegion.getHomePlanarRegion()))
                             .collect(Collectors.toList());
   }

   private class IntermediateComparator implements Comparator<Point2DReadOnly>
   {
      private final Point2D startPoint = new Point2D();
      private final Point2D endPoint = new Point2D();

      public void setStartPoint(Point2DReadOnly startPoint)
      {
         this.startPoint.set(startPoint);
      }

      public void setEndPoint(Point2DReadOnly endPoint)
      {
         this.endPoint.set(endPoint);
      }

      @Override
      public int compare(Point2DReadOnly pointA, Point2DReadOnly pointB)
      {
         double distanceA = EuclidGeometryTools.percentageAlongLineSegment2D(pointA, startPoint, endPoint);
         double distanceB = EuclidGeometryTools.percentageAlongLineSegment2D(pointB, startPoint, endPoint);
         return Double.compare(distanceA, distanceB);
      }
   }
}
