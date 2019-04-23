package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "QuadrupedFootstepPlannerParametersPacket" defined in "QuadrupedFootstepPlannerParametersPacket_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from QuadrupedFootstepPlannerParametersPacket_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit QuadrupedFootstepPlannerParametersPacket_.idl instead.
*
*/
public class QuadrupedFootstepPlannerParametersPacketPubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::QuadrupedFootstepPlannerParametersPacket_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data) throws java.io.IOException
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

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);



      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      cdr.write_type_4(data.getSequenceId());

      cdr.write_type_6(data.getMaximumStepReach());

      cdr.write_type_6(data.getMaximumStepLength());

      cdr.write_type_6(data.getMinimumStepLength());

      cdr.write_type_6(data.getMaximumStepWidth());

      cdr.write_type_6(data.getMinimumStepWidth());

      cdr.write_type_6(data.getMinimumStepYaw());

      cdr.write_type_6(data.getMaximumStepYaw());

      cdr.write_type_6(data.getMaximumStepChangeZ());

      cdr.write_type_6(data.getBodyGroundClearance());

      cdr.write_type_6(data.getDistanceHeuristicWeight());

      cdr.write_type_6(data.getYawWeight());

      cdr.write_type_6(data.getXGaitWeight());

      cdr.write_type_6(data.getCostPerStep());

      cdr.write_type_6(data.getStepUpWeight());

      cdr.write_type_6(data.getStepDownWeight());

      cdr.write_type_6(data.getHeuristicsWeight());

      cdr.write_type_6(data.getMinXClearanceFromFoot());

      cdr.write_type_6(data.getMinYClearanceFromFoot());

      cdr.write_type_6(data.getCrawlSpeed());

      cdr.write_type_6(data.getTrotSpeed());

      cdr.write_type_6(data.getPaceSpeed());

      cdr.write_type_6(data.getProjectionInsideDistance());

      cdr.write_type_6(data.getMinimumSurfaceInclineRadians());

      cdr.write_type_6(data.getCliffHeightToAvoid());

      cdr.write_type_6(data.getMinimumDistanceFromCliffBottoms());

      cdr.write_type_6(data.getMinimumDistanceFromCliffTops());

   }

   public static void read(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      data.setSequenceId(cdr.read_type_4());
      	
      data.setMaximumStepReach(cdr.read_type_6());
      	
      data.setMaximumStepLength(cdr.read_type_6());
      	
      data.setMinimumStepLength(cdr.read_type_6());
      	
      data.setMaximumStepWidth(cdr.read_type_6());
      	
      data.setMinimumStepWidth(cdr.read_type_6());
      	
      data.setMinimumStepYaw(cdr.read_type_6());
      	
      data.setMaximumStepYaw(cdr.read_type_6());
      	
      data.setMaximumStepChangeZ(cdr.read_type_6());
      	
      data.setBodyGroundClearance(cdr.read_type_6());
      	
      data.setDistanceHeuristicWeight(cdr.read_type_6());
      	
      data.setYawWeight(cdr.read_type_6());
      	
      data.setXGaitWeight(cdr.read_type_6());
      	
      data.setCostPerStep(cdr.read_type_6());
      	
      data.setStepUpWeight(cdr.read_type_6());
      	
      data.setStepDownWeight(cdr.read_type_6());
      	
      data.setHeuristicsWeight(cdr.read_type_6());
      	
      data.setMinXClearanceFromFoot(cdr.read_type_6());
      	
      data.setMinYClearanceFromFoot(cdr.read_type_6());
      	
      data.setCrawlSpeed(cdr.read_type_6());
      	
      data.setTrotSpeed(cdr.read_type_6());
      	
      data.setPaceSpeed(cdr.read_type_6());
      	
      data.setProjectionInsideDistance(cdr.read_type_6());
      	
      data.setMinimumSurfaceInclineRadians(cdr.read_type_6());
      	
      data.setCliffHeightToAvoid(cdr.read_type_6());
      	
      data.setMinimumDistanceFromCliffBottoms(cdr.read_type_6());
      	
      data.setMinimumDistanceFromCliffTops(cdr.read_type_6());
      	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_4("sequence_id", data.getSequenceId());
      ser.write_type_6("maximum_step_reach", data.getMaximumStepReach());
      ser.write_type_6("maximum_step_length", data.getMaximumStepLength());
      ser.write_type_6("minimum_step_length", data.getMinimumStepLength());
      ser.write_type_6("maximum_step_width", data.getMaximumStepWidth());
      ser.write_type_6("minimum_step_width", data.getMinimumStepWidth());
      ser.write_type_6("minimum_step_yaw", data.getMinimumStepYaw());
      ser.write_type_6("maximum_step_yaw", data.getMaximumStepYaw());
      ser.write_type_6("maximum_step_change_z", data.getMaximumStepChangeZ());
      ser.write_type_6("body_ground_clearance", data.getBodyGroundClearance());
      ser.write_type_6("distance_heuristic_weight", data.getDistanceHeuristicWeight());
      ser.write_type_6("yaw_weight", data.getYawWeight());
      ser.write_type_6("x_gait_weight", data.getXGaitWeight());
      ser.write_type_6("cost_per_step", data.getCostPerStep());
      ser.write_type_6("step_up_weight", data.getStepUpWeight());
      ser.write_type_6("step_down_weight", data.getStepDownWeight());
      ser.write_type_6("heuristics_weight", data.getHeuristicsWeight());
      ser.write_type_6("min_x_clearance_from_foot", data.getMinXClearanceFromFoot());
      ser.write_type_6("min_y_clearance_from_foot", data.getMinYClearanceFromFoot());
      ser.write_type_6("crawl_speed", data.getCrawlSpeed());
      ser.write_type_6("trot_speed", data.getTrotSpeed());
      ser.write_type_6("pace_speed", data.getPaceSpeed());
      ser.write_type_6("projection_inside_distance", data.getProjectionInsideDistance());
      ser.write_type_6("minimum_surface_incline_radians", data.getMinimumSurfaceInclineRadians());
      ser.write_type_6("cliff_height_to_avoid", data.getCliffHeightToAvoid());
      ser.write_type_6("minimum_distance_from_cliff_bottoms", data.getMinimumDistanceFromCliffBottoms());
      ser.write_type_6("minimum_distance_from_cliff_tops", data.getMinimumDistanceFromCliffTops());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data)
   {
      data.setSequenceId(ser.read_type_4("sequence_id"));
      data.setMaximumStepReach(ser.read_type_6("maximum_step_reach"));
      data.setMaximumStepLength(ser.read_type_6("maximum_step_length"));
      data.setMinimumStepLength(ser.read_type_6("minimum_step_length"));
      data.setMaximumStepWidth(ser.read_type_6("maximum_step_width"));
      data.setMinimumStepWidth(ser.read_type_6("minimum_step_width"));
      data.setMinimumStepYaw(ser.read_type_6("minimum_step_yaw"));
      data.setMaximumStepYaw(ser.read_type_6("maximum_step_yaw"));
      data.setMaximumStepChangeZ(ser.read_type_6("maximum_step_change_z"));
      data.setBodyGroundClearance(ser.read_type_6("body_ground_clearance"));
      data.setDistanceHeuristicWeight(ser.read_type_6("distance_heuristic_weight"));
      data.setYawWeight(ser.read_type_6("yaw_weight"));
      data.setXGaitWeight(ser.read_type_6("x_gait_weight"));
      data.setCostPerStep(ser.read_type_6("cost_per_step"));
      data.setStepUpWeight(ser.read_type_6("step_up_weight"));
      data.setStepDownWeight(ser.read_type_6("step_down_weight"));
      data.setHeuristicsWeight(ser.read_type_6("heuristics_weight"));
      data.setMinXClearanceFromFoot(ser.read_type_6("min_x_clearance_from_foot"));
      data.setMinYClearanceFromFoot(ser.read_type_6("min_y_clearance_from_foot"));
      data.setCrawlSpeed(ser.read_type_6("crawl_speed"));
      data.setTrotSpeed(ser.read_type_6("trot_speed"));
      data.setPaceSpeed(ser.read_type_6("pace_speed"));
      data.setProjectionInsideDistance(ser.read_type_6("projection_inside_distance"));
      data.setMinimumSurfaceInclineRadians(ser.read_type_6("minimum_surface_incline_radians"));
      data.setCliffHeightToAvoid(ser.read_type_6("cliff_height_to_avoid"));
      data.setMinimumDistanceFromCliffBottoms(ser.read_type_6("minimum_distance_from_cliff_bottoms"));
      data.setMinimumDistanceFromCliffTops(ser.read_type_6("minimum_distance_from_cliff_tops"));
   }

   public static void staticCopy(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket src, controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket createData()
   {
      return new controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket();
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
   
   public void serialize(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket src, controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public QuadrupedFootstepPlannerParametersPacketPubSubType newInstance()
   {
      return new QuadrupedFootstepPlannerParametersPacketPubSubType();
   }
}
