package us.ihmc.avatar.networkProcessor.kinemtaticsStreamingToolboxModule;

import static us.ihmc.avatar.networkProcessor.kinemtaticsStreamingToolboxModule.KinematicsStreamingToolboxController.KSTState.SLEEP;
import static us.ihmc.avatar.networkProcessor.kinemtaticsStreamingToolboxModule.KinematicsStreamingToolboxController.KSTState.STREAMING;

import controller_msgs.msg.dds.CapturabilityBasedStatus;
import controller_msgs.msg.dds.RobotConfigurationData;
import controller_msgs.msg.dds.WholeBodyTrajectoryMessage;
import us.ihmc.avatar.networkProcessor.modules.ToolboxController;
import us.ihmc.commons.Conversions;
import us.ihmc.communication.controllerAPI.CommandInputManager;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.packets.ToolboxState;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotics.stateMachine.core.State;
import us.ihmc.robotics.stateMachine.core.StateMachine;
import us.ihmc.robotics.stateMachine.factories.StateMachineFactory;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class KinematicsStreamingToolboxController extends ToolboxController
{
   public enum KSTState
   {
      SLEEP, STREAMING;

      private static final KSTState[] values = values();

      public byte toByte()
      {
         return (byte) ordinal();
      }

      public static KSTState fromByte(byte enumAsByte)
      {
         if (enumAsByte == -1)
            return null;
         return values[enumAsByte];
      }
   };

   private final KSTTools tools;

   private final YoDouble time = new YoDouble("time", registry);
   private final StateMachine<KSTState, State> stateMachine;

   private final KSTSleepState sleepState;
   private final KSTStreamingState streamingState;

   public KinematicsStreamingToolboxController(CommandInputManager commandInputManager, StatusMessageOutputManager statusOutputManager,
                                               FullHumanoidRobotModel desiredFullRobotModel, FullHumanoidRobotModelFactory fullRobotModelFactory,
                                               double walkingControllerPeriod, double toolboxControllerPeriod, YoGraphicsListRegistry yoGraphicsListRegistry,
                                               YoVariableRegistry parentRegistry)
   {
      super(statusOutputManager, parentRegistry);

      tools = new KSTTools(commandInputManager,
                           statusOutputManager,
                           desiredFullRobotModel,
                           fullRobotModelFactory,
                           walkingControllerPeriod,
                           toolboxControllerPeriod,
                           yoGraphicsListRegistry,
                           registry);

      sleepState = new KSTSleepState(tools);
      streamingState = new KSTStreamingState(tools);

      stateMachine = createStateMachine(time);
   }

   private StateMachine<KSTState, State> createStateMachine(DoubleProvider timeProvider)
   {
      StateMachineFactory<KSTState, State> factory = new StateMachineFactory<>(KSTState.class);
      factory.setNamePrefix("mainStateMachine").setRegistry(registry).buildYoClock(timeProvider);

      factory.addState(SLEEP, sleepState);
      factory.addState(STREAMING, streamingState);

      factory.addDoneTransition(SLEEP, STREAMING);

      return factory.build(KSTState.SLEEP);
   }

   public void setOutputPublisher(OutputPublisher outputPublisher)
   {
      streamingState.setOutputPublisher(outputPublisher);
   }

   @Override
   public boolean initialize()
   {
      return true;
   }

   private long initialTimestamp = -1L;

   @Override
   public void updateInternal()
   {
      if (initialTimestamp == -1L)
         initialTimestamp = System.nanoTime();
      time.set(Conversions.nanosecondsToSeconds(System.nanoTime() - initialTimestamp));

      stateMachine.doActionAndTransition();
   }

   @Override
   public void notifyToolboxStateChange(ToolboxState newState)
   {
      if (newState == ToolboxState.SLEEP)
         stateMachine.performTransition(SLEEP);
   }

   @Override
   public boolean isDone()
   {
      return false;
   }

   public static interface OutputPublisher
   {
      void publish(WholeBodyTrajectoryMessage messageToPublish);
   }

   KSTState getCurrentStateKey()
   {
      return stateMachine.getCurrentStateKey();
   }

   public void updateRobotConfigurationData(RobotConfigurationData newConfigurationData)
   {
      tools.updateRobotConfigurationData(newConfigurationData);
   }

   public void updateCapturabilityBasedStatus(CapturabilityBasedStatus newStatus)
   {
      tools.updateCapturabilityBasedStatus(newStatus);
   }

   public FullHumanoidRobotModel getDesiredFullRobotModel()
   {
      return tools.getDesiredFullRobotModel();
   }
}
