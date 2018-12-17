package us.ihmc.footstepPlanning.ui.controllers;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.AbortPlanningTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ComputePathTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GlobalResetTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalOrientationTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalPositionTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartOrientationTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartPositionTopic;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.javaFXToolkit.messager.MessageBidirectionalBinding.PropertyToMessageTypeConverter;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.messager.TopicListener;
import us.ihmc.pathPlanning.visibilityGraphs.ui.properties.Point3DProperty;
import us.ihmc.pathPlanning.visibilityGraphs.ui.properties.YawProperty;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullRobotModel;

public class MainTabController
{
   private static final boolean verbose = false;

   // control
   @FXML
   private ComboBox<FootstepPlannerType> plannerType;
   @FXML
   private CheckBox acceptNewRegions;
   @FXML
   private Spinner<Double> timeout;
   @FXML
   private Spinner<Double> horizonLength;

   // status
   @FXML
   private TextField sentRequestId;
   @FXML
   private TextField receivedRequestId;

   @FXML
   private TextField timeTaken;
   @FXML
   private TextField planningResult;
   @FXML
   private TextField plannerStatus;

   // goal placement
   @FXML
   private ToggleButton placeStart;
   @FXML
   private ToggleButton placeGoal;

   @FXML
   private ToggleButton rotateStart;
   @FXML
   private ToggleButton rotateGoal;

   @FXML
   private Spinner<Double> startXPosition;
   @FXML
   private Spinner<Double> startYPosition;
   @FXML
   private Spinner<Double> startZPosition;
   @FXML
   private Spinner<Double> goalXPosition;
   @FXML
   private Spinner<Double> goalYPosition;
   @FXML
   private Spinner<Double> goalZPosition;

   @FXML
   private Spinner<Double> startYaw;
   @FXML
   private Spinner<Double> goalYaw;

   @FXML
   public void computePath()
   {
      if (verbose)
         PrintTools.info(this, "Clicked compute path...");

      int newRequestID = currentPlannerRequestId.get() + 1;
      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerRequestIdTopic, newRequestID);
      messager.submitMessage(ComputePathTopic, true);
   }

   @FXML
   public void abortPlanning()
   {
      if (verbose)
         PrintTools.info(this, "Clicked abort planning...");

      messager.submitMessage(AbortPlanningTopic, true);
   }

   @FXML
   public void sendPlan()
   {

   }

   private JavaFXMessager messager;

   private AtomicReference<Integer> currentPlannerRequestId;
   private final AtomicBoolean requestSetStartPoseFromRobot = new AtomicBoolean(false);
   private AnimationTimer robotPoseHandler;
   private HumanoidReferenceFrames humanoidReferenceFrames;

   private final Point3DProperty startPositionProperty = new Point3DProperty(this, "startPositionProperty", new Point3D());
   private final Point3DProperty goalPositionProperty = new Point3DProperty(this, "goalPositionProperty", new Point3D());

   private final YawProperty startRotationProperty = new YawProperty(this, "startRotationProperty", 0.0);
   private final YawProperty goalRotationProperty = new YawProperty(this, "goalRotationProperty", 0.0);

   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;

      currentPlannerRequestId = messager.createInput(FootstepPlannerMessagerAPI.PlannerRequestIdTopic, -1);

   }

   private void setupControls()
   {
      startXPosition.setValueFactory(createStartGoalPositionValueFactory());
      startYPosition.setValueFactory(createStartGoalPositionValueFactory());
      startZPosition.setValueFactory(createStartGoalPositionValueFactory());

      goalXPosition.setValueFactory(createStartGoalPositionValueFactory());
      goalYPosition.setValueFactory(createStartGoalPositionValueFactory());
      goalZPosition.setValueFactory(createStartGoalPositionValueFactory());

      startYaw.setValueFactory(createStartGoalOrientationValueFactory());
      goalYaw.setValueFactory(createStartGoalOrientationValueFactory());

      ObservableList<us.ihmc.footstepPlanning.FootstepPlannerType> plannerTypeOptions = FXCollections.observableArrayList(FootstepPlannerType.values);
      plannerType.setItems(plannerTypeOptions);
      plannerType.setValue(FootstepPlannerType.A_STAR);

      timeout.setValueFactory(createTimeoutValueFactory());
      horizonLength.setValueFactory(createHorizonValueFactory());
   }

   public void bindControls()
   {
      setupControls();


      // control
      messager.bindBidirectional(FootstepPlannerMessagerAPI.PlannerTypeTopic, plannerType.valueProperty(), true);
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlannerRequestIdTopic, new TextViewerListener<>(sentRequestId));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.ReceivedPlanIdTopic, new TextViewerListener<>(receivedRequestId));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlannerTimeTakenTopic, new TextViewerListener<>(timeTaken));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlanningResultTopic, new TextViewerListener<>(planningResult));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlannerStatusTopic, new TextViewerListener<>(plannerStatus));

      messager.bindBidirectional(FootstepPlannerMessagerAPI.AcceptNewPlanarRegions, acceptNewRegions.selectedProperty(), true);

      messager.bindBidirectional(FootstepPlannerMessagerAPI.PlannerTimeoutTopic, timeout.getValueFactory().valueProperty(), doubleToDoubleConverter, true);

      messager.bindBidirectional(FootstepPlannerMessagerAPI.PlannerHorizonLengthTopic, horizonLength.getValueFactory().valueProperty(), doubleToDoubleConverter, true);


      // set goal
      messager.bindBidirectional(FootstepPlannerMessagerAPI.StartPositionEditModeEnabledTopic, placeStart.selectedProperty(), false);
      messager.bindBidirectional(FootstepPlannerMessagerAPI.GoalPositionEditModeEnabledTopic, placeGoal.selectedProperty(), false);
      messager.bindBidirectional(FootstepPlannerMessagerAPI.StartOrientationEditModeEnabledTopic, rotateStart.selectedProperty(), false);
      messager.bindBidirectional(FootstepPlannerMessagerAPI.GoalOrientationEditModeEnabledTopic, rotateGoal.selectedProperty(), false);

      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.GoalPositionEditModeEnabledTopic, placeStart.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.StartOrientationEditModeEnabledTopic, placeStart.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.GoalOrientationEditModeEnabledTopic, placeStart.disableProperty());

      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.StartPositionEditModeEnabledTopic, placeGoal.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.StartOrientationEditModeEnabledTopic, placeGoal.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.GoalOrientationEditModeEnabledTopic, placeGoal.disableProperty());

      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.StartPositionEditModeEnabledTopic, rotateStart.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.GoalPositionEditModeEnabledTopic, rotateStart.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.GoalOrientationEditModeEnabledTopic, rotateStart.disableProperty());

      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.StartPositionEditModeEnabledTopic, rotateGoal.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.GoalPositionEditModeEnabledTopic, rotateGoal.disableProperty());
      messager.bindPropertyToTopic(FootstepPlannerMessagerAPI.StartOrientationEditModeEnabledTopic, rotateGoal.disableProperty());

      startPositionProperty.bindBidirectionalX(startXPosition.getValueFactory().valueProperty());
      startPositionProperty.bindBidirectionalY(startYPosition.getValueFactory().valueProperty());
      startPositionProperty.bindBidirectionalZ(startZPosition.getValueFactory().valueProperty());
      messager.bindBidirectional(StartPositionTopic, startPositionProperty, false);

      goalPositionProperty.bindBidirectionalX(goalXPosition.getValueFactory().valueProperty());
      goalPositionProperty.bindBidirectionalY(goalYPosition.getValueFactory().valueProperty());
      goalPositionProperty.bindBidirectionalZ(goalZPosition.getValueFactory().valueProperty());

      messager.bindBidirectional(FootstepPlannerMessagerAPI.GoalPositionTopic, goalPositionProperty, false);

      messager.bindBidirectional(GoalPositionTopic, goalPositionProperty, false);

      startRotationProperty.bindBidirectionalYaw(startYaw.getValueFactory().valueProperty());
      messager.bindBidirectional(StartOrientationTopic, startRotationProperty, false);

      goalRotationProperty.bindBidirectionalYaw(goalYaw.getValueFactory().valueProperty());
      messager.bindBidirectional(GoalOrientationTopic, goalRotationProperty, false);

      robotPoseHandler = new AnimationTimer()
      {
         @Override
         public void handle(long now)
         {
            if(requestSetStartPoseFromRobot.getAndSet(false))
            {
               humanoidReferenceFrames.updateFrames();
               MovingReferenceFrame midFeetZUpFrame = humanoidReferenceFrames.getMidFeetZUpFrame();
               FramePose3D startPose = new FramePose3D();
               startPose.setToZero(midFeetZUpFrame);
               startPose.changeFrame(ReferenceFrame.getWorldFrame());

               double x = startPose.getX();
               double y = startPose.getY();
               double yaw = startPose.getYaw();

               startPositionProperty.set(new Point3D(x, y, startPositionProperty.get().getZ()));
               startRotationProperty.set(new Quaternion(yaw, 0.0, 0.0));
            }
         }
      };
      robotPoseHandler.start();

      messager.registerTopicListener(GlobalResetTopic, reset -> clearStartGoalTextFields());
   }

   @FXML
   private void setStartPoseFromRobot()
   {
      requestSetStartPoseFromRobot.set(true);
   }

   public void setFullRobotModel(FullHumanoidRobotModel fullHumanoidRobotModel)
   {
      this.humanoidReferenceFrames = new HumanoidReferenceFrames(fullHumanoidRobotModel);
   }

   private void clearStartGoalTextFields()
   {
      startXPosition.valueFactoryProperty().getValue().setValue(0.0);
      startYPosition.valueFactoryProperty().getValue().setValue(0.0);
      startZPosition.valueFactoryProperty().getValue().setValue(0.0);

      goalXPosition.valueFactoryProperty().getValue().setValue(0.0);
      goalYPosition.valueFactoryProperty().getValue().setValue(0.0);
      goalZPosition.valueFactoryProperty().getValue().setValue(0.0);

      startYaw.valueFactoryProperty().getValue().setValue(0.0);
      goalYaw.valueFactoryProperty().getValue().setValue(0.0);
   }

   private SpinnerValueFactory.DoubleSpinnerValueFactory createStartGoalPositionValueFactory()
   {
      double min = -100.0;
      double max = 100.0;
      double amountToStepBy = 0.1;
      return new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, 0.0, amountToStepBy);
   }

   private SpinnerValueFactory.DoubleSpinnerValueFactory createStartGoalOrientationValueFactory()
   {
      double min = -Math.PI;
      double max = Math.PI;
      double amountToStepBy = 0.1;
      return new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, 0.0, amountToStepBy);
   }

   private SpinnerValueFactory.DoubleSpinnerValueFactory createTimeoutValueFactory()
   {
      double min = 0.0;
      double max = 500.0;
      double amountToStepBy = 5;
      return new DoubleSpinnerValueFactory(min, max, 15.0, amountToStepBy);
   }

   private SpinnerValueFactory.DoubleSpinnerValueFactory createHorizonValueFactory()
   {
      double min = 0.0;
      double max = 1000.0;
      double amountToStepBy = 0.25;
      return new DoubleSpinnerValueFactory(min, max, 0.0, amountToStepBy);
   }

   private final PropertyToMessageTypeConverter<Double, Double> doubleToDoubleConverter = new PropertyToMessageTypeConverter<Double, Double>()
   {
      @Override
      public Double convert(Double propertyValue)
      {
         return propertyValue;
      }

      @Override
      public Double interpret(Double newValue)
      {
         return newValue;
      }
   };

   private class TextViewerListener<T> implements TopicListener<T>
   {
      private final TextField textField;
      public TextViewerListener(TextField textField)
      {
         this.textField = textField;
      }

      public void receivedMessageForTopic(T messageContent)
      {
         if (messageContent != null)
            textField.promptTextProperty().setValue(messageContent.toString());
      }
   }
}
