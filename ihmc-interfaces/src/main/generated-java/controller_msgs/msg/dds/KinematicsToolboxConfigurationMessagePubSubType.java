package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "KinematicsToolboxConfigurationMessage" defined in "KinematicsToolboxConfigurationMessage_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from KinematicsToolboxConfigurationMessage_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit KinematicsToolboxConfigurationMessage_.idl instead.
*
*/
public class KinematicsToolboxConfigurationMessagePubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::KinematicsToolboxConfigurationMessage_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data) throws java.io.IOException
   {
      deserializeCDR.deserialize(serializedPayload);
      read(data, deserializeCDR);
      deserializeCDR.finishDeserialize();
   }

   public static int getMaxCdrSerializedSize()
   {
      return getMaxCdrSerializedSize(0);
   }

   public static int getMaxCdrSerializedSize(int current_alignment)
   {
      int initial_alignment = current_alignment;


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data, int current_alignment)
   {
      int initial_alignment = current_alignment;


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);



      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);



      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);



      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);



      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);



      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);



      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);



      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);



      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);



      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data, us.ihmc.idl.CDR cdr)
   {

      cdr.write_type_4(data.getSequenceId());


      cdr.write_type_6(data.getJointVelocityWeight());


      cdr.write_type_6(data.getJointAccelerationWeight());


      cdr.write_type_7(data.getEnableJointVelocityLimits());


      cdr.write_type_7(data.getDisableJointVelocityLimits());


      cdr.write_type_7(data.getDisableCollisionAvoidance());


      cdr.write_type_7(data.getEnableCollisionAvoidance());


      cdr.write_type_7(data.getDisableInputPersistence());


      cdr.write_type_7(data.getEnableInputPersistence());

   }

   public static void read(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data, us.ihmc.idl.CDR cdr)
   {

      data.setSequenceId(cdr.read_type_4());
      	

      data.setJointVelocityWeight(cdr.read_type_6());
      	

      data.setJointAccelerationWeight(cdr.read_type_6());
      	

      data.setEnableJointVelocityLimits(cdr.read_type_7());
      	

      data.setDisableJointVelocityLimits(cdr.read_type_7());
      	

      data.setDisableCollisionAvoidance(cdr.read_type_7());
      	

      data.setEnableCollisionAvoidance(cdr.read_type_7());
      	

      data.setDisableInputPersistence(cdr.read_type_7());
      	

      data.setEnableInputPersistence(cdr.read_type_7());
      	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data, us.ihmc.idl.InterchangeSerializer ser)
   {

      ser.write_type_4("sequence_id", data.getSequenceId());

      ser.write_type_6("joint_velocity_weight", data.getJointVelocityWeight());

      ser.write_type_6("joint_acceleration_weight", data.getJointAccelerationWeight());

      ser.write_type_7("enable_joint_velocity_limits", data.getEnableJointVelocityLimits());

      ser.write_type_7("disable_joint_velocity_limits", data.getDisableJointVelocityLimits());

      ser.write_type_7("disable_collision_avoidance", data.getDisableCollisionAvoidance());

      ser.write_type_7("enable_collision_avoidance", data.getEnableCollisionAvoidance());

      ser.write_type_7("disable_input_persistence", data.getDisableInputPersistence());

      ser.write_type_7("enable_input_persistence", data.getEnableInputPersistence());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data)
   {

      data.setSequenceId(ser.read_type_4("sequence_id"));

      data.setJointVelocityWeight(ser.read_type_6("joint_velocity_weight"));

      data.setJointAccelerationWeight(ser.read_type_6("joint_acceleration_weight"));

      data.setEnableJointVelocityLimits(ser.read_type_7("enable_joint_velocity_limits"));

      data.setDisableJointVelocityLimits(ser.read_type_7("disable_joint_velocity_limits"));

      data.setDisableCollisionAvoidance(ser.read_type_7("disable_collision_avoidance"));

      data.setEnableCollisionAvoidance(ser.read_type_7("enable_collision_avoidance"));

      data.setDisableInputPersistence(ser.read_type_7("disable_input_persistence"));

      data.setEnableInputPersistence(ser.read_type_7("enable_input_persistence"));
   }

   public static void staticCopy(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage src, controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage createData()
   {
      return new controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage();
   }
   @Override
   public int getTypeSize()
   {
      return us.ihmc.idl.CDR.getTypeSize(getMaxCdrSerializedSize());
   }

   @Override
   public java.lang.String getName()
   {
      return name;
   }
   
   public void serialize(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage src, controller_msgs.msg.dds.KinematicsToolboxConfigurationMessage dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public KinematicsToolboxConfigurationMessagePubSubType newInstance()
   {
      return new KinematicsToolboxConfigurationMessagePubSubType();
   }
}
