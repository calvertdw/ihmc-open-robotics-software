package us.ihmc.behaviors.sequence.actions;

import behavior_msgs.msg.dds.ArmJointAnglesActionDefinitionMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import us.ihmc.avatar.arm.PresetArmConfiguration;
import us.ihmc.behaviors.sequence.ActionNodeDefinition;
import us.ihmc.communication.crdt.CRDTInfo;
import us.ihmc.communication.crdt.CRDTUnidirectionalDouble;
import us.ihmc.communication.crdt.CRDTUnidirectionalDoubleArray;
import us.ihmc.communication.crdt.CRDTUnidirectionalEnumField;
import us.ihmc.communication.ros2.ROS2ActorDesignation;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.io.WorkspaceResourceDirectory;

import javax.annotation.Nullable;

public class ArmJointAnglesActionDefinition extends ActionNodeDefinition
{
   public static final int NUMBER_OF_JOINTS = 7;
   public static final String CUSTOM_ANGLES_NAME = "CUSTOM_ANGLES";

   /** Preset is null when using explicitly specified custom joint angles */
   private final CRDTUnidirectionalEnumField<PresetArmConfiguration> preset;
   private final CRDTUnidirectionalEnumField<RobotSide> side;
   private final CRDTUnidirectionalDouble trajectoryDuration;
   private final CRDTUnidirectionalDoubleArray jointAngles;

   public ArmJointAnglesActionDefinition(CRDTInfo crdtInfo, WorkspaceResourceDirectory saveFileDirectory)
   {
      super(crdtInfo, saveFileDirectory);

      preset = new CRDTUnidirectionalEnumField<>(ROS2ActorDesignation.OPERATOR, crdtInfo, PresetArmConfiguration.HOME);
      side = new CRDTUnidirectionalEnumField<>(ROS2ActorDesignation.OPERATOR, crdtInfo, RobotSide.LEFT);
      trajectoryDuration = new CRDTUnidirectionalDouble(ROS2ActorDesignation.OPERATOR, crdtInfo, 4.0);
      jointAngles = new CRDTUnidirectionalDoubleArray(ROS2ActorDesignation.OPERATOR, crdtInfo, NUMBER_OF_JOINTS);
   }

   @Override
   public void saveToFile(ObjectNode jsonNode)
   {
      super.saveToFile(jsonNode);

      jsonNode.put("preset", preset.getValue() == null ? CUSTOM_ANGLES_NAME : preset.getValue().name());
      jsonNode.put("side", side.getValue().getLowerCaseName());
      jsonNode.put("trajectoryDuration", trajectoryDuration.getValue());
      if (preset.getValue() == null)
      {
         for (int i = 0; i < NUMBER_OF_JOINTS; i++)
         {
            jsonNode.put("j" + i, jointAngles.getValueReadOnly(i));
         }
      }
   }

   @Override
   public void loadFromFile(JsonNode jsonNode)
   {
      super.loadFromFile(jsonNode);

      String presetName = jsonNode.get("preset").textValue();
      preset.setValue(presetName.equals(CUSTOM_ANGLES_NAME) ? null : PresetArmConfiguration.valueOf(presetName));
      side.setValue(RobotSide.getSideFromString(jsonNode.get("side").asText()));
      trajectoryDuration.setValue(jsonNode.get("trajectoryDuration").asDouble());
      if (preset.getValue() == null)
      {
         for (int i = 0; i < NUMBER_OF_JOINTS; i++)
         {
            jointAngles.getValue()[i] = jsonNode.get("j" + i).asDouble();
         }
      }
   }

   public void toMessage(ArmJointAnglesActionDefinitionMessage message)
   {
      super.toMessage(message.getDefinition());

      message.setPreset(preset == null ? -1 : preset.toMessage().ordinal());
      message.setRobotSide(side.toMessage().toByte());
      message.setTrajectoryDuration(trajectoryDuration.toMessage());
      jointAngles.toMessage(message.getJointAngles());
   }

   public void fromMessage(ArmJointAnglesActionDefinitionMessage message)
   {
      super.fromMessage(message.getDefinition());

      int presetOrdinal = message.getPreset();
      preset.fromMessage(presetOrdinal == -1 ? null : PresetArmConfiguration.values()[presetOrdinal]);
      side.fromMessage(RobotSide.fromByte(message.getRobotSide()));
      trajectoryDuration.fromMessage(message.getTrajectoryDuration());
      jointAngles.fromMessage(message.getJointAngles());
   }

   public CRDTUnidirectionalDoubleArray getJointAngles()
   {
      return jointAngles;
   }

   @Nullable
   public PresetArmConfiguration getPreset()
   {
      return preset.getValue();
   }

   public void setPreset(@Nullable PresetArmConfiguration preset)
   {
      this.preset.setValue(preset);
   }

   public double getTrajectoryDuration()
   {
      return trajectoryDuration.getValue();
   }

   public RobotSide getSide()
   {
      return side.getValue();
   }

   public void setTrajectoryDuration(double trajectoryDuration)
   {
      this.trajectoryDuration.setValue(trajectoryDuration);
   }

   public void setSide(RobotSide side)
   {
      this.side.setValue(side);
   }
}
