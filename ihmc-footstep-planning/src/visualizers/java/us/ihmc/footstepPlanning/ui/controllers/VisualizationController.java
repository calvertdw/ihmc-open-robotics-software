package us.ihmc.footstepPlanning.ui.controllers;

import controller_msgs.msg.dds.BipedalSupportPlanarRegionParametersMessage;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import us.ihmc.commons.PrintTools;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.BipedalFootstepPlannerNodeRejectionReason;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.RejectionReasonToVisualize;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.*;

public class VisualizationController
{
   private static final boolean verbose = false;

   @FXML
   private CheckBox showBodyPathToggleButton;
   @FXML
   private CheckBox showRobotToggleButton;
   @FXML
   private CheckBox showInnerRegionMapsToggleButton;
   @FXML
   private CheckBox showInterRegionMapToggleButton;
   @FXML
   private CheckBox showStartMapToggleButton;
   @FXML
   private CheckBox showGoalMapToggleButton;
   @FXML
   private CheckBox showPlanarRegionsToggleButton;
   @FXML
   private CheckBox showClusterRawPointsToggleButton;
   @FXML
   private CheckBox showClusterNavigableExtrusionsToggleButton;
   @FXML
   private CheckBox showClusterNonNavigableExtrusionsToggleButton;
   @FXML
   private CheckBox showClusterPreferredNavigableExtrusionsToggleButton;
   @FXML
   private CheckBox showClusterPreferredNonNavigableExtrusionsToggleButton;
   @FXML
   private CheckBox showOccupancyMap;
   @FXML
   private CheckBox showExpandedNodes;
   @FXML
   private CheckBox showSolution;
   @FXML
   private CheckBox showIntermediateSolution;
   @FXML
   private CheckBox showPostProcessingInfo;
   @FXML
   private CheckBox showGraphSteps;
   @FXML
   private CheckBox showRejectedNodes;
   @FXML
   private ComboBox<RejectionReasonToVisualize> rejectionReasonToShow;
   @FXML
   private Slider expansionSlider;

   @FXML
   public void requestStatistics()
   {
      if (verbose)
         PrintTools.info(this, "Clicked request statistics...");

      messager.submitMessage(RequestPlannerStatistics, true);
   }

   private JavaFXMessager messager;

   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
   }

   public void bindControls()
   {
      messager.bindBidirectional(ShowBodyPath, showBodyPathToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowRobot, showRobotToggleButton.selectedProperty(), false);

      messager.bindBidirectional(ShowPlanarRegions, showPlanarRegionsToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowClusterRawPoints, showClusterRawPointsToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowClusterNavigableExtrusions, showClusterNavigableExtrusionsToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowClusterNonNavigableExtrusions, showClusterNonNavigableExtrusionsToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowClusterPreferredNavigableExtrusions, showClusterPreferredNavigableExtrusionsToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowClusterPreferredNonNavigableExtrusions, showClusterPreferredNonNavigableExtrusionsToggleButton.selectedProperty(), true);

      messager.bindBidirectional(ShowNavigableRegionVisibilityMaps, showInnerRegionMapsToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowInterRegionVisibilityMap, showInterRegionMapToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowStartVisibilityMap, showStartMapToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowGoalVisibilityMap, showGoalMapToggleButton.selectedProperty(), true);
      messager.bindBidirectional(ShowOccupancyMap, showOccupancyMap.selectedProperty(), true);
      messager.bindBidirectional(ShowExpandedNodes, showExpandedNodes.selectedProperty(), true);
      messager.bindBidirectional(ShowFootstepPlan, showSolution.selectedProperty(), true);
      messager.bindBidirectional(ShowNodeData, showIntermediateSolution.selectedProperty(), true);
      messager.bindBidirectional(ShowRejectedNodes, showRejectedNodes.selectedProperty(), true);
      messager.bindBidirectional(ShowFullGraph, showGraphSteps.selectedProperty(), true);
      messager.bindBidirectional(ShowPostProcessingInfo, showPostProcessingInfo.selectedProperty(), true);


      ObservableList<RejectionReasonToVisualize> rejectionReasons = FXCollections.observableArrayList(RejectionReasonToVisualize.values);
      rejectionReasonToShow.setItems(rejectionReasons);
      rejectionReasonToShow.setValue(RejectionReasonToVisualize.ALL);
      messager.bindBidirectional(RejectionReasonToShow, rejectionReasonToShow.valueProperty(), true);


      expansionSlider.valueProperty().addListener((observable, oldValue, newValue) -> messager.submitMessage(ExpansionFractionToShow, newValue.doubleValue()));
   }
}
