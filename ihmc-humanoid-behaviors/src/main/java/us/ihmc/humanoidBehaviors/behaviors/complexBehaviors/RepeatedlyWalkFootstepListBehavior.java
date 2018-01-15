package us.ihmc.humanoidBehaviors.behaviors.complexBehaviors;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridgeInterface;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingStatusMessage;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.MovingReferenceFrame;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class RepeatedlyWalkFootstepListBehavior extends AbstractBehavior
{
   private static final int defaultNumberOfStepsToTake = 10;
   private static final int defaultNumberOfIterations = 5;

   private final YoBoolean walkingForward = new YoBoolean("walkingFotward", registry);
   private final YoDouble footstepLength = new YoDouble("footstepLength", registry);
   private final YoDouble footstepWidth = new YoDouble("footstepWidth", registry);
   private final YoDouble swingTime = new YoDouble("swingTime", registry);
   private final YoDouble transferTime = new YoDouble("transferTime", registry);
   private final YoInteger numberOfStepsToTake = new YoInteger("numberOfStepsToTake", registry);
   private final YoInteger iterations = new YoInteger("iterations", registry);
   private final YoInteger iterationCounter = new YoInteger("iterationCounter", registry);
   private final YoEnum<RobotSide> initialSwingSide = YoEnum.create("initialSwingSide", RobotSide.class, registry);

   private final FootstepDataListMessage forwardFootstepList = new FootstepDataListMessage();
   private final FootstepDataListMessage backwardFootstepList = new FootstepDataListMessage();

   private final AtomicInteger stepsAlongCurrentList = new AtomicInteger();

   private final SideDependentList<MovingReferenceFrame> soleFrames;
   private final ReferenceFrame midFootZUpFrame;

   public RepeatedlyWalkFootstepListBehavior(CommunicationBridgeInterface communicationBridge, HumanoidReferenceFrames referenceFrames,
                                             YoVariableRegistry parentRegistry)
   {
      super(communicationBridge);

      soleFrames = referenceFrames.getSoleFrames();
      midFootZUpFrame = referenceFrames.getMidFeetZUpFrame();

      communicationBridge.attachListener(WalkingStatusMessage.class, (packet) ->
      {
         if(packet.status.equals(WalkingStatusMessage.Status.COMPLETED))
         {
            stepsAlongCurrentList.incrementAndGet();
         }
      });

      walkingForward.set(true);
      initialSwingSide.set(RobotSide.RIGHT);

      numberOfStepsToTake.set(defaultNumberOfStepsToTake);
      iterations.set(defaultNumberOfIterations);
      swingTime.set(1.5);
      transferTime.set(0.3);
      footstepLength.set(0.3);
      footstepWidth.set(0.25);

      parentRegistry.addChild(registry);
   }

   @Override
   public void onBehaviorEntered()
   {
      computeForwardFootstepList();
      computeBackwardFootstepList();

      sendPacket(forwardFootstepList);
      walkingForward.set(true);
   }

   private void computeForwardFootstepList()
   {
      forwardFootstepList.clear();

      RobotSide swingSide = initialSwingSide.getEnumValue();

      for (int i = 0; i < numberOfStepsToTake.getIntegerValue(); i++)
      {
         FootstepDataMessage footstepDataMessage = constructFootstepDataMessage(midFootZUpFrame, footstepLength.getDoubleValue() * (i + 1), 0.5 * swingSide.negateIfRightSide(footstepWidth.getDoubleValue()), swingSide);
         forwardFootstepList.add(footstepDataMessage);

         swingSide = swingSide.getOppositeSide();
      }

      forwardFootstepList.setDefaultSwingDuration(swingTime.getDoubleValue());
      forwardFootstepList.setDefaultTransferDuration(transferTime.getDoubleValue());
   }

   private void computeBackwardFootstepList()
   {
      backwardFootstepList.clear();

      ArrayList<FootstepDataMessage> footstepDataList = new ArrayList<>();
      footstepDataList.addAll(forwardFootstepList.getDataList());
      footstepDataList.remove(footstepDataList.size() - 1);

      Collections.reverse(footstepDataList);

      RobotSide initialStanceSide = initialSwingSide.getEnumValue().getOppositeSide();
      FootstepDataMessage initialStanceFoot = constructFootstepDataMessage(soleFrames.get(initialStanceSide), 0.0, 0.0,
                                                                           initialStanceSide);
      footstepDataList.add(initialStanceFoot);
      footstepDataList.forEach(backwardFootstepList::add);

      backwardFootstepList.setDefaultSwingDuration(swingTime.getDoubleValue());
      backwardFootstepList.setDefaultTransferDuration(transferTime.getDoubleValue());
   }

   private static FootstepDataMessage constructFootstepDataMessage(ReferenceFrame frame, double xOffset, double yOffset, RobotSide side)
   {
      FootstepDataMessage footstepDataMessage = new FootstepDataMessage();

      FramePose footstepPose = new FramePose();
      footstepPose.setToZero(frame);
      footstepPose.setPosition(xOffset, yOffset, 0.0);
      footstepPose.changeFrame(ReferenceFrame.getWorldFrame());

      footstepDataMessage.setLocation(footstepPose.getPosition());
      footstepDataMessage.setOrientation(footstepPose.getOrientation());
      footstepDataMessage.setRobotSide(side);

      return footstepDataMessage;
   }

   @Override
   public void doControl()
   {
      if(isDone())
      {
         return;
      }

      if(stepsAlongCurrentList.get() == forwardFootstepList.getDataList().size())
      {
         if(walkingForward.getBooleanValue())
         {
            sendPacket(backwardFootstepList);
            walkingForward.set(false);
         }
         else
         {
            iterationCounter.increment();
            if(isDone())
            {
               return;
            }

            sendPacket(forwardFootstepList);
            walkingForward.set(true);
         }

         stepsAlongCurrentList.set(0);
      }
   }

   @Override
   public void onBehaviorAborted()
   {
   }

   @Override
   public void onBehaviorPaused()
   {
   }

   @Override
   public void onBehaviorResumed()
   {
   }

   @Override
   public void onBehaviorExited()
   {
   }

   @Override
   public boolean isDone()
   {
      return iterationCounter.getIntegerValue() == iterations.getIntegerValue();
   }
}
