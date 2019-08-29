package us.ihmc.pathPlanning.visibilityGraphs.postProcessing;

import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.commons.InterpolationTools;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Vector2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.pathPlanning.bodyPathPlanner.BodyPathPlannerTools;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapSolution;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsParameters;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityTools;
import us.ihmc.robotics.geometry.AngleTools;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PathOrientationCalculator
{
   private final VisibilityGraphsParameters parameters;

   public PathOrientationCalculator(VisibilityGraphsParameters parameters)
   {
      this.parameters = parameters;
   }

   public List<Pose3DReadOnly> computePosesFromPath(List<? extends Point3DReadOnly> path, VisibilityMapSolution visibilityMapSolution)
   {
      List<Pose3D> newPathPoses = new ArrayList<>();
      List<Cluster> allObstacleClusters = new ArrayList<>();
      visibilityMapSolution.getNavigableRegions().getNaviableRegionsList().forEach(region -> allObstacleClusters.addAll(region.getObstacleClusters()));

      int size = path.size();
      double startHeading = BodyPathPlannerTools.calculateHeading(path.get(0), path.get(1));

      newPathPoses.add(new Pose3D(path.get(0), new Quaternion(startHeading, 0.0, 0.0)));
      for (int i = 1; i < path.size() - 1; i++)
      {
         Point3DReadOnly previousPosition = path.get(i - 1);
         Point3DReadOnly currentPosition = path.get(i);
         Point3DReadOnly nextPosition = path.get(i + 1);

         Point2D currentPosition2D = new Point2D(currentPosition);

         double previousHeading = BodyPathPlannerTools.calculateHeading(previousPosition, currentPosition);
         double nextHeading = BodyPathPlannerTools.calculateHeading(currentPosition, nextPosition);
         double desiredOrientation = InterpolationTools.linearInterpolate(previousHeading, nextHeading, 0.5);

         Point2D closestObstaclePoint = new Point2D();
         double distanceToClosestPoint = Double.POSITIVE_INFINITY;
         for (Cluster cluster : allObstacleClusters)
         {
            Point2D closestPointInCluster = new Point2D();
            double distance = VisibilityTools.distanceToCluster(currentPosition2D, cluster.getNonNavigableExtrusionsInWorld2D(), closestPointInCluster, null);
            if (distance < distanceToClosestPoint)
            {
               distanceToClosestPoint = distance;
               closestObstaclePoint = closestPointInCluster;
            }
         }

         Vector2D vectorToObstacle = new Vector2D();
         vectorToObstacle.sub(closestObstaclePoint, currentPosition2D);

         if (previousPosition.distanceXY(currentPosition) < 2.0 * parameters.getPreferredObstacleExtrusionDistance())
            desiredOrientation = newPathPoses.get(i - 1).getOrientation().getYaw();

         desiredOrientation = getHeadingToAvoidObstacles(desiredOrientation, vectorToObstacle);
         newPathPoses.add(new Pose3D(path.get(i), new Quaternion(desiredOrientation, 0.0, 0.0)));
      }

      double endHeading = BodyPathPlannerTools.calculateHeading(path.get(size - 2), path.get(size - 1));

      newPathPoses.add(new Pose3D(path.get(size - 1), new Quaternion(endHeading, 0.0, 0.0)));

      return newPathPoses.parallelStream().map(Pose3D::new).collect(Collectors.toList());
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

      TDoubleArrayList possibleHeadings = new TDoubleArrayList();
      possibleHeadings.add(AngleTools.trimAngleMinusPiToPi(headingToObstacle + rotationForAvoidance));
      possibleHeadings.add(AngleTools.trimAngleMinusPiToPi(headingToObstacle + rotationForAvoidance + Math.PI));
      possibleHeadings.add(AngleTools.trimAngleMinusPiToPi(headingToObstacle - rotationForAvoidance));
      possibleHeadings.add(AngleTools.trimAngleMinusPiToPi(headingToObstacle - rotationForAvoidance + Math.PI));

      double bestHeading = nominalHeading;
      double smallestAngleDifference = Double.POSITIVE_INFINITY;
      for (int i = 0; i < possibleHeadings.size(); i++)
      {
         double heading = possibleHeadings.get(i);
         double angleDifference = Math.abs(AngleTools.computeAngleDifferenceMinusPiToPi(heading, nominalHeading));
         if (angleDifference < smallestAngleDifference)
         {
            bestHeading = heading;
            smallestAngleDifference = angleDifference;
         }
      }

      return bestHeading;
   }
}
