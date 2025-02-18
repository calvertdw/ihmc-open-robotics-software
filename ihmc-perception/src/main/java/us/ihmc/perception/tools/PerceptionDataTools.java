package us.ihmc.perception.tools;

import org.bytedeco.opencv.opencv_core.Mat;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.log.LogTools;
import us.ihmc.perception.BytedecoImage;

import java.nio.FloatBuffer;

public class PerceptionDataTools
{
   public static FloatBuffer convertSphericalDepthImageToPointCloudInSensorFrame(BytedecoImage imageToConvert, double verticalFOV, double horizontalFOV)
   {
      int numberOfPoints = imageToConvert.getImageHeight() * imageToConvert.getImageWidth();
      FloatBuffer depthData = NativeMemoryTools.allocate(3 * Float.BYTES * numberOfPoints).asFloatBuffer();
      depthData.rewind();

      int rowMiddle = imageToConvert.getImageHeight() / 2;
      int colMiddle = imageToConvert.getImageWidth() / 2;
      for (int row = 0; row < imageToConvert.getImageHeight(); row++)
      {
         for (int col = 0; col < imageToConvert.getImageWidth(); col++)
         {
            int colFromCenter = -col - colMiddle;
            int rowFromCenter = -(row - rowMiddle);

            double yaw = colFromCenter / (double) imageToConvert.getImageWidth() * horizontalFOV;
            double pitch = rowFromCenter / (double) imageToConvert.getImageHeight() * verticalFOV;

            int index = row * imageToConvert.getImageWidth() + col;
            double depth = imageToConvert.getPointerForAccessSpeed().getShort(index * 2L) / 1000.0;
            double r = depth * Math.cos(pitch);

            double x = r * Math.cos(yaw);
            double y = r * Math.sin(yaw);
            double z = depth * Math.sin(pitch);

            depthData.put((float) x);
            depthData.put((float) y);
            depthData.put((float) z);
         }
      }

      return depthData;
   }

   public static void fillStepInHeightMap(Mat heightMap, int rStart, int cStart, int rEnd, int cEnd, float height)
   {
      for (int i = rStart; i < rEnd; i++)
      {
         for (int j = cStart; j < cEnd; j++)
         {
            if (i >= 0 && i < heightMap.rows() && j >= 0 && j < heightMap.cols())
            {
               heightMap.ptr(i, j).putShort((short) (32768 + height * 10000));
            }
         }
      }
   }

   public static void fillStepInHeightMap(Mat heightMap, Point2D origin, Point2D dimensions, float height, boolean flipRows)
   {
      int rCenter = (int) (origin.getX() * 50 + heightMap.rows() / 2);
      int cCenter = (int) (origin.getY() * 50 + heightMap.cols() / 2);

      int rStart = rCenter - (int) (dimensions.getX() * 50) / 2;
      int cStart = cCenter - (int) (dimensions.getY() * 50) / 2;
      int rEnd = rCenter + (int) (dimensions.getX() * 50) / 2;
      int cEnd = cCenter + (int) (dimensions.getY() * 50) / 2;

      if (flipRows)
      {
         rStart = heightMap.rows() - rStart;
         rEnd = heightMap.rows() - rEnd;
      }

      LogTools.debug(String.format("rStart: %d, cStart: %d, rEnd: %d, cEnd: %d, rCenter: %d, cCenter: %d", rStart, cStart, rEnd, cEnd, rCenter, cCenter));

      fillStepInHeightMap(heightMap, rStart, cStart, rEnd, cEnd, height);
   }
}
