package us.ihmc.humanoidBehaviors.ui;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import std_msgs.msg.dds.Empty;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.humanoidBehaviors.*;
import us.ihmc.humanoidBehaviors.ui.behaviors.DirectRobotUIController;
import us.ihmc.humanoidBehaviors.ui.graphics.ConsoleScrollPane;
import us.ihmc.javafx.JavaFXLinuxGUIRecorder;
import us.ihmc.javafx.JavaFXMissingTools;
import us.ihmc.javafx.applicationCreator.JavaFXApplicationCreator;
import us.ihmc.javafx.graphics.LabelGraphic;
import us.ihmc.humanoidBehaviors.ui.tools.JavaFXRemoteRobotVisualizer;
import us.ihmc.humanoidBehaviors.ui.tools.LocalParameterServer;
import us.ihmc.javaFXToolkit.scenes.View3DFactory;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.ros2.ROS2Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class constructs a UI for behavior operation.
 */
public class BehaviorUI
{
   private static boolean RECORD_VIDEO = Boolean.parseBoolean(System.getProperty("record.video"));

   private BorderPane mainPane;
   private final Messager behaviorMessager;
   private final ROS2Node ros2Node;
   private final Map<String, BehaviorUIInterface> behaviorUIInterfaces = new HashMap<>();
   private JavaFXLinuxGUIRecorder guiRecorder;
   private ArrayList<Runnable> onCloseRequestListeners = new ArrayList<>();
   private LocalParameterServer parameterServer;
   private JavaFXRemoteRobotVisualizer robotVisualizer;

   public static volatile Object ACTIVE_EDITOR; // a tool to assist editors in making sure there isn't more than one active

   @FXML private ChoiceBox<String> behaviorSelector;
   @FXML private Button startRecording;
   @FXML private Button stopRecording;
   @FXML private DirectRobotUIController directRobotUIController;

   public static BehaviorUI createInterprocess(BehaviorUIRegistry behaviorUIRegistry, DRCRobotModel robotModel, String behaviorModuleAddress)
   {
      return new BehaviorUI(behaviorUIRegistry,
                            RemoteBehaviorInterface.createForUI(behaviorUIRegistry, behaviorModuleAddress),
                            robotModel,
                            PubSubImplementation.FAST_RTPS);
   }

   public static BehaviorUI createIntraprocess(BehaviorUIRegistry behaviorUIRegistry, DRCRobotModel robotModel, Messager behaviorSharedMemoryMessager)
   {
      return new BehaviorUI(behaviorUIRegistry, behaviorSharedMemoryMessager, robotModel, PubSubImplementation.INTRAPROCESS);
   }

   public BehaviorUI(BehaviorUIRegistry behaviorUIRegistry, Messager behaviorMessager, DRCRobotModel robotModel, PubSubImplementation pubSubImplementation)
   {
      this.behaviorMessager = behaviorMessager;

      ros2Node = ROS2Tools.createROS2Node(pubSubImplementation, "behavior_ui");

      if (LabelGraphic.TUNING_MODE)
      {
         parameterServer = new LocalParameterServer(getClass(), 16784);
         LabelGraphic.initializeYoVariables(parameterServer.getRegistry());
         parameterServer.start();
      }

      JavaFXApplicationCreator.createAJavaFXApplication();
      Platform.runLater(() ->
      {
         mainPane = JavaFXMissingTools.loadFromFXML(this);

         BorderPane bottom = (BorderPane) mainPane.getBottom();
         TabPane tabPane = (TabPane) bottom.getCenter();

         for (BehaviorUIDefinition uiDefinitionEntry : behaviorUIRegistry.getUIDefinitionEntries())
         {
            BehaviorUIInterface behaviorUIInterface = uiDefinitionEntry.getBehaviorUISupplier().get();
            behaviorUIInterfaces.put(uiDefinitionEntry.getName(), behaviorUIInterface);
            Tab tab = new Tab(uiDefinitionEntry.getName(), JavaFXMissingTools.loadFromFXML(behaviorUIInterface));
            tabPane.getTabs().add(tab);
         }

         AnchorPane mainAnchorPane = new AnchorPane();

         View3DFactory view3DFactory = View3DFactory.createSubscene();
         view3DFactory.addCameraController(0.05, 2000.0, true);
         view3DFactory.addWorldCoordinateSystem(0.3);
         view3DFactory.addDefaultLighting();
         SubScene subScene3D = view3DFactory.getSubScene();
         Pane view3DSubSceneWrappedInsidePane = view3DFactory.getSubSceneWrappedInsidePane();
         StackPane view3DStackPane = new StackPane(view3DSubSceneWrappedInsidePane);
         AnchorPane.setTopAnchor(view3DStackPane, 0.0);
         AnchorPane.setBottomAnchor(view3DStackPane, 0.0);
         AnchorPane.setLeftAnchor(view3DStackPane, 0.0);
         AnchorPane.setRightAnchor(view3DStackPane, 0.0);
         mainAnchorPane.getChildren().add(view3DStackPane);

         VBox sideVisualizationArea = new VBox();

         ConsoleScrollPane consoleScrollPane = new ConsoleScrollPane(behaviorMessager);

         stopRecording.setDisable(true);

         behaviorSelector.getItems().add("None");
         behaviorSelector.setValue("None");

         for (BehaviorDefinition behaviorDefinition : behaviorUIRegistry.getDefinitionEntries())
         {
            behaviorSelector.getItems().add(behaviorDefinition.getName());
         }

         for (BehaviorUIInterface behaviorUIInterface : behaviorUIInterfaces.values())
         {
            behaviorUIInterface.init(subScene3D, sideVisualizationArea, ros2Node, behaviorMessager, robotModel);
            view3DFactory.addNodeToView(behaviorUIInterface);
         }

         behaviorSelector.valueProperty().addListener(this::onBehaviorSelection);

         directRobotUIController.init(mainAnchorPane, subScene3D, ros2Node, robotModel);
         view3DFactory.addNodeToView(directRobotUIController);
         robotVisualizer = new JavaFXRemoteRobotVisualizer(robotModel, ros2Node);
         view3DFactory.addNodeToView(robotVisualizer);

         SplitPane mainSplitPane = (SplitPane) mainPane.getCenter();
         mainSplitPane.getItems().add(mainAnchorPane);
         mainSplitPane.getItems().add(consoleScrollPane);
         mainSplitPane.setDividerPositions(2.0 / 3.0);

         Stage primaryStage = new Stage();
         primaryStage.setTitle("Behavior UI");
         primaryStage.setMaximized(false);
         Scene mainScene = new Scene(mainPane, 1750, 1000);

         primaryStage.setScene(mainScene);
         primaryStage.show();
         primaryStage.toFront();

         primaryStage.setOnCloseRequest(event -> onCloseRequestListeners.forEach(Runnable::run));

         guiRecorder = new JavaFXLinuxGUIRecorder(primaryStage, 24, 0.8f, getClass().getSimpleName());
         onCloseRequestListeners.add(guiRecorder::stop);
         Runtime.getRuntime().addShutdownHook(new Thread(guiRecorder::stop, "GUIRecorderStop"));

         if (RECORD_VIDEO)
         {
            ThreadTools.scheduleSingleExecution("DelayRecordingStart", this::startRecording, 2.0);
            ThreadTools.scheduleSingleExecution("SafetyStop", guiRecorder::stop, 1200.0);
         }

         IHMCROS2Publisher<Empty> shutdownPublisher = ROS2Tools.createPublisher(ros2Node, BehaviorModule.API.SHUTDOWN);
         onCloseRequestListeners.add(() ->
         {
            LogTools.info("Publishing SHUTDOWN on {}", BehaviorModule.API.SHUTDOWN.getName());
            shutdownPublisher.publish(new Empty());
            destroy();
         });

         // do this last for now in case events starts firing early
         consoleScrollPane.setupAtEnd();
      });
   }

   @FXML public void startRecording()
   {
      startRecording.setDisable(true);
      stopRecording.setDisable(false);
      guiRecorder.deleteOldLogs(15);
      ThreadTools.startAThread(() -> guiRecorder.start(), "RecordingStart");
      ThreadTools.scheduleSingleExecution("SafetyStop", guiRecorder::stop, 3600.0);
   }

   @FXML public void stopRecording()
   {
      startRecording.setDisable(false);
      stopRecording.setDisable(true);
      ThreadTools.startAThread(() -> guiRecorder.stop(), "RecordingStop");
   }

   public void addOnCloseRequestListener(Runnable onCloseRequest)
   {
      onCloseRequestListeners.add(onCloseRequest);
   }

   public void closeMessager()
   {
      ExceptionTools.handle(behaviorMessager::closeMessager, DefaultExceptionHandler.PRINT_STACKTRACE);
   }

   private void onBehaviorSelection(ObservableValue<? extends String> observable, String oldValue, String newValue)
   {
      behaviorMessager.submitMessage(BehaviorModule.API.BehaviorSelection, newValue);

      for (String behaviorName : behaviorUIInterfaces.keySet())
      {
         if (newValue.equals(behaviorName))
         {
            behaviorUIInterfaces.get(behaviorName).setEnabled(true);
         }
         else
         {
            behaviorUIInterfaces.get(behaviorName).setEnabled(false);
         }
      }
   }

   public static void claimEditing(Object claimingEditor)
   {
      if (BehaviorUI.ACTIVE_EDITOR != null)
      {
         throw new RuntimeException("Only one editor may be active at a time.");
      }
      else
      {
         BehaviorUI.ACTIVE_EDITOR = claimingEditor;
         LogTools.debug("editor activated: {}", claimingEditor.getClass().getSimpleName());
      }
   }

   public void destroy()
   {
      if (robotVisualizer != null)
         robotVisualizer.destroy();
      if (parameterServer != null)
         parameterServer.destroy();
      for (BehaviorUIInterface behaviorUIInterface : behaviorUIInterfaces.values())
      {
         behaviorUIInterface.destroy();
      }
      directRobotUIController.destroy();
      ros2Node.destroy();
   }
}
