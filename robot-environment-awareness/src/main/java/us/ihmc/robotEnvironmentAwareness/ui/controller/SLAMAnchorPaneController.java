package us.ihmc.robotEnvironmentAwareness.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import us.ihmc.robotEnvironmentAwareness.communication.SLAMModuleAPI;
import us.ihmc.robotEnvironmentAwareness.ui.properties.RandomICPSLAMParametersProperty;

public class SLAMAnchorPaneController extends REABasicUIController
{
   @FXML
   private ToggleButton enableSLAMButton;

   @FXML
   private TextField queuedBufferSize;

   @FXML
   private TextField slamStatus;

   @FXML
   private ToggleButton latestFrameEnable;

   @FXML
   private ToggleButton octreeMapEnable;
   
   @FXML
   private CheckBox showNormal;

   @FXML
   private ToggleButton sensorFrameEnable;

   @FXML
   private ToggleButton planarRegionsEnable;

   @FXML
   private Slider sourcePointsSlider;

   @FXML
   private Slider searchingSizeSlider;

   @FXML
   private Slider minimumOverlappedRatioSlider;

   @FXML
   private Slider windowMarginSlider;

   @FXML
   private Slider minimumInliersRatioSlider;

   @FXML
   private Label stationaryFlag;

   private final RandomICPSLAMParametersProperty ihmcSLAMParametersProperty = new RandomICPSLAMParametersProperty(this, "ihmcSLAMParameters");

   public SLAMAnchorPaneController()
   {

   }

   private void updateSensorStatusViz(boolean moving)
   {
      if (moving)
      {
         stationaryFlag.setStyle("-fx-background-color: red;");
      }
      else
      {
         stationaryFlag.setStyle("-fx-background-color: green;");
      }
   }

   @Override
   public void bindControls()
   {
      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.SLAMEnable, enableSLAMButton.selectedProperty());

      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.QueuedBuffers, queuedBufferSize.textProperty());
      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.SLAMStatus, slamStatus.textProperty());

      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.ShowLatestFrame, latestFrameEnable.selectedProperty());
      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.ShowSLAMOctreeMap, octreeMapEnable.selectedProperty());
      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.ShowSLAMOctreeNormalMap, showNormal.selectedProperty());
      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.ShowSLAMSensorTrajectory, sensorFrameEnable.selectedProperty());
      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.ShowPlanarRegionsMap, planarRegionsEnable.selectedProperty());

      ihmcSLAMParametersProperty.bindBidirectionalNumberOfSourcePoints(sourcePointsSlider.valueProperty());
      ihmcSLAMParametersProperty.bindBidirectionalMaximumICPSearchingSize(searchingSizeSlider.valueProperty());
      ihmcSLAMParametersProperty.bindBidirectionalMinimumOverlappedRatio(minimumOverlappedRatioSlider.valueProperty());
      ihmcSLAMParametersProperty.bindBidirectionalWindowSize(windowMarginSlider.valueProperty());
      ihmcSLAMParametersProperty.bindBidirectionalMinimumInliersRatioOfKeyFrame(minimumInliersRatioSlider.valueProperty());

      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.SLAMParameters, ihmcSLAMParametersProperty);

      initializeSetup();

      uiMessager.registerTopicListener(SLAMModuleAPI.SensorStatus, (moving) -> updateSensorStatusViz(moving));
   }

   private void initializeSetup()
   {
      uiMessager.broadcastMessage(SLAMModuleAPI.SLAMEnable, true);
      uiMessager.broadcastMessage(SLAMModuleAPI.ShowLatestFrame, true);
      uiMessager.broadcastMessage(SLAMModuleAPI.ShowSLAMOctreeMap, true);
      uiMessager.broadcastMessage(SLAMModuleAPI.ShowSLAMSensorTrajectory, true);
      uiMessager.broadcastMessage(SLAMModuleAPI.ShowPlanarRegionsMap, true);
   }

   @FXML
   public void clear()
   {
      uiMessager.broadcastMessage(SLAMModuleAPI.SLAMClear, true);
      uiMessager.broadcastMessage(SLAMModuleAPI.SLAMVizClear, true);
      uiMessager.broadcastMessage(SLAMModuleAPI.SensorPoseHistoryClear, true);
   }
}
