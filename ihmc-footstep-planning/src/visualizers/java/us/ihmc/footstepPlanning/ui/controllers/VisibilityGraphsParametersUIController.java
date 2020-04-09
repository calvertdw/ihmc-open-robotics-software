package us.ihmc.footstepPlanning.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersBasics;
import us.ihmc.robotEnvironmentAwareness.ui.properties.JavaFXStoredPropertyMap;
import us.ihmc.robotEnvironmentAwareness.ui.properties.StoredPropertyTableViewWrapper;
import us.ihmc.robotEnvironmentAwareness.ui.properties.StoredPropertyTableViewWrapper.ParametersTableRow;

public class VisibilityGraphsParametersUIController
{
   private JavaFXMessager messager;
   private VisibilityGraphsParametersBasics planningParameters;
   private JavaFXStoredPropertyMap javaFXStoredPropertyMap;
   private StoredPropertyTableViewWrapper tableViewWrapper;

   @FXML
   private TableView<ParametersTableRow> parameterTable;

   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
   }

   public void setVisbilityGraphsParameters(VisibilityGraphsParametersBasics parameters)
   {
      this.planningParameters = parameters;
      this.javaFXStoredPropertyMap = new JavaFXStoredPropertyMap(planningParameters);
   }

   public void bindControls()
   {
      tableViewWrapper = new StoredPropertyTableViewWrapper(380.0, 260.0, 4, parameterTable, javaFXStoredPropertyMap);
      tableViewWrapper.setTableUpdatedCallback(() -> messager.submitMessage(FootstepPlannerMessagerAPI.VisibilityGraphsParameters, planningParameters));
   }

   public void onPrimaryStageLoaded()
   {
      tableViewWrapper.removeHeader();
   }

   @FXML
   public void saveParameters()
   {
      planningParameters.save();
   }
}
