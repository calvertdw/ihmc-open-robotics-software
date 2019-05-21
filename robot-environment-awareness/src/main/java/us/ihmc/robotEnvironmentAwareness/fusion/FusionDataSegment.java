package us.ihmc.robotEnvironmentAwareness.fusion;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.linearAlgebra.PrincipalComponentAnalysis3D;

public class FusionDataSegment
{
   private int id = -1;

   private final int imageSegmentLabel;
   private final TIntArrayList adjacentSegmentLabels = new TIntArrayList();
   private final List<Point3D> points = new ArrayList<>();

   private final Point3D center = new Point3D();
   private final Vector3D normal = new Vector3D();

   public final Vector3D standardDeviation = new Vector3D();

   private final PrincipalComponentAnalysis3D pca = new PrincipalComponentAnalysis3D();

   private static final boolean useAdjacentScore = true;
   private static final int numberOfAdjacentPixels = 20;
   private final TIntArrayList adjacentScore = new TIntArrayList();

   public FusionDataSegment(int labelID)
   {
      imageSegmentLabel = labelID;
   }

   public boolean contains(int otherLabel)
   {
      if (useAdjacentScore)
      {
         for (int i = 0; i < adjacentSegmentLabels.size(); i++)
         {
            if (adjacentSegmentLabels.get(i) == otherLabel)
            {
               adjacentScore.replace(i, adjacentScore.get(i) + 1);
               return true;
            }
         }
         return false;
      }
      else
      {
         return adjacentSegmentLabels.contains(otherLabel);
      }
   }

   public void addAdjacentSegmentLabel(int otherLabel)
   {
      adjacentSegmentLabels.add(otherLabel);
      adjacentScore.add(1);
   }

   public void addPoint(Point3D point)
   {
      points.add(point);
   }

   public void update()
   {
      if (useAdjacentScore)
      {
         TIntArrayList newAdjacentSegmentLabels = new TIntArrayList();
         TIntArrayList newAdjacentScore = new TIntArrayList();
         for (int i = 0; i < adjacentSegmentLabels.size(); i++)
         {
            if (adjacentScore.get(i) > numberOfAdjacentPixels)
            {
               newAdjacentSegmentLabels.add(adjacentSegmentLabels.get(i));
               newAdjacentScore.add(adjacentScore.get(i));
            }
         }
         adjacentSegmentLabels.clear();
         adjacentSegmentLabels.addAll(newAdjacentSegmentLabels);
         adjacentScore.clear();
         adjacentScore.addAll(newAdjacentScore);
      }

      pca.clear();
      points.stream().forEach(point -> pca.addPoint(point.getX(), point.getY(), point.getZ()));
      pca.compute();

      pca.getMean(center);
      pca.getThirdVector(normal);

      if (normal.getZ() < 0.0)
         normal.negate();

      pca.getStandardDeviation(standardDeviation);
   }

   public void setID(int id)
   {
      this.id = id;
   }

   public boolean isSparse(double threshold)
   {
      return standardDeviation.getZ() > threshold;
   }

   public int[] getAdjacentSegmentLabels()
   {
      return adjacentSegmentLabels.toArray();
   }

   public double getWeight()
   {
      return (double) points.size();
   }

   public Point3D getCenter()
   {
      return center;
   }

   public Vector3D getNormal()
   {
      return normal;
   }

   public int getId()
   {
      return id;
   }

   public int getImageSegmentLabel()
   {
      return imageSegmentLabel;
   }

   public List<Point3D> getPoints()
   {
      return points;
   }
}
