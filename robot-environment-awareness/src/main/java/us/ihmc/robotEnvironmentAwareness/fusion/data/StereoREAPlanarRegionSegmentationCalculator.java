package us.ihmc.robotEnvironmentAwareness.fusion.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.log.LogTools;
import us.ihmc.robotEnvironmentAwareness.fusion.parameters.PlanarRegionPropagationParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationRawData;

public class StereoREAPlanarRegionSegmentationCalculator
{
   private PlanarRegionPropagationParameters planarRegionPropagationParameters = new PlanarRegionPropagationParameters();

   private static final int NUMBER_OF_ITERATE = 1000;
   private static final int MAXIMUM_NUMBER_OF_TRIALS_TO_FIND_UN_ID_LABEL = 500;
   private static final int MINIMAM_NUMBER_OF_SEGMENTATION_RAW_DATA_FOR_PLANAR_REGIEON = 3;

   private final AtomicReference<LidarImageFusionData> data = new AtomicReference<LidarImageFusionData>(null);
   private int numberOfLabels = 0;
   private final List<SegmentationNodeData> segments = new ArrayList<SegmentationNodeData>();
   private List<PlanarRegionSegmentationRawData> regionsNodeData = new ArrayList<>();

   private final Random random = new Random(0612L);

   public void updateFusionData(LidarImageFusionData lidarImageFusionData, PlanarRegionPropagationParameters propagationParameters)
   {
      lidarImageFusionData.updateSparsity(propagationParameters);
      data.set(lidarImageFusionData);
      numberOfLabels = lidarImageFusionData.getNumberOfImageSegments();
      planarRegionPropagationParameters.set(propagationParameters);
   }

   public void initialize()
   {
      segments.clear();
      regionsNodeData.clear();
   }

   public boolean calculate()
   {
      //      segments.add(createSegmentNodeData(365, 0));
      //      segments.add(createSegmentNodeData(451, 1));
      for (int i = 0; i < NUMBER_OF_ITERATE; i++)
      {
         if (!iterateSegmenataionPropagation(i))
         {
            LogTools.info("iterative is terminated " + i);
            break;
         }
      }

      if (planarRegionPropagationParameters.isEnableExtending())
      {
         extendingSegmentations();
      }

      convertNodeDataToPlanarRegionSegmentationRawData();
      return true;
   }

   private void extendingSegmentations()
   {
      for (SegmentationNodeData segment : segments)
      {
         int[] adjacentLabels = data.get().getAdjacentLabels(segment.getLabels());
         for (int adjacentLabel : adjacentLabels)
         {
            SegmentationRawData adjacentData = data.get().getFusionDataSegment(adjacentLabel);
            if (adjacentData.getId() == SegmentationRawData.DEFAULT_SEGMENT_ID)
            {
               segment.extend(adjacentData, planarRegionPropagationParameters.getExtendingDistanceThreshold(),
                              planarRegionPropagationParameters.isUpdateExtendedData(), planarRegionPropagationParameters.getExtendingRadiusThreshold());
            }

         }
      }
   }

   public List<PlanarRegionSegmentationRawData> getSegmentationRawData()
   {
      return regionsNodeData;
   }

   private void convertNodeDataToPlanarRegionSegmentationRawData()
   {
      for (SegmentationNodeData segmentationNodeData : segments)
      {
         if (segmentationNodeData.getLabels().size() < MINIMAM_NUMBER_OF_SEGMENTATION_RAW_DATA_FOR_PLANAR_REGIEON)
            continue;
         // PlanarRegionSegmentationRawData planarRegionSegmentationRawData = new PlanarRegionSegmentationRawData(segmentationNodeData.getId(),
         PlanarRegionSegmentationRawData planarRegionSegmentationRawData = new PlanarRegionSegmentationRawData(random.nextInt(),
                                                                                                               segmentationNodeData.getNormal(),
                                                                                                               segmentationNodeData.getCenter(),
                                                                                                               segmentationNodeData.getPointsInSegment());
         regionsNodeData.add(planarRegionSegmentationRawData);
      }
   }

   private boolean iterateSegmenataionPropagation(int segmentId)
   {
      int nonIDLabel = selectRandomNonIdentifiedLabel();

      if (nonIDLabel == SegmentationRawData.DEFAULT_SEGMENT_ID)
         return false;
      else
         segments.add(createSegmentNodeData(nonIDLabel, segmentId));

      return true;
   }

   /**
    * iterate computation until there is no more candidate to try merge.
    */
   private SegmentationNodeData createSegmentNodeData(int seedLabel, int segmentId)
   {
      SegmentationRawData seedImageSegment = data.get().getFusionDataSegment(seedLabel);
      seedImageSegment.setId(segmentId);
      SegmentationNodeData newSegment = new SegmentationNodeData(seedImageSegment);

      boolean isPropagating = true;

      while (isPropagating)
      {
//         LogTools.info("SegmentationNodeData " + segmentId);
         isPropagating = false;

         int[] adjacentLabels = data.get().getAdjacentLabels(newSegment.getLabels());

         for (int adjacentLabel : adjacentLabels)
         {
            SegmentationRawData candidate = data.get().getFusionDataSegment(adjacentLabel);

            if (candidate.getId() != SegmentationRawData.DEFAULT_SEGMENT_ID || candidate.isSparse())
            {
               continue;
            }

            boolean isParallel = false;
            boolean isCoplanar = false;
            if (newSegment.isParallel(candidate, planarRegionPropagationParameters.getPlanarityThreshold()))
               isParallel = true;
            if (newSegment.isCoplanar(candidate, planarRegionPropagationParameters.getProximityThreshold()))
               isCoplanar = true;

//            LogTools.info("adjacentLabel ?? " + adjacentLabel + " " + isParallel + " " + isCoplanar);
            if (isParallel && isCoplanar)
            {
               candidate.setId(segmentId);
               newSegment.merge(candidate);
               isPropagating = true;
//               LogTools.info("adjacentLabel merged " + adjacentLabel);
            }
         }
      }

      return newSegment;
   }

   private int selectRandomNonIdentifiedLabel()
   {
      int randomSeedLabel = -1;
      for (int i = 0; i < MAXIMUM_NUMBER_OF_TRIALS_TO_FIND_UN_ID_LABEL; i++)
      {
         randomSeedLabel = random.nextInt(numberOfLabels - 1);
         SegmentationRawData fusionDataSegment = data.get().getFusionDataSegment(randomSeedLabel);
         if (fusionDataSegment.getId() == SegmentationRawData.DEFAULT_SEGMENT_ID && !fusionDataSegment.isSparse())
            return randomSeedLabel;
      }
      return -1;
   }
}
