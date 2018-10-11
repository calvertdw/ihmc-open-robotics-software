package us.ihmc.footstepPlanning;

import org.junit.Assert;
import org.junit.Test;
import us.ihmc.commons.PrintTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.tools.FootstepPlannerIOTools;
import us.ihmc.footstepPlanning.tools.FootstepPlannerIOTools.FootstepPlannerUnitTestDataset;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityGraphsIOTools.VisibilityGraphsUnitTestDataset;
import us.ihmc.pathPlanning.visibilityGraphs.ui.VisibilityGraphsDataExporter;
import us.ihmc.robotics.geometry.PlanarRegionsList;

import java.util.List;
import java.util.Random;

public abstract class FootstepPlannerDataSetTest
{
   // Whether to start the UI or not.
   protected static boolean VISUALIZE = false;
   // For enabling helpful prints.
   protected static boolean DEBUG = true;

   public abstract FootstepPlannerType getPlannerType();

   @Test(timeout = 500000)
   @ContinuousIntegrationTest(estimatedDuration = 13.0)
   public void testDatasetsWithoutOcclusion()
   {
      runAssertionsOnAllDatasets(dataset -> runAssertionsWithoutOcclusion(dataset));
   }

   protected void runAssertionsOnAllDatasets(DatasetTestRunner datasetTestRunner)
   {
      List<FootstepPlannerUnitTestDataset> allDatasets = FootstepPlannerIOTools.loadAllFootstepPlannerDatasets(VisibilityGraphsDataExporter.class);

      if (DEBUG)
      {
         PrintTools.info("Unit test files found: " + allDatasets.size());
      }

      int numberOfFailingDatasets = 0;
      int numberOfTotalDatasets = 0;
      String errorMessages = "";

      int currentDatasetIndex = 0;
      if (allDatasets.isEmpty())
         Assert.fail("Did not find any datasets to test.");

      // Randomizing the regionIds so the viz is better
      Random random = new Random(324);
      allDatasets.stream().map(VisibilityGraphsUnitTestDataset::getPlanarRegionsList).map(PlanarRegionsList::getPlanarRegionsAsList)
                 .forEach(regionsList -> regionsList.forEach(region -> region.setRegionId(random.nextInt())));

      FootstepPlannerUnitTestDataset dataset = allDatasets.get(currentDatasetIndex);

      while (dataset != null)
      {
         if (DEBUG)
         {
            PrintTools.info("Processing file: " + dataset.getDatasetName());
         }

         boolean hasType = false;
         for (FootstepPlannerType type : dataset.getTypes())
         {
            if (getPlannerType() == type)
               hasType = true;
         }

         if (hasType)
         {
            String errorMessagesForCurrentFile = datasetTestRunner.testDataset(dataset);
            if (!errorMessagesForCurrentFile.isEmpty())
               numberOfFailingDatasets++;
            errorMessages += errorMessagesForCurrentFile;
            numberOfTotalDatasets++;
         }

         currentDatasetIndex++;
         if (currentDatasetIndex < allDatasets.size())
            dataset = allDatasets.get(currentDatasetIndex);
         else
            dataset = null;

         ThreadTools.sleep(100); // Apparently need to give some time for the prints to appear in the right order.
      }

      Assert.assertTrue("Number of failing datasets: " + numberOfFailingDatasets + " out of " + numberOfTotalDatasets + ". Errors:" + errorMessages,
                        errorMessages.isEmpty());
   }

   protected String runAssertionsWithoutOcclusion(FootstepPlannerUnitTestDataset dataset)
   {
      submitDataSet(dataset);

      return findPlanAndAssertGoodResult(dataset);
   }

   protected void runAssertionsOnDataset(DatasetTestRunner datasetTestRunner, String datasetName)
   {
      List<FootstepPlannerUnitTestDataset> allDatasets = FootstepPlannerIOTools.loadAllFootstepPlannerDatasets(VisibilityGraphsDataExporter.class);

      if (DEBUG)
      {
         PrintTools.info("Unit test files found: " + allDatasets.size());
      }

      String errorMessages = "";

      if (allDatasets.isEmpty())
         Assert.fail("Did not find any datasets to test.");

      // Randomizing the regionIds so the viz is better
      Random random = new Random(324);
      allDatasets.stream().map(VisibilityGraphsUnitTestDataset::getPlanarRegionsList).map(PlanarRegionsList::getPlanarRegionsAsList)
                 .forEach(regionsList -> regionsList.forEach(region -> region.setRegionId(random.nextInt())));

      FootstepPlannerUnitTestDataset dataset = null;
      for (FootstepPlannerUnitTestDataset datasetToQuery : allDatasets)
      {
         if (datasetToQuery.getDatasetName().equals(datasetName))
         {
            dataset = datasetToQuery;
            break;
         }
      }

      if (dataset == null)
         throw new RuntimeException("Dataset " + datasetName + " does not exist!");

      if (DEBUG)
      {
         PrintTools.info("Processing file: " + dataset.getDatasetName());
      }

      String errorMessagesForCurrentFile = datasetTestRunner.testDataset(dataset);
      errorMessages += errorMessagesForCurrentFile;

      ThreadTools.sleep(100); // Apparently need to give some time for the prints to appear in the right order.

      Assert.assertTrue("Errors:" + errorMessages, errorMessages.isEmpty());
   }

   public abstract void submitDataSet(FootstepPlannerUnitTestDataset dataset);

   public abstract String findPlanAndAssertGoodResult(FootstepPlannerUnitTestDataset dataset);

   protected static interface DatasetTestRunner
   {
      String testDataset(FootstepPlannerUnitTestDataset dataset);
   }
}
