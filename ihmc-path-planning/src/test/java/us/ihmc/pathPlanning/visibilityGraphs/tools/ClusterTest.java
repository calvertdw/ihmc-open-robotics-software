package us.ihmc.pathPlanning.visibilityGraphs.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import us.ihmc.commons.MutationTestFacilitator;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.geometry.BoundingBox2D;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;

public class ClusterTest
{
   private static final double EPSILON = 1e-10;

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testIsInsideNonNavigableZone()
   {
      Point2D pointA = new Point2D(0.0, 0.0);
      Point2D pointB = new Point2D(1.0, 0.0);
      Point2D pointC = new Point2D(1.0, 1.0);
      Point2D pointD = new Point2D(0.0, 1.0);

      Cluster obstacleCluster = new Cluster();

      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(0.1, -0.1));
      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(1.1, -0.1));
      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(1.1, 1.1));
      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(0.1, 1.1));

      assertFalse(obstacleCluster.isInsideNonNavigableZone(pointA));
      assertTrue(obstacleCluster.isInsideNonNavigableZone(pointB));
      assertTrue(obstacleCluster.isInsideNonNavigableZone(pointC));
      assertFalse(obstacleCluster.isInsideNonNavigableZone(pointD));
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testBoundingBox()
   {
      Cluster obstacleCluster = new Cluster();

      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(0.1, -0.1));
      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(1.1, -0.1));
      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(1.1, 1.6));
      obstacleCluster.addNonNavigableExtrusionInLocal(new Point2D(0.3, 1.1));

      BoundingBox2D boundingBox = obstacleCluster.getNonNavigableExtrusionsBoundingBox();
      assertEquals(0.1, boundingBox.getMinX(), EPSILON);
      assertEquals(-0.1, boundingBox.getMinY(), EPSILON);
      assertEquals(1.1, boundingBox.getMaxX(), EPSILON);
      assertEquals(1.6, boundingBox.getMaxY(), EPSILON);
   }

   public static void main(String[] args)
   {
      MutationTestFacilitator.facilitateMutationTestForClass(Cluster.class, ClusterTest.class);
   }

}
