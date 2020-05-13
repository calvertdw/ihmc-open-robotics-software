package us.ihmc.humanoidBehaviors.tools;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.footstepPlanningModule.FootstepPlanningModuleLauncher;
import us.ihmc.commons.thread.Notification;
import us.ihmc.communication.ROS2PlanarRegionsInput;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.RemoteREAInterface;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.footstepPlanning.FootstepPlanningModule;
import us.ihmc.footstepPlanning.graphSearch.VisibilityGraphPathPlanner;
import us.ihmc.humanoidBehaviors.tools.footstepPlanner.RemoteFootstepPlannerInterface;
import us.ihmc.humanoidBehaviors.tools.ros2.ManagedROS2Node;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.TopicListener;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersBasics;
import us.ihmc.pathPlanning.visibilityGraphs.postProcessing.BodyPathPostProcessor;
import us.ihmc.pathPlanning.visibilityGraphs.postProcessing.ObstacleAvoidanceProcessor;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.tools.thread.ActivationReference;
import us.ihmc.tools.thread.PausablePeriodicThread;
import us.ihmc.tools.thread.TypedNotification;
import us.ihmc.wholeBodyController.RobotContactPointParameters;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for entry methods for developing robot behaviors. The idea is to have this be the one-stop
 * shopping location for everything one might want to do when creating a robot behavior. It should
 * hide all of the network traffic. The methods should be a useful reminder to a behavior developer
 * all of the things that one can do with the robots. This class will likely get too large
 * eventually and need to be refactored into several classes. But until that time comes it should
 * contain everything for interacting with: the robot actions (taking steps and achieving poses),
 * robot sensing (reference frames, etc.), REA (getting planar regions), footstep planning, etc. At
 * first we'll make this so that all of the things are created, even if you don't need them. But
 * later, we'll make it so that they are created as needed. The main goal is to simplify and make
 * clean the Behaviors themselves. The public interface of this class should be a joy to work with.
 * The internals and all the things it relies on might be a nightmare, but the public API should not
 * be.
 *
 * Open question: Trust vs. power vs. safety for/from behavior authors
 *
 * Robot:
 * - Command-only
 * - Status-only
 * - Interactive (footstep completion, hand trajectory completion, etc.)
 *
 * UI Communication.
 *
 * Toolbox comms:
 * - REA Input/Output
 * - Footstep planner
 *
 * Helper tools (threading, etc.)
 */
public class BehaviorHelper
{
   private final DRCRobotModel robotModel;
   private final ManagedMessager managedMessager;
   private final ManagedROS2Node managedROS2Node;
   private RemoteHumanoidRobotInterface robot;
   private RemoteFootstepPlannerInterface footstepPlannerToolbox;
   private RemoteREAInterface rea;
   private RemoteEnvironmentMapInterface environmentMap;
   private FootstepPlanningModule footstepPlanner;
   private VisibilityGraphPathPlanner bodyPathPlanner;

   public BehaviorHelper(DRCRobotModel robotModel, Messager messager, Ros2Node ros2Node)
   {
      this.robotModel = robotModel;
      managedMessager = new ManagedMessager(messager);
      managedROS2Node = new ManagedROS2Node(ros2Node);

      setCommunicationCallbacksEnabled(false); // should do this?
   }

   // Construction-only methods:
   // These not safe yet. "Create" needs to happen at construction or not at all. Maybe doesn't matter right now.

   public RemoteHumanoidRobotInterface getOrCreateRobotInterface()
   {
      if (robot == null)
         robot = new RemoteHumanoidRobotInterface(managedROS2Node, robotModel);
      return robot;
   }

   public RemoteFootstepPlannerInterface getOrCreateFootstepPlannerToolboxInterface()
   {
      if (footstepPlannerToolbox == null)
         footstepPlannerToolbox = new RemoteFootstepPlannerInterface(managedROS2Node, robotModel, managedMessager);
      return footstepPlannerToolbox; // planner toolbox
   }

   public RemoteREAInterface getOrCreateREAInterface()
   {
      if (rea == null)
         rea = new RemoteREAInterface(managedROS2Node);
      return rea; // REA toolbox
   }

   public RemoteEnvironmentMapInterface getOrCreateEnvironmentMapInterface()
   {
      if (environmentMap == null)
         environmentMap = new RemoteEnvironmentMapInterface(managedROS2Node);
      return environmentMap;
   }

   public FootstepPlanningModule getOrCreateFootstepPlanner()
   {
      if (footstepPlanner == null)
      {
         footstepPlanner = FootstepPlanningModuleLauncher.createModule(robotModel);
      }

      return footstepPlanner;
   }

   public VisibilityGraphPathPlanner getOrCreateBodyPathPlanner()
   {
      if (bodyPathPlanner == null)
      {
         VisibilityGraphsParametersBasics visibilityGraphParameters = robotModel.getVisibilityGraphsParameters();
         BodyPathPostProcessor pathPostProcessor = new ObstacleAvoidanceProcessor(visibilityGraphParameters);
         bodyPathPlanner = new VisibilityGraphPathPlanner(visibilityGraphParameters, pathPostProcessor, new YoVariableRegistry(getClass().getSimpleName()));
      }

      return bodyPathPlanner;
   }

   // UI Communication Methods:
   // Extract into class?

   public <T> void publishToUI(Topic<T> topic, T message)
   {
      managedMessager.submitMessage(topic, message);
   }

   public ActivationReference<Boolean> createBooleanActivationReference(Topic<Boolean> topic)
   {
      return managedMessager.createBooleanActivationReference(topic);
   }

   public <T> void createUICallback(Topic<T> topic, TopicListener<T> listener)
   {
      managedMessager.registerTopicListener(topic, listener);
   }

   public <T> AtomicReference<T> createUIInput(Topic<T> topic, T initialValue)
   {
      return managedMessager.createInput(topic, initialValue);
   }

   public Notification createUINotification(Topic<Object> topic)
   {
      Notification notification = new Notification();
      createUICallback(topic, object -> notification.set());
      return notification;
   }

   public <T extends K, K> TypedNotification<K> createUITypedNotification(Topic<T> topic)
   {
      TypedNotification<K> typedNotification = new TypedNotification<>();
      createUICallback(topic, message -> typedNotification.add(message));
      return typedNotification;
   }

   // ROS 2 Methods:

   public ROS2PlanarRegionsInput createPlanarRegionsInput(String specifier)
   {
      String lidarRegionsTopic = ROS2Tools.getTopicNameGenerator(null, ROS2Tools.REA_MODULE + specifier, ROS2Tools.ROS2TopicQualifier.OUTPUT)
                                          .generateTopicName(PlanarRegionsListMessage.class);
      return new ROS2PlanarRegionsInput(managedROS2Node, PlanarRegionsListMessage.class, lidarRegionsTopic);
   }

   // Thread and Schedule Methods:
   // TODO: Track and auto start/stop threads?

   public PausablePeriodicThread createPausablePeriodicThread(Class<?> clazz, double period, Runnable runnable)
   {
      return createPausablePeriodicThread(clazz.getSimpleName(), period, 0, runnable);
   }

   public PausablePeriodicThread createPausablePeriodicThread(Class<?> clazz, double period, int crashesBeforeGivingUp, Runnable runnable)
   {
      return createPausablePeriodicThread(clazz.getSimpleName(), period, crashesBeforeGivingUp, runnable);
   }

   public PausablePeriodicThread createPausablePeriodicThread(String name, double period, int crashesBeforeGivingUp, Runnable runnable)
   {
      return new PausablePeriodicThread(name, period, crashesBeforeGivingUp, runnable);
   }

   // Behavior Helper Stuff:

   // Let behaviors manage or manage for them?
   // Split into finer granularity -- publishers and subscribers?
   public void setCommunicationCallbacksEnabled(boolean enabled)
   {
      managedROS2Node.setEnabled(enabled);
      managedMessager.setEnabled(enabled);
   }

   public Messager getManagedMessager()
   {
      return managedMessager;
   }

   public ManagedROS2Node getManagedROS2Node()
   {
      return managedROS2Node;
   }

   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   public SideDependentList<ConvexPolygon2D> createFootPolygons()
   {
      RobotContactPointParameters<RobotSide> contactPointParameters = robotModel.getContactPointParameters();
      SideDependentList<ConvexPolygon2D> footPolygons = new SideDependentList<>();
      for (RobotSide side : RobotSide.values)
      {
         ArrayList<Point2D> footPoints = contactPointParameters.getFootContactPoints().get(side);
         ConvexPolygon2D scaledFoot = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(footPoints));
         footPolygons.set(side, scaledFoot);
      }

      return footPolygons;
   }
}
