package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "KinematicsPlanningToolboxRigidBodyMessage" defined in "KinematicsPlanningToolboxRigidBodyMessage_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from KinematicsPlanningToolboxRigidBodyMessage_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit KinematicsPlanningToolboxRigidBodyMessage_.idl instead.
*
*/
public class KinematicsPlanningToolboxRigidBodyMessagePubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::KinematicsPlanningToolboxRigidBodyMessage_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data) throws java.io.IOException
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

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);current_alignment += (100 * 8) + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);for(int i0 = 0; i0 < 100; ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PosePubSubType.getMaxCdrSerializedSize(current_alignment);}
      current_alignment += controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += geometry_msgs.msg.dds.QuaternionPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);current_alignment += (100 * 8) + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);current_alignment += (100 * 8) + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      current_alignment += (data.getKeyFrameTimes().size() * 8) + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      for(int i0 = 0; i0 < data.getKeyFramePoses().size(); ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PosePubSubType.getCdrSerializedSize(data.getKeyFramePoses().get(i0), current_alignment);}

      current_alignment += controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.getCdrSerializedSize(data.getAngularSelectionMatrix(), current_alignment);

      current_alignment += controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.getCdrSerializedSize(data.getLinearSelectionMatrix(), current_alignment);

      current_alignment += controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.getCdrSerializedSize(data.getAngularWeightMatrix(), current_alignment);

      current_alignment += controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.getCdrSerializedSize(data.getLinearWeightMatrix(), current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getControlFramePositionInEndEffector(), current_alignment);

      current_alignment += geometry_msgs.msg.dds.QuaternionPubSubType.getCdrSerializedSize(data.getControlFrameOrientationInEndEffector(), current_alignment);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      current_alignment += (data.getAllowablePositionDisplacement().size() * 8) + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      current_alignment += (data.getAllowableOrientationDisplacement().size() * 8) + us.ihmc.idl.CDR.alignment(current_alignment, 8);



      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data, us.ihmc.idl.CDR cdr)
   {
      cdr.write_type_4(data.getSequenceId());

      cdr.write_type_2(data.getEndEffectorHashCode());

      if(data.getKeyFrameTimes().size() <= 100)
      cdr.write_type_e(data.getKeyFrameTimes());else
          throw new RuntimeException("key_frame_times field exceeds the maximum length");

      if(data.getKeyFramePoses().size() <= 100)
      cdr.write_type_e(data.getKeyFramePoses());else
          throw new RuntimeException("key_frame_poses field exceeds the maximum length");

      controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.write(data.getAngularSelectionMatrix(), cdr);
      controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.write(data.getLinearSelectionMatrix(), cdr);
      controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.write(data.getAngularWeightMatrix(), cdr);
      controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.write(data.getLinearWeightMatrix(), cdr);
      geometry_msgs.msg.dds.PointPubSubType.write(data.getControlFramePositionInEndEffector(), cdr);
      geometry_msgs.msg.dds.QuaternionPubSubType.write(data.getControlFrameOrientationInEndEffector(), cdr);
      if(data.getAllowablePositionDisplacement().size() <= 100)
      cdr.write_type_e(data.getAllowablePositionDisplacement());else
          throw new RuntimeException("allowable_position_displacement field exceeds the maximum length");

      if(data.getAllowableOrientationDisplacement().size() <= 100)
      cdr.write_type_e(data.getAllowableOrientationDisplacement());else
          throw new RuntimeException("allowable_orientation_displacement field exceeds the maximum length");

   }

   public static void read(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data, us.ihmc.idl.CDR cdr)
   {
      data.setSequenceId(cdr.read_type_4());
      	
      data.setEndEffectorHashCode(cdr.read_type_2());
      	
      cdr.read_type_e(data.getKeyFrameTimes());	
      cdr.read_type_e(data.getKeyFramePoses());	
      controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.read(data.getAngularSelectionMatrix(), cdr);	
      controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.read(data.getLinearSelectionMatrix(), cdr);	
      controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.read(data.getAngularWeightMatrix(), cdr);	
      controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.read(data.getLinearWeightMatrix(), cdr);	
      geometry_msgs.msg.dds.PointPubSubType.read(data.getControlFramePositionInEndEffector(), cdr);	
      geometry_msgs.msg.dds.QuaternionPubSubType.read(data.getControlFrameOrientationInEndEffector(), cdr);	
      cdr.read_type_e(data.getAllowablePositionDisplacement());	
      cdr.read_type_e(data.getAllowableOrientationDisplacement());	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_4("sequence_id", data.getSequenceId());
      ser.write_type_2("end_effector_hash_code", data.getEndEffectorHashCode());
      ser.write_type_e("key_frame_times", data.getKeyFrameTimes());
      ser.write_type_e("key_frame_poses", data.getKeyFramePoses());
      ser.write_type_a("angular_selection_matrix", new controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType(), data.getAngularSelectionMatrix());

      ser.write_type_a("linear_selection_matrix", new controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType(), data.getLinearSelectionMatrix());

      ser.write_type_a("angular_weight_matrix", new controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType(), data.getAngularWeightMatrix());

      ser.write_type_a("linear_weight_matrix", new controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType(), data.getLinearWeightMatrix());

      ser.write_type_a("control_frame_position_in_end_effector", new geometry_msgs.msg.dds.PointPubSubType(), data.getControlFramePositionInEndEffector());

      ser.write_type_a("control_frame_orientation_in_end_effector", new geometry_msgs.msg.dds.QuaternionPubSubType(), data.getControlFrameOrientationInEndEffector());

      ser.write_type_e("allowable_position_displacement", data.getAllowablePositionDisplacement());
      ser.write_type_e("allowable_orientation_displacement", data.getAllowableOrientationDisplacement());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data)
   {
      data.setSequenceId(ser.read_type_4("sequence_id"));
      data.setEndEffectorHashCode(ser.read_type_2("end_effector_hash_code"));
      ser.read_type_e("key_frame_times", data.getKeyFrameTimes());
      ser.read_type_e("key_frame_poses", data.getKeyFramePoses());
      ser.read_type_a("angular_selection_matrix", new controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType(), data.getAngularSelectionMatrix());

      ser.read_type_a("linear_selection_matrix", new controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType(), data.getLinearSelectionMatrix());

      ser.read_type_a("angular_weight_matrix", new controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType(), data.getAngularWeightMatrix());

      ser.read_type_a("linear_weight_matrix", new controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType(), data.getLinearWeightMatrix());

      ser.read_type_a("control_frame_position_in_end_effector", new geometry_msgs.msg.dds.PointPubSubType(), data.getControlFramePositionInEndEffector());

      ser.read_type_a("control_frame_orientation_in_end_effector", new geometry_msgs.msg.dds.QuaternionPubSubType(), data.getControlFrameOrientationInEndEffector());

      ser.read_type_e("allowable_position_displacement", data.getAllowablePositionDisplacement());
      ser.read_type_e("allowable_orientation_displacement", data.getAllowableOrientationDisplacement());
   }

   public static void staticCopy(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage src, controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage createData()
   {
      return new controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage();
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
   
   public void serialize(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage src, controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public KinematicsPlanningToolboxRigidBodyMessagePubSubType newInstance()
   {
      return new KinematicsPlanningToolboxRigidBodyMessagePubSubType();
   }
}
