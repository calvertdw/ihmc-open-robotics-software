package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "QuadrupedContinuousPlanningRequestPacket" defined in "QuadrupedContinuousPlanningRequestPacket_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from QuadrupedContinuousPlanningRequestPacket_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit QuadrupedContinuousPlanningRequestPacket_.idl instead.
*
*/
public class QuadrupedContinuousPlanningRequestPacketPubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::QuadrupedContinuousPlanningRequestPacket_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data) throws java.io.IOException
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

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += geometry_msgs.msg.dds.QuaternionPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getFrontLeftStartPositionInWorld(), current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getFrontRightStartPositionInWorld(), current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getHindLeftStartPositionInWorld(), current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getHindRightStartPositionInWorld(), current_alignment);

      current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getGoalPositionInWorld(), current_alignment);

      current_alignment += geometry_msgs.msg.dds.QuaternionPubSubType.getCdrSerializedSize(data.getGoalOrientationInWorld(), current_alignment);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);



      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data, us.ihmc.idl.CDR cdr)
   {
      cdr.write_type_4(data.getSequenceId());

      cdr.write_type_9(data.getStartTargetType());

      geometry_msgs.msg.dds.PointPubSubType.write(data.getFrontLeftStartPositionInWorld(), cdr);
      geometry_msgs.msg.dds.PointPubSubType.write(data.getFrontRightStartPositionInWorld(), cdr);
      geometry_msgs.msg.dds.PointPubSubType.write(data.getHindLeftStartPositionInWorld(), cdr);
      geometry_msgs.msg.dds.PointPubSubType.write(data.getHindRightStartPositionInWorld(), cdr);
      geometry_msgs.msg.dds.PointPubSubType.write(data.getGoalPositionInWorld(), cdr);
      geometry_msgs.msg.dds.QuaternionPubSubType.write(data.getGoalOrientationInWorld(), cdr);
      cdr.write_type_2(data.getPlannerRequestId());

      cdr.write_type_6(data.getTimeout());

      cdr.write_type_6(data.getHorizonLength());

   }

   public static void read(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data, us.ihmc.idl.CDR cdr)
   {
      data.setSequenceId(cdr.read_type_4());
      	
      data.setStartTargetType(cdr.read_type_9());
      	
      geometry_msgs.msg.dds.PointPubSubType.read(data.getFrontLeftStartPositionInWorld(), cdr);	
      geometry_msgs.msg.dds.PointPubSubType.read(data.getFrontRightStartPositionInWorld(), cdr);	
      geometry_msgs.msg.dds.PointPubSubType.read(data.getHindLeftStartPositionInWorld(), cdr);	
      geometry_msgs.msg.dds.PointPubSubType.read(data.getHindRightStartPositionInWorld(), cdr);	
      geometry_msgs.msg.dds.PointPubSubType.read(data.getGoalPositionInWorld(), cdr);	
      geometry_msgs.msg.dds.QuaternionPubSubType.read(data.getGoalOrientationInWorld(), cdr);	
      data.setPlannerRequestId(cdr.read_type_2());
      	
      data.setTimeout(cdr.read_type_6());
      	
      data.setHorizonLength(cdr.read_type_6());
      	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_4("sequence_id", data.getSequenceId());
      ser.write_type_9("start_target_type", data.getStartTargetType());
      ser.write_type_a("front_left_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getFrontLeftStartPositionInWorld());

      ser.write_type_a("front_right_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getFrontRightStartPositionInWorld());

      ser.write_type_a("hind_left_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getHindLeftStartPositionInWorld());

      ser.write_type_a("hind_right_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getHindRightStartPositionInWorld());

      ser.write_type_a("goal_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getGoalPositionInWorld());

      ser.write_type_a("goal_orientation_in_world", new geometry_msgs.msg.dds.QuaternionPubSubType(), data.getGoalOrientationInWorld());

      ser.write_type_2("planner_request_id", data.getPlannerRequestId());
      ser.write_type_6("timeout", data.getTimeout());
      ser.write_type_6("horizon_length", data.getHorizonLength());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data)
   {
      data.setSequenceId(ser.read_type_4("sequence_id"));
      data.setStartTargetType(ser.read_type_9("start_target_type"));
      ser.read_type_a("front_left_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getFrontLeftStartPositionInWorld());

      ser.read_type_a("front_right_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getFrontRightStartPositionInWorld());

      ser.read_type_a("hind_left_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getHindLeftStartPositionInWorld());

      ser.read_type_a("hind_right_start_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getHindRightStartPositionInWorld());

      ser.read_type_a("goal_position_in_world", new geometry_msgs.msg.dds.PointPubSubType(), data.getGoalPositionInWorld());

      ser.read_type_a("goal_orientation_in_world", new geometry_msgs.msg.dds.QuaternionPubSubType(), data.getGoalOrientationInWorld());

      data.setPlannerRequestId(ser.read_type_2("planner_request_id"));
      data.setTimeout(ser.read_type_6("timeout"));
      data.setHorizonLength(ser.read_type_6("horizon_length"));
   }

   public static void staticCopy(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket src, controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket createData()
   {
      return new controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket();
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
   
   public void serialize(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket src, controller_msgs.msg.dds.QuadrupedContinuousPlanningRequestPacket dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public QuadrupedContinuousPlanningRequestPacketPubSubType newInstance()
   {
      return new QuadrupedContinuousPlanningRequestPacketPubSubType();
   }
}
