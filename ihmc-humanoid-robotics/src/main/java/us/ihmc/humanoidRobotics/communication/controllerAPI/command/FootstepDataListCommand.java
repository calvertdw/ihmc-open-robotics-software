package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import java.util.List;

import controller_msgs.msg.dds.FootstepDataListMessage;
import controller_msgs.msg.dds.FootstepDataMessage;
import controller_msgs.msg.dds.StepConstraintsListMessage;
import us.ihmc.communication.controllerAPI.command.QueueableCommand;
import us.ihmc.communication.packets.ExecutionTiming;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.commons.lists.RecyclingArrayList;

public class FootstepDataListCommand extends QueueableCommand<FootstepDataListCommand, FootstepDataListMessage>
{
   private long sequenceId;
   private double defaultSwingDuration;
   private double defaultTransferDuration;
   private double finalTransferDuration;
   private ExecutionTiming executionTiming = ExecutionTiming.CONTROL_DURATIONS;
   private final RecyclingArrayList<FootstepDataCommand> footsteps = new RecyclingArrayList<>(30, FootstepDataCommand.class);
   private final StepConstraintsListCommand defaultStepConstraints = new StepConstraintsListCommand();

   /** If {@code false} the controller adjust each footstep height to be at the support sole height. */
   private boolean trustHeightOfFootsteps = true;
   /** If {@code false} the controller can adjust the footsteps. */
   private boolean areFootstepsAdjustable = false;
   /** If {@code true} the controller will adjust the x and y coordinates of the upcoming footsteps with the location error of previous steps. */
   private boolean offsetFootstepsWithExecutionError = false;
   /** If {@code true} the controller will adjust the z coordinate of the upcoming footsteps with the location error of previous steps. */
   private boolean offsetFootstepsHeightWithExecutionError = false;

   private boolean shouldCheckForReachability = false;

   public FootstepDataListCommand()
   {
      clear();
   }

   @Override
   public void clear()
   {
      sequenceId = 0;
      defaultSwingDuration = 0.0;
      defaultTransferDuration = 0.0;
      finalTransferDuration = 0.0;
      footsteps.clear();
      defaultStepConstraints.clear();
      clearQueuableCommandVariables();
      shouldCheckForReachability = false;
   }

   @Override
   public void setFromMessage(FootstepDataListMessage message)
   {
      clear();

      sequenceId = message.getSequenceId();
      defaultSwingDuration = message.getDefaultSwingDuration();
      defaultTransferDuration = message.getDefaultTransferDuration();
      finalTransferDuration = message.getFinalTransferDuration();
      executionTiming = ExecutionTiming.fromByte(message.getExecutionTiming());
      trustHeightOfFootsteps = message.getTrustHeightOfFootsteps();
      areFootstepsAdjustable = message.getAreFootstepsAdjustable();
      offsetFootstepsWithExecutionError = message.getOffsetFootstepsWithExecutionError();
      offsetFootstepsHeightWithExecutionError = message.getOffsetFootstepsHeightWithExecutionError();
      shouldCheckForReachability = message.getShouldCheckForReachability();
      List<FootstepDataMessage> dataList = message.getFootstepDataList();
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      if (dataList != null)
      {
         for (int i = 0; i < dataList.size(); i++)
            footsteps.add().set(worldFrame, dataList.get(i));
      }
      StepConstraintsListMessage stepConstraints = message.getDefaultStepConstraints();
      if (stepConstraints != null)
      {
         defaultStepConstraints.setFromMessage(stepConstraints);
      }
      setQueueableCommandVariables(message.getQueueingProperties());
   }

   @Override
   public void set(FootstepDataListCommand other)
   {
      clear();

      sequenceId = other.sequenceId;
      defaultSwingDuration = other.defaultSwingDuration;
      defaultTransferDuration = other.defaultTransferDuration;
      finalTransferDuration = other.finalTransferDuration;
      executionTiming = other.executionTiming;
      adjustedExecutionTime = other.adjustedExecutionTime;
      trustHeightOfFootsteps = other.trustHeightOfFootsteps;
      areFootstepsAdjustable = other.areFootstepsAdjustable;
      offsetFootstepsWithExecutionError = other.offsetFootstepsWithExecutionError;
      offsetFootstepsHeightWithExecutionError = other.offsetFootstepsHeightWithExecutionError;
      shouldCheckForReachability = other.shouldCheckForReachability;
      RecyclingArrayList<FootstepDataCommand> otherFootsteps = other.getFootsteps();
      if (otherFootsteps != null)
      {
         for (int i = 0; i < otherFootsteps.size(); i++)
            footsteps.add().set(otherFootsteps.get(i));
      }
      defaultStepConstraints.set(other.getDefaultStepConstraints());
      setQueueableCommandVariables(other);
   }

   public void clearFoosteps()
   {
      clear();
   }

   public void addFootstep(FootstepDataCommand footstep)
   {
      footsteps.add().set(footstep);
   }

   public void setDefaultSwingDuration(double defaultSwingDuration)
   {
      this.defaultSwingDuration = defaultSwingDuration;
   }

   public void setDefaultTransferDuration(double defaultTransferDuration)
   {
      this.defaultTransferDuration = defaultTransferDuration;
   }

   public double getDefaultSwingDuration()
   {
      return defaultSwingDuration;
   }

   public double getDefaultTransferDuration()
   {
      return defaultTransferDuration;
   }

   public double getFinalTransferDuration()
   {
      return finalTransferDuration;
   }

   public StepConstraintsListCommand getDefaultStepConstraints()
   {
      return defaultStepConstraints;
   }

   public ExecutionTiming getExecutionTiming()
   {
      return executionTiming;
   }

   public RecyclingArrayList<FootstepDataCommand> getFootsteps()
   {
      return footsteps;
   }

   public void removeFootstep(int footstepIndex)
   {
      footsteps.remove(footstepIndex);
   }

   public FootstepDataCommand getFootstep(int footstepIndex)
   {
      return footsteps.get(footstepIndex);
   }

   public int getNumberOfFootsteps()
   {
      return footsteps.size();
   }

   public boolean getShouldCheckForReachability()
   {
      return shouldCheckForReachability;
   }

   @Override
   public Class<FootstepDataListMessage> getMessageClass()
   {
      return FootstepDataListMessage.class;
   }

   @Override
   public boolean isCommandValid()
   {
      return getNumberOfFootsteps() > 0 && executionModeValid();
   }

   public boolean isTrustHeightOfFootsteps()
   {
      return trustHeightOfFootsteps;
   }

   public void setAreFootstepsAdjustable(boolean areFootstepsAdjustable)
   {
      this.areFootstepsAdjustable= areFootstepsAdjustable ;
   }

   public boolean areFootstepsAdjustable()
   {
      return areFootstepsAdjustable;
   }

   public void setOffsetFootstepsWithExecutionError(boolean offsetFootstepsWithExecutionError)
   {
      this.offsetFootstepsWithExecutionError = offsetFootstepsWithExecutionError;
   }

   public boolean isOffsetFootstepsWithExecutionError()
   {
      return offsetFootstepsWithExecutionError;
   }

   public void setOffsetFootstepsHeightWithExecutionError(boolean offsetFootstepsHeightWithExecutionError)
   {
      this.offsetFootstepsHeightWithExecutionError = offsetFootstepsHeightWithExecutionError;
   }

   public boolean isOffsetFootstepsHeightWithExecutionError()
   {
      return offsetFootstepsHeightWithExecutionError;
   }

   public void setShouldCheckForReachability(boolean shouldCheckForReachability)
   {
      this.shouldCheckForReachability = shouldCheckForReachability;
   }

   @Override
   public void addTimeOffset(double timeOffset)
   {
      // Not needed for footsteps since timing is defined in durations inside the command rather then
      // absolute trajectory point times.
      throw new RuntimeException("This method should not be used with footstep lists.");
   }

   @Override
   public long getSequenceId()
   {
      return sequenceId;
   }
}
