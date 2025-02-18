package us.ihmc.behaviors.behaviorTree;

import behavior_msgs.msg.dds.BehaviorTreeStateMessage;
import us.ihmc.behaviors.sequence.ActionSequenceDefinition;
import us.ihmc.behaviors.sequence.actions.*;

public class BehaviorTreeDefinitionRegistry
{
   private record RegistryRecord(Class<?> typeClass, byte messageByte) { }

   private static final RegistryRecord[] DEFINITIONS = new RegistryRecord[]
   {
      new RegistryRecord(BehaviorTreeNodeDefinition.class, BehaviorTreeStateMessage.BASIC_NODE),
      new RegistryRecord(ActionSequenceDefinition.class, BehaviorTreeStateMessage.ACTION_SEQUENCE),

      new RegistryRecord(ArmJointAnglesActionDefinition.class, BehaviorTreeStateMessage.ARM_JOINT_ANGLES_ACTION),
      new RegistryRecord(ChestOrientationActionDefinition.class, BehaviorTreeStateMessage.CHEST_ORIENTATION_ACTION),
      new RegistryRecord(FootstepPlanActionDefinition.class, BehaviorTreeStateMessage.FOOTSTEP_PLAN_ACTION),
      new RegistryRecord(HandPoseActionDefinition.class, BehaviorTreeStateMessage.HAND_POSE_ACTION),
      new RegistryRecord(HandWrenchActionDefinition.class, BehaviorTreeStateMessage.HAND_WRENCH_ACTION),
      new RegistryRecord(ScrewPrimitiveActionDefinition.class, BehaviorTreeStateMessage.SCREW_PRIMITIVE_ACTION),
      new RegistryRecord(PelvisHeightPitchActionDefinition.class, BehaviorTreeStateMessage.PELVIS_HEIGHT_PITCH_ACTION),
      new RegistryRecord(SakeHandCommandActionDefinition.class, BehaviorTreeStateMessage.SAKE_HAND_COMMAND_ACTION),
      new RegistryRecord(WaitDurationActionDefinition.class, BehaviorTreeStateMessage.WAIT_DURATION_ACTION),
   };

   public static Class<?> getClassFromTypeName(String typeName)
   {
      for (RegistryRecord definitionEntry : DEFINITIONS)
      {
         if (typeName.equals(definitionEntry.typeClass().getSimpleName()))
            return definitionEntry.typeClass();
      }

      return null;
   }

   public static Class<?> getNodeStateClass(byte nodeType)
   {
      for (RegistryRecord definitionEntry : DEFINITIONS)
      {
         if (nodeType == definitionEntry.messageByte())
            return definitionEntry.typeClass();
      }

      return null;
   }
}
