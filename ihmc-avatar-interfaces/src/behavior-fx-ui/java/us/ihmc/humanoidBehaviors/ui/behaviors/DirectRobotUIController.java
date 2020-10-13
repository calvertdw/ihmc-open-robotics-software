package us.ihmc.humanoidBehaviors.ui.behaviors;

import com.sun.javafx.collections.ImmutableObservableList;
import controller_msgs.msg.dds.*;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import std_msgs.msg.dds.Empty;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.supportingPlanarRegionPublisher.BipedalSupportPlanarRegionPublisher;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.MathTools;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.controllerAPI.RobotLowLevelMessenger;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.euclid.referenceFrame.FrameYawPitchRoll;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehaviorAPI;
import us.ihmc.avatar.drcRobot.RemoteSyncedRobotModel;
import us.ihmc.humanoidBehaviors.tools.ThrottledRobotStateCallback;
import us.ihmc.humanoidBehaviors.ui.graphics.live.LivePlanarRegionsGraphic;
import us.ihmc.humanoidBehaviors.ui.tools.AtlasDirectRobotInterface;
import us.ihmc.humanoidBehaviors.ui.tools.ValkyrieDirectRobotInterface;
import us.ihmc.humanoidBehaviors.ui.video.JavaFXROS2VideoView;
import us.ihmc.humanoidBehaviors.ui.video.JavaFXROS2VideoViewOverlay;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.GoHomeCommand;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.javafx.JavaFXReactiveSlider;
import us.ihmc.log.LogTools;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.robotEnvironmentAwareness.communication.SLAMModuleAPI;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.partNames.NeckJointName;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.ros2.ROS2TopicNameTools;
import us.ihmc.tools.string.StringTools;

public class DirectRobotUIController extends Group
{
   private static final double MIN_PELVIS_HEIGHT = 0.52;
   private static final double MAX_PELVIS_HEIGHT = 0.90;
   private static final double PELVIS_HEIGHT_RANGE = MAX_PELVIS_HEIGHT - MIN_PELVIS_HEIGHT;
   private static final double MIN_CHEST_PITCH = Math.toRadians(-15.0);
   private static final double MAX_CHEST_PITCH = Math.toRadians(50.0);
   private static final double CHEST_PITCH_RANGE = MAX_CHEST_PITCH - MIN_CHEST_PITCH;
   private static final double SLIDER_RANGE = 100.0;
   private static final double ROBOT_DATA_EXPIRATION = 1.0;

   @FXML private ComboBox<Integer> pumpPSI;
   @FXML private CheckBox enableSupportRegions;
   @FXML private Spinner<Double> supportRegionScale;
   @FXML private CheckBox showLidarRegions;
   @FXML private CheckBox showRealsenseRegions;
   @FXML private CheckBox showMapRegions;
   @FXML private CheckBox showSupportRegions;
   @FXML private CheckBox showMultisenseVideo;
   @FXML private CheckBox showRealsenseVideo;
   @FXML private Slider stanceHeightSlider;
   @FXML private Slider leanForwardSlider;
   @FXML private Slider neckSlider;

   private RobotLowLevelMessenger robotLowLevelMessenger;
   private RemoteSyncedRobotModel syncedRobotForHeightSlider;
   private RemoteSyncedRobotModel syncedRobotForChestSlider;
   private IHMCROS2Publisher<GoHomeMessage> goHomePublisher;
   private IHMCROS2Publisher<BipedalSupportPlanarRegionParametersMessage> supportRegionsParametersPublisher;
   private IHMCROS2Publisher<REAStateRequestMessage> reaStateRequestPublisher;
   private IHMCROS2Publisher<Empty> clearSLAMPublisher;
   private IHMCROS2Publisher<NeckTrajectoryMessage> neckTrajectoryPublisher;
   private IHMCROS2Publisher<ChestTrajectoryMessage> chestTrajectoryPublisher;
   private IHMCROS2Publisher<PelvisHeightTrajectoryMessage> pelvisHeightTrajectoryPublisher;
   private LivePlanarRegionsGraphic lidarRegionsGraphic;
   private LivePlanarRegionsGraphic realsenseRegionsGraphic;
   private LivePlanarRegionsGraphic mapRegionsGraphic;
   private LivePlanarRegionsGraphic supportRegionsGraphic;
   private JavaFXROS2VideoViewOverlay multisenseVideoOverlay;
   private StackPane multisenseVideoStackPane;
   private JavaFXROS2VideoViewOverlay realsenseVideoOverlay;
   private StackPane realsenseVideoStackPane;
   private JavaFXReactiveSlider stanceHeightReactiveSlider;
   private JavaFXReactiveSlider leanForwardReactiveSlider;
   private JavaFXReactiveSlider neckReactiveSlider;

   public void init(AnchorPane mainAnchorPane, SubScene subScene, ROS2Node ros2Node, DRCRobotModel robotModel)
   {
      String robotName = robotModel.getSimpleRobotName();
      FullHumanoidRobotModel fullRobotModel = robotModel.createFullRobotModel();
      syncedRobotForHeightSlider = new RemoteSyncedRobotModel(robotModel, ros2Node);
      syncedRobotForChestSlider = new RemoteSyncedRobotModel(robotModel, ros2Node);

      if (robotName.toLowerCase().contains("atlas"))
      {
         robotLowLevelMessenger = new AtlasDirectRobotInterface(ros2Node, robotModel);

         neckTrajectoryPublisher = new IHMCROS2Publisher<>(ros2Node,
                                                           ControllerAPIDefinition.getTopic(NeckTrajectoryMessage.class, robotName));
         chestTrajectoryPublisher = new IHMCROS2Publisher<>(ros2Node,
                                                           ControllerAPIDefinition.getTopic(ChestTrajectoryMessage.class, robotName));
         pelvisHeightTrajectoryPublisher = new IHMCROS2Publisher<>(ros2Node,
                                                                   ControllerAPIDefinition.getTopic(PelvisHeightTrajectoryMessage.class, robotName));
         OneDoFJointBasics neckJoint = fullRobotModel.getNeckJoint(NeckJointName.PROXIMAL_NECK_PITCH);
         double neckJointLimitUpper = neckJoint.getJointLimitUpper();
         double neckJointJointLimitLower = neckJoint.getJointLimitLower();
         double neckJointRange = neckJointLimitUpper - neckJointJointLimitLower;

         stanceHeightReactiveSlider = new JavaFXReactiveSlider(stanceHeightSlider, value ->
         {
            if (syncedRobotForHeightSlider.getDataReceptionTimerSnapshot().isRunning(ROBOT_DATA_EXPIRATION))
            {
               syncedRobotForHeightSlider.update();
               double sliderValue = value.doubleValue();
               double pelvisZ = syncedRobotForHeightSlider.getFramePoseReadOnly(HumanoidReferenceFrames::getPelvisZUpFrame).getZ();
               double midFeetZ = syncedRobotForHeightSlider.getFramePoseReadOnly(HumanoidReferenceFrames::getMidFeetZUpFrame).getZ();
               double desiredHeight = MIN_PELVIS_HEIGHT + PELVIS_HEIGHT_RANGE * sliderValue / SLIDER_RANGE;
               double desiredHeightInWorld = desiredHeight + midFeetZ;
               LogTools.info(StringTools.format3D("Commanding height trajectory. slider: {} desired: {} (pelvis - midFeetZ): {} in world: {}",
                                                  sliderValue,
                                                  desiredHeight,
                                                  pelvisZ - midFeetZ,
                                                  desiredHeightInWorld));
               PelvisHeightTrajectoryMessage message = new PelvisHeightTrajectoryMessage();
               message.getEuclideanTrajectory()
                      .set(HumanoidMessageTools.createEuclideanTrajectoryMessage(2.0,
                                                                                 new Point3D(0.0, 0.0, desiredHeightInWorld),
                                                                                 ReferenceFrame.getWorldFrame()));
               long frameId = MessageTools.toFrameId(ReferenceFrame.getWorldFrame());
               message.getEuclideanTrajectory().getFrameInformation().setDataReferenceFrameId(frameId);
               message.getEuclideanTrajectory().getSelectionMatrix().setXSelected(false);
               message.getEuclideanTrajectory().getSelectionMatrix().setYSelected(false);
               message.getEuclideanTrajectory().getSelectionMatrix().setZSelected(true);
               pelvisHeightTrajectoryPublisher.publish(message);
            }
         });
         leanForwardReactiveSlider = new JavaFXReactiveSlider(leanForwardSlider, value ->
         {
            if (syncedRobotForChestSlider.getDataReceptionTimerSnapshot().isRunning(ROBOT_DATA_EXPIRATION))
            {
               syncedRobotForChestSlider.update();
               double sliderValue = 100.0 - value.doubleValue();
               double desiredChestPitch = MIN_CHEST_PITCH + CHEST_PITCH_RANGE * sliderValue / SLIDER_RANGE;

               FrameYawPitchRoll frameChestYawPitchRoll = new FrameYawPitchRoll(syncedRobotForChestSlider.getReferenceFrames().getChestFrame());
               frameChestYawPitchRoll.changeFrame(syncedRobotForChestSlider.getReferenceFrames().getPelvisZUpFrame());
               frameChestYawPitchRoll.setPitch(desiredChestPitch);
               frameChestYawPitchRoll.changeFrame(ReferenceFrame.getWorldFrame());

               LogTools.info(StringTools.format3D("Commanding chest pitch. slider: {} pitch: {}", sliderValue, desiredChestPitch));

               ChestTrajectoryMessage message = new ChestTrajectoryMessage();
               message.getSo3Trajectory()
                      .set(HumanoidMessageTools.createSO3TrajectoryMessage(2.0,
                                                                           frameChestYawPitchRoll,
                                                                           EuclidCoreTools.zeroVector3D,
                                                                           ReferenceFrame.getWorldFrame()));
               long frameId = MessageTools.toFrameId(ReferenceFrame.getWorldFrame());
               message.getSo3Trajectory().getFrameInformation().setDataReferenceFrameId(frameId);

               chestTrajectoryPublisher.publish(message);
            }
         });
         neckReactiveSlider = new JavaFXReactiveSlider(neckSlider, sliderValue ->
         {
            double percent = sliderValue.doubleValue() / 100.0;
            percent = 1.0 - percent;
            MathTools.checkIntervalContains(percent, 0.0, 1.0);
            double jointAngle = neckJointJointLimitLower + percent * neckJointRange;
            LogTools.info("Commanding neck trajectory: slider: {} angle: {}", neckSlider.getValue(), jointAngle);
            neckTrajectoryPublisher.publish(HumanoidMessageTools.createNeckTrajectoryMessage(3.0, new double[] {jointAngle}));
         });

         new ThrottledRobotStateCallback(ros2Node, robotModel, 5.0, syncedRobot ->
         {
            double pelvisZ = syncedRobot.getFramePoseReadOnly(HumanoidReferenceFrames::getPelvisZUpFrame).getZ();
            double midFeetZ = syncedRobot.getFramePoseReadOnly(HumanoidReferenceFrames::getMidFeetZUpFrame).getZ();
            double midFeetToPelvis = pelvisZ - midFeetZ;
            double heightInRange = midFeetToPelvis - MIN_PELVIS_HEIGHT;
            double newHeightSliderValue = SLIDER_RANGE * heightInRange / PELVIS_HEIGHT_RANGE;
            stanceHeightReactiveSlider.acceptUpdatedValue(newHeightSliderValue);

            FrameYawPitchRoll chestFrame = new FrameYawPitchRoll(syncedRobot.getReferenceFrames().getChestFrame());
            chestFrame.changeFrame(syncedRobot.getReferenceFrames().getPelvisZUpFrame());
            double leanForwardValue = chestFrame.getPitch();
            double pitchInRange = leanForwardValue - MIN_CHEST_PITCH;
            double newChestSliderValue = SLIDER_RANGE * pitchInRange / CHEST_PITCH_RANGE;
            double flippedChestSliderValue = 100.0 - newChestSliderValue;
            leanForwardReactiveSlider.acceptUpdatedValue(flippedChestSliderValue);

            double neckAngle = syncedRobot.getFullRobotModel().getNeckJoint(NeckJointName.PROXIMAL_NECK_PITCH).getQ();
            double angleInRange = neckAngle - neckJointJointLimitLower;
            double newNeckSliderValue = SLIDER_RANGE * angleInRange / neckJointRange;
            double flippedNeckSliderValue = 100.0 - newNeckSliderValue;
            neckReactiveSlider.acceptUpdatedValue(flippedNeckSliderValue);
         });
      }
      else if (robotName.toLowerCase().contains("valkyrie"))
      {
         robotLowLevelMessenger = new ValkyrieDirectRobotInterface(ros2Node, robotModel);
      }
      else
      {
         throw new RuntimeException("Please add implementation of RobotLowLevelMessenger for " + robotName);
      }

      goHomePublisher = ROS2Tools.createPublisherTypeNamed(ros2Node,
                                                           ROS2TopicNameTools.newMessageInstance(GoHomeCommand.class).getMessageClass(),
                                                           ROS2Tools.getControllerInputTopic(robotName));

      supportRegionsParametersPublisher = ROS2Tools.createPublisherTypeNamed(ros2Node,
                                                                             BipedalSupportPlanarRegionParametersMessage.class,
                                                                             BipedalSupportPlanarRegionPublisher.getTopic(robotName));

      pumpPSI.setItems(new ImmutableObservableList<>(1500, 2300, 2500, 2800));
      pumpPSI.getSelectionModel().select(1);
      pumpPSI.valueProperty().addListener((ChangeListener) -> sendPumpPSI());

      lidarRegionsGraphic = new LivePlanarRegionsGraphic(ros2Node, ROS2Tools.LIDAR_REA_REGIONS, false);
      lidarRegionsGraphic.setEnabled(false);
      getChildren().add(lidarRegionsGraphic);
      realsenseRegionsGraphic = new LivePlanarRegionsGraphic(ros2Node, LookAndStepBehaviorAPI.REGIONS_FOR_FOOTSTEP_PLANNING, false);
      realsenseRegionsGraphic.setEnabled(false);
      getChildren().add(realsenseRegionsGraphic);
      mapRegionsGraphic = new LivePlanarRegionsGraphic(ros2Node, ROS2Tools.MAP_REGIONS, false);
      mapRegionsGraphic.setEnabled(false);
      getChildren().add(mapRegionsGraphic);
      supportRegionsGraphic = new LivePlanarRegionsGraphic(ros2Node, ROS2Tools.BIPEDAL_SUPPORT_REGIONS, false);
      supportRegionsGraphic.setEnabled(false);
      getChildren().add(supportRegionsGraphic);

      multisenseVideoOverlay = new JavaFXROS2VideoViewOverlay(new JavaFXROS2VideoView(ros2Node, ROS2Tools.VIDEO, 1024, 544, false, false));
      multisenseVideoStackPane = new StackPane(multisenseVideoOverlay.getNode());
      multisenseVideoStackPane.setVisible(false);
      AnchorPane.setTopAnchor(multisenseVideoStackPane, 10.0);
      AnchorPane.setRightAnchor(multisenseVideoStackPane, 10.0);
      multisenseVideoOverlay.getNode().addEventHandler(MouseEvent.MOUSE_PRESSED, event -> multisenseVideoOverlay.toggleMode());
      mainAnchorPane.getChildren().add(multisenseVideoStackPane);

      realsenseVideoOverlay = new JavaFXROS2VideoViewOverlay(new JavaFXROS2VideoView(ros2Node, ROS2Tools.D435_VIDEO, 640, 480, false, false));
      realsenseVideoStackPane = new StackPane(realsenseVideoOverlay.getNode());
      realsenseVideoStackPane.setVisible(false);
      AnchorPane.setBottomAnchor(realsenseVideoStackPane, 10.0);
      AnchorPane.setRightAnchor(realsenseVideoStackPane, 10.0);
      realsenseVideoOverlay.getNode().addEventHandler(MouseEvent.MOUSE_PRESSED, event -> realsenseVideoOverlay.toggleMode());
      mainAnchorPane.getChildren().add(realsenseVideoStackPane);

      reaStateRequestPublisher = new IHMCROS2Publisher<>(ros2Node, ROS2Tools.REA_STATE_REQUEST);
      clearSLAMPublisher = ROS2Tools.createPublisher(ros2Node, SLAMModuleAPI.CLEAR);

      supportRegionScale.setValueFactory(new DoubleSpinnerValueFactory(0.0, 10.0, BipedalSupportPlanarRegionPublisher.defaultScaleFactor, 0.1));
      supportRegionScale.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> sendSupportRegionParameters());
      enableSupportRegions.setSelected(true);
      enableSupportRegions.selectedProperty().addListener(observable -> sendSupportRegionParameters());
   }

   @FXML public void homeAll()
   {
      GoHomeMessage homeLeftArm = new GoHomeMessage();
      homeLeftArm.setHumanoidBodyPart(GoHomeMessage.HUMANOID_BODY_PART_ARM);
      homeLeftArm.setRobotSide(GoHomeMessage.ROBOT_SIDE_LEFT);
      goHomePublisher.publish(homeLeftArm);

      GoHomeMessage homeRightArm = new GoHomeMessage();
      homeRightArm.setHumanoidBodyPart(GoHomeMessage.HUMANOID_BODY_PART_ARM);
      homeRightArm.setRobotSide(GoHomeMessage.ROBOT_SIDE_RIGHT);
      goHomePublisher.publish(homeRightArm);
   }

   @FXML public void freeze()
   {
      robotLowLevelMessenger.sendFreezeRequest();
   }

   @FXML public void standPrep()
   {
      robotLowLevelMessenger.sendStandRequest();
   }

   @FXML public void shutdown()
   {
      robotLowLevelMessenger.sendShutdownRequest();
   }

   private void sendPumpPSI()
   {
      robotLowLevelMessenger.setHydraulicPumpPSI(pumpPSI.getValue());
   }

   private void sendSupportRegionParameters()
   {
      BipedalSupportPlanarRegionParametersMessage supportPlanarRegionParametersMessage = new BipedalSupportPlanarRegionParametersMessage();
      supportPlanarRegionParametersMessage.setEnable(enableSupportRegions.isSelected());
      supportPlanarRegionParametersMessage.setSupportRegionScaleFactor(supportRegionScale.getValue());
      LogTools.info("Sending {}, {}", enableSupportRegions.isSelected(), supportRegionScale.getValue());
      supportRegionsParametersPublisher.publish(supportPlanarRegionParametersMessage);
   }

   @FXML public void showLidarRegions()
   {
      lidarRegionsGraphic.setEnabled(showLidarRegions.isSelected());
      lidarRegionsGraphic.clear();
   }

   @FXML public void showRealsenseRegions()
   {
      realsenseRegionsGraphic.setEnabled(showRealsenseRegions.isSelected());
      realsenseRegionsGraphic.clear();
   }

   @FXML public void showMapRegions()
   {
      mapRegionsGraphic.setEnabled(showMapRegions.isSelected());
      mapRegionsGraphic.clear();
   }

   @FXML public void showSupportRegions()
   {
      supportRegionsGraphic.setEnabled(showSupportRegions.isSelected());
      supportRegionsGraphic.clear();
   }

   @FXML public void showMultisenseVideo()
   {
      multisenseVideoStackPane.setVisible(showMultisenseVideo.isSelected());
      if (showMultisenseVideo.isSelected())
      {
         multisenseVideoOverlay.start();
      }
      else
      {
         multisenseVideoOverlay.stop();
      }
   }

   @FXML public void showRealsenseVideo()
   {
      realsenseVideoStackPane.setVisible(showRealsenseVideo.isSelected());
      if (showRealsenseVideo.isSelected())
      {
         realsenseVideoOverlay.start();
      }
      else
      {
         realsenseVideoOverlay.stop();
      }
   }

   @FXML public void clearREA()
   {
      REAStateRequestMessage clearMessage = new REAStateRequestMessage();
      clearMessage.setRequestClear(true);
      reaStateRequestPublisher.publish(clearMessage);
   }

   @FXML public void clearSLAM()
   {
      clearSLAMPublisher.publish(new Empty());
   }

   public void destroy()
   {
      lidarRegionsGraphic.destroy();
      mapRegionsGraphic.destroy();
      realsenseRegionsGraphic.destroy();
      supportRegionsGraphic.destroy();
   }
}
