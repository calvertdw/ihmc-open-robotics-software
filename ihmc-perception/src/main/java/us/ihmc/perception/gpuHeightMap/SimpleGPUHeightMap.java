package us.ihmc.perception.gpuHeightMap;

import controller_msgs.msg.dds.HeightMapMessage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.ejml.data.DMatrixRMaj;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.heightMap.HeightMapTools;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class SimpleGPUHeightMap
{
   private final Point2D center = new Point2D();
   private double resolution;
   private int cellsPerSide;

   private final DMatrixRMaj heightDataMap = new DMatrixRMaj(0, 0);
   private final DMatrixRMaj varianceDataMap = new DMatrixRMaj(0, 0);
   private final DMatrixRMaj countDataMap = new DMatrixRMaj(0, 0);

   public void setResolution(double resolution)
   {
      this.resolution = resolution;
   }

   public void setCenter(double x, double y)
   {
      center.set(x, y);
   }

   public double getCellX(int element)
   {
      int x = element / cellsPerSide;

      return x * resolution + center.getX() - resolution * cellsPerSide * 0.5;
   }

   public double getCellY(int element)
   {
      int y = element % cellsPerSide;
      return y * resolution + center.getY() - resolution * cellsPerSide * 0.5;
   }

   public double getCellZ(int element)
   {
      return heightDataMap.get(element);
   }

   public double getVariance(int element)
   {
      return varianceDataMap.get(element);
   }

   public Point2DReadOnly getCenter()
   {
      return center;
   }

   public Point2DReadOnly getCellLocation(int x, int y)
   {
      Point2D cellLocation = new Point2D((x - 0.5 * cellsPerSide) * resolution, (y - 0.5 * cellsPerSide) * resolution);
      cellLocation.add(center);

      return cellLocation;
   }

   public double getResolution()
   {
      return resolution;
   }

   public int getCellsPerSide()
   {
      return cellsPerSide;
   }

   public int getXIndex(double value)
   {
      return getIndex(value, center.getX());
   }

   public int getYIndex(double value)
   {
      return getIndex(value, center.getY());
   }

   public double getHeightAtPoint(Point2DReadOnly point)
   {
      return getHeightAtPoint(point.getX(), point.getY());
   }

   public double getHeightAtPoint(double x, double y)
   {
      return heightDataMap.get(getXIndex(x), getYIndex(y));
   }

   public double getVarianceAtPoint(Point2DReadOnly point)
   {
      return getVarianceAtPoint(point.getX(), point.getY());
   }

   public double getVarianceAtPoint(double x, double y)
   {
      return varianceDataMap.get(getXIndex(x), getYIndex(y));
   }

   public double getPointsAtPoint(Point2DReadOnly point)
   {
      return getPointsAtPoint(point.getX(), point.getY());
   }

   public double getPointsAtPoint(double x, double y)
   {
      return countDataMap.get(getXIndex(x), getYIndex(y));
   }

   private int getIndex(double value, double center)
   {
      return getIndex(value, center, resolution, cellsPerSide);
   }

   public static int getIndex(double value, double center, double resolution, int cellsPerSide)
   {
      int idx = (int) ((value - center) / resolution + 0.5 * cellsPerSide);

      return MathTools.clamp(idx, 0, cellsPerSide);
   }

   public void updateFromFloatBufferImage(FloatBuffer heightBuffer,
                                          FloatBuffer varianceBuffer,
                                          Mat countMat,
                                          int cellsPerSide)
   {
      this.cellsPerSide = cellsPerSide;

      heightBuffer.position(0);
      varianceBuffer.position(0);
//      countBuffer.position(0);
      heightDataMap.reshape(cellsPerSide, cellsPerSide);
      varianceDataMap.reshape(cellsPerSide, cellsPerSide);
      countDataMap.reshape(cellsPerSide, cellsPerSide);

      for (int y = 0; y < cellsPerSide; y++)
      {
         for (int x = 0; x < cellsPerSide; x++)
         {
            heightDataMap.set(x, y, heightBuffer.get());
            varianceDataMap.set(x, y, varianceBuffer.get());
            int count = Byte.toUnsignedInt(countMat.ptr(y, x).get());
            countDataMap.set(x, y, count);
         }
      }
   }
   public void updateFromFloatBuffer(FloatBuffer floatBuffer, int cellsPerSide)
   {
      this.cellsPerSide = cellsPerSide;

      floatBuffer.position(0);
      heightDataMap.reshape(cellsPerSide, cellsPerSide);
      varianceDataMap.reshape(cellsPerSide, cellsPerSide);
      countDataMap.reshape(cellsPerSide, cellsPerSide);

      for (int x = 0; x < cellsPerSide; x++)
      {
         for (int y = 0; y < cellsPerSide; y++)
            heightDataMap.set(x, y, floatBuffer.get());
      }
      for (int x = 0; x < cellsPerSide; x++)
      {
         for (int y = 0; y < cellsPerSide; y++)
            varianceDataMap.set(x, y, floatBuffer.get());
      }
      for (int x = 0; x < cellsPerSide; x++)
      {
         for (int y = 0; y < cellsPerSide; y++)
         {
            countDataMap.set(x, y, floatBuffer.get());
         }
      }
   }

   public HeightMapMessage buildMessage()
   {
      // Copy and report over messager
      HeightMapMessage message = new HeightMapMessage();
      message.setGridSizeXy(cellsPerSide * resolution);
      message.setXyResolution(resolution);
      message.setGridCenterX(center.getX());
      message.setGridCenterY(center.getY());

      int centerIndex = HeightMapTools.computeCenterIndex(cellsPerSide * resolution, resolution);

      for (int xIndex = 0; xIndex < heightDataMap.getNumRows(); xIndex++)
      {
         for (int yIndex = 0; yIndex < heightDataMap.getNumCols(); yIndex++)
         {
//            if (!MathTools.epsilonEquals(heightDataMap.get(xIndex, yIndex), 0.0, 1e-5))
            if (countDataMap.get(xIndex, yIndex) > 0)
            {
               int key = HeightMapTools.indicesToKey(xIndex, yIndex, centerIndex);
               message.getKeys().add(key);
               message.getHeights().add((float) heightDataMap.get(xIndex, yIndex));
            }
         }
      }

      return message;
   }
}
