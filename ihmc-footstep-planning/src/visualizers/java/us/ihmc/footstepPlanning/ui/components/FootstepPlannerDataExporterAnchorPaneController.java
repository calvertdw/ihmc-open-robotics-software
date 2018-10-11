package us.ihmc.footstepPlanning.ui.components;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import us.ihmc.commons.PrintTools;
import us.ihmc.footstepPlanning.communication.FootstepPlannerSharedMemoryAPI;
import us.ihmc.footstepPlanning.tools.FootstepPlannerIOTools;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class FootstepPlannerDataExporterAnchorPaneController
{
   private final DirectoryChooser directoryChooser = new DirectoryChooser();
   private final File defaultDataFolder;
   private Window ownerWindow;
   private JavaFXMessager messager;

   @FXML
   private TextField currentPlanarRegionDataFolderTextField;

   public FootstepPlannerDataExporterAnchorPaneController()
   {
      File file = new File(".");

      try
      {
         URL testDataFolderURL = Thread.currentThread().getContextClassLoader().getResource(FootstepPlannerIOTools.TEST_DATA_URL);
         file = new File(testDataFolderURL.toURI());
      }
      catch(URISyntaxException e)
      {
         PrintTools.error("Could not load test data folder with URL: " + FootstepPlannerIOTools.TEST_DATA_URL);
         e.printStackTrace();
      }

      defaultDataFolder = file;
   }

   public void setMainWindow(Window ownerWindow)
   {
      this.ownerWindow = ownerWindow;
   }

   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
      currentPlanarRegionDataFolderTextField.setText(defaultDataFolder.getAbsolutePath());
   }

   @FXML
   private void browsePlanarRegionOutputFolder()
   {
      directoryChooser.setInitialDirectory(defaultDataFolder);
      File result = directoryChooser.showDialog(ownerWindow);
      if (result == null)
         return;
      String newPath = result.getAbsolutePath();
      messager.submitMessage(FootstepPlannerSharedMemoryAPI.exportUnitTestPath, newPath);
      Platform.runLater(() -> currentPlanarRegionDataFolderTextField.setText(newPath));
   }

   @FXML
   private void exportPlanarRegion()
   {
      messager.submitMessage(FootstepPlannerSharedMemoryAPI.exportUnitTestPath, currentPlanarRegionDataFolderTextField.getText());
      messager.submitMessage(FootstepPlannerSharedMemoryAPI.exportUnitTestDataFile, true);
   }
}
