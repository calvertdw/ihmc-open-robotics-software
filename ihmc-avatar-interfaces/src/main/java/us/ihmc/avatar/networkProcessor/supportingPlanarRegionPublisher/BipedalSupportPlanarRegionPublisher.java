package us.ihmc.avatar.networkProcessor.supportingPlanarRegionPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import controller_msgs.msg.dds.*;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.kinematicsToolboxModule.KinematicsToolboxHelper;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.matrix.interfaces.RotationMatrixReadOnly;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingStatus;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullRobotModelUtils;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.ros2.RealtimeROS2Node;
import us.ihmc.tools.thread.CloseableAndDisposable;

public class BipedalSupportPlanarRegionPublisher implements CloseableAndDisposable
{
   public static final double defaultScaleFactor = 2.0;

   private static final int LEFT_FOOT_INDEX = 0;
   private static final int RIGHT_FOOT_INDEX = 1;
   private static final int CONVEX_HULL_INDEX = 2;

   private final RealtimeROS2Node ros2Node;
   private final IHMCRealtimeROS2Publisher<PlanarRegionsListMessage> regionPublisher;

   private final AtomicReference<CapturabilityBasedStatus> latestCapturabilityBasedStatusMessage = new AtomicReference<>(null);
   private final AtomicReference<RobotConfigurationData> latestRobotConfigurationData = new AtomicReference<>(null);
   private final AtomicReference<BipedalSupportPlanarRegionParametersMessage> latestParametersMessage = new AtomicReference<>(null);

   private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));
   private ScheduledFuture<?> task;

   private final FullHumanoidRobotModel fullRobotModel;
   private final OneDoFJointBasics[] allJointsExcludingHands;
   private final HumanoidReferenceFrames referenceFrames;
   private final SideDependentList<ContactablePlaneBody> contactableFeet;
   private final SideDependentList<List<FramePoint2D>> scaledContactPointList = new SideDependentList<>(new ArrayList<>(), new ArrayList<>());
   private final List<PlanarRegion> supportRegions = new ArrayList<>();

   public BipedalSupportPlanarRegionPublisher(DRCRobotModel robotModel, PubSubImplementation pubSubImplementation)
   {
      String robotName = robotModel.getSimpleRobotName();
      fullRobotModel = robotModel.createFullRobotModel();
      allJointsExcludingHands = FullRobotModelUtils.getAllJointsExcludingHands(fullRobotModel);
      referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      ContactableBodiesFactory<RobotSide> contactableBodiesFactory = new ContactableBodiesFactory<>();
      contactableBodiesFactory.setFullRobotModel(fullRobotModel);
      contactableBodiesFactory.setReferenceFrames(referenceFrames);
      contactableBodiesFactory.setFootContactPoints(robotModel.getContactPointParameters().getControllerFootGroundContactPoints());
      contactableFeet = new SideDependentList<>(contactableBodiesFactory.createFootContactablePlaneBodies());

      ros2Node = ROS2Tools.createRealtimeROS2Node(pubSubImplementation, "supporting_planar_region_publisher");

      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node,
                                                    CapturabilityBasedStatus.class, ROS2Tools.getControllerOutputTopic(robotName),
                                           subscriber -> latestCapturabilityBasedStatusMessage.set(subscriber.takeNextData()));
      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node,
                                                    RobotConfigurationData.class, ROS2Tools.getControllerOutputTopic(robotName),
                                           subscriber -> latestRobotConfigurationData.set(subscriber.takeNextData()));
      regionPublisher = ROS2Tools.createPublisherTypeNamed(ros2Node,
                                                           PlanarRegionsListMessage.class,
                                                           REACommunicationProperties.subscriberCustomRegionsTopicName);
      ROS2Tools.createCallbackSubscription(ros2Node, BipedalSupportPlanarRegionParametersMessage.class, getTopic(robotName),
                                           s -> latestParametersMessage.set(s.takeNextData()));
      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node,
                                                    WalkingStatusMessage.class,
                                                    ControllerAPIDefinition.getOutputTopic(robotName),
                                                    status ->
                                                    {
                                                       if (WalkingStatus.fromByte(status.takeNextData().getWalkingStatus())
                                                       == WalkingStatus.STARTED)
                                                       {
                                                          BipedalSupportPlanarRegionParametersMessage parameters = new BipedalSupportPlanarRegionParametersMessage();
                                                          parameters.setEnable(false);
                                                          latestParametersMessage.set(parameters);
                                                       }
                                                    });

      BipedalSupportPlanarRegionParametersMessage defaultParameters = new BipedalSupportPlanarRegionParametersMessage();
      defaultParameters.setEnable(true);
      defaultParameters.setSupportRegionScaleFactor(defaultScaleFactor);
      latestParametersMessage.set(defaultParameters);

      for (int i = 0; i < 3; i++)
      {
         supportRegions.add(new PlanarRegion());
      }
   }

   public void start()
   {
      ros2Node.spin();
      task = executorService.scheduleWithFixedDelay(this::run, 0, 1, TimeUnit.SECONDS);
   }

   private void run()
   {
      BipedalSupportPlanarRegionParametersMessage parameters = latestParametersMessage.get();
      if (!parameters.getEnable() || parameters.getSupportRegionScaleFactor() <= 0.0)
      {
         supportRegions.set(LEFT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(RIGHT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(CONVEX_HULL_INDEX, new PlanarRegion());

         publishRegions();
         return;
      }

      CapturabilityBasedStatus capturabilityBasedStatus = latestCapturabilityBasedStatusMessage.get();
      if (capturabilityBasedStatus == null)
      {
         return;
      }

      for (RobotSide robotSide : RobotSide.values)
      {
         scaledContactPointList.get(robotSide).clear();
         for (FramePoint2D contactPoint : contactableFeet.get(robotSide).getContactPoints2d())
         {
            FramePoint2D scaledContactPoint = new FramePoint2D(contactPoint);
            scaledContactPoint.scale(parameters.getSupportRegionScaleFactor());
            scaledContactPointList.get(robotSide).add(scaledContactPoint);
         }
      }

      RobotConfigurationData robotConfigurationData = latestRobotConfigurationData.get();
      if (robotConfigurationData == null)
      {
         return;
      }

      KinematicsToolboxHelper.setRobotStateFromRobotConfigurationData(robotConfigurationData, fullRobotModel.getRootJoint(), allJointsExcludingHands);

      referenceFrames.updateFrames();

      SideDependentList<Boolean> isInSupport = new SideDependentList<Boolean>(!capturabilityBasedStatus.getLeftFootSupportPolygon3d().isEmpty(),
                                                                              !capturabilityBasedStatus.getRightFootSupportPolygon3d().isEmpty());
      if (feetAreInSamePlane(isInSupport))
      {
         ReferenceFrame leftSoleFrame = contactableFeet.get(RobotSide.LEFT).getSoleFrame();

         List<FramePoint2D> allContactPoints = new ArrayList<>();
         allContactPoints.addAll(scaledContactPointList.get(RobotSide.LEFT));
         allContactPoints.addAll(scaledContactPointList.get(RobotSide.RIGHT));
         allContactPoints.forEach(p -> p.changeFrameAndProjectToXYPlane(leftSoleFrame));

         supportRegions.set(LEFT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(RIGHT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(CONVEX_HULL_INDEX, new PlanarRegion(leftSoleFrame.getTransformToWorldFrame(),
                                                new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(allContactPoints))));
      }
      else
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            if (isInSupport.get(robotSide))
            {
               ContactablePlaneBody contactableFoot = contactableFeet.get(robotSide);
               List<FramePoint2D> contactPoints = scaledContactPointList.get(robotSide);
               RigidBodyTransform transformToWorld = contactableFoot.getSoleFrame().getTransformToWorldFrame();
               supportRegions.set(robotSide.ordinal(),
                                  new PlanarRegion(transformToWorld, new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(contactPoints))));
            }
            else
            {
               supportRegions.set(robotSide.ordinal(), new PlanarRegion());
            }
         }

         supportRegions.set(CONVEX_HULL_INDEX, new PlanarRegion());
      }

      publishRegions();
   }

   private void publishRegions()
   {
      for (int i = 0; i < 3; i++)
      {
         supportRegions.get(i).setRegionId(i);
      }

      regionPublisher.publish(PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(new PlanarRegionsList(supportRegions)));
   }

   private boolean feetAreInSamePlane(SideDependentList<Boolean> isInSupport)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         if (!isInSupport.get(robotSide))
         {
            return false;
         }
      }
      ReferenceFrame leftSoleFrame = contactableFeet.get(RobotSide.LEFT).getSoleFrame();
      ReferenceFrame rightSoleFrame = contactableFeet.get(RobotSide.RIGHT).getSoleFrame();
      RigidBodyTransform relativeSoleTransform = leftSoleFrame.getTransformToDesiredFrame(rightSoleFrame);
      RotationMatrixReadOnly relativeOrientation = relativeSoleTransform.getRotation();

      double rotationEpsilon = Math.toRadians(3.0);
      double translationEpsilon = 0.02;
      return Math.abs(relativeOrientation.getPitch()) < rotationEpsilon && Math.abs(relativeOrientation.getRoll()) < rotationEpsilon
            && Math.abs(relativeSoleTransform.getTranslationZ()) < translationEpsilon;
   }

   public void stop()
   {
      task.cancel(false);
   }

   public void destroy()
   {
      stop();
      executorService.shutdownNow();
      ros2Node.destroy();
   }

   @Override
   public void closeAndDispose()
   {
      destroy();
   }

   public static ROS2Topic<BipedalSupportPlanarRegionParametersMessage> getTopic(String robotName)
   {
      return ROS2Tools.BIPED_SUPPORT_REGION_PUBLISHER.withRobot(robotName)
                                                     .withInput().withType(BipedalSupportPlanarRegionParametersMessage.class);
   }
}
