package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "FootstepPostProcessingParametersPacket" defined in "FootstepPostProcessingParametersPacket_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from FootstepPostProcessingParametersPacket_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit FootstepPostProcessingParametersPacket_.idl instead.
*
*/
public class FootstepPostProcessingParametersPacketPubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.FootstepPostProcessingParametersPacket>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::FootstepPostProcessingParametersPacket_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data) throws java.io.IOException
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

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);



      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      cdr.write_type_4(data.getSequenceId());

      cdr.write_type_7(data.getSplitFractionProcessingEnabled());

      cdr.write_type_7(data.getSwingOverRegionsEnabled());

      cdr.write_type_6(data.getStepHeightForLargeStepDown());

      cdr.write_type_6(data.getLargestStepDownHeight());

      cdr.write_type_6(data.getTransferSplitFractionAtFullDepth());

      cdr.write_type_6(data.getTransferWeightDistributionAtFullDepth());

      cdr.write_type_6(data.getMinimumSwingFootClearance());

      cdr.write_type_4(data.getNumberOfChecksPerSwing());

      cdr.write_type_4(data.getMaximumNumberOfAdjustmentAttempts());

      cdr.write_type_6(data.getMaximumWaypointAdjustmentDistance());

      cdr.write_type_6(data.getIncrementalWaypointAdjustmentDistance());

   }

   public static void read(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      data.setSequenceId(cdr.read_type_4());
      	
      data.setSplitFractionProcessingEnabled(cdr.read_type_7());
      	
      data.setSwingOverRegionsEnabled(cdr.read_type_7());
      	
      data.setStepHeightForLargeStepDown(cdr.read_type_6());
      	
      data.setLargestStepDownHeight(cdr.read_type_6());
      	
      data.setTransferSplitFractionAtFullDepth(cdr.read_type_6());
      	
      data.setTransferWeightDistributionAtFullDepth(cdr.read_type_6());
      	
      data.setMinimumSwingFootClearance(cdr.read_type_6());
      	
      data.setNumberOfChecksPerSwing(cdr.read_type_4());
      	
      data.setMaximumNumberOfAdjustmentAttempts(cdr.read_type_4());
      	
      data.setMaximumWaypointAdjustmentDistance(cdr.read_type_6());
      	
      data.setIncrementalWaypointAdjustmentDistance(cdr.read_type_6());
      	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_4("sequence_id", data.getSequenceId());
      ser.write_type_7("split_fraction_processing_enabled", data.getSplitFractionProcessingEnabled());
      ser.write_type_7("swing_over_regions_enabled", data.getSwingOverRegionsEnabled());
      ser.write_type_6("step_height_for_large_step_down", data.getStepHeightForLargeStepDown());
      ser.write_type_6("largest_step_down_height", data.getLargestStepDownHeight());
      ser.write_type_6("transfer_split_fraction_at_full_depth", data.getTransferSplitFractionAtFullDepth());
      ser.write_type_6("transfer_weight_distribution_at_full_depth", data.getTransferWeightDistributionAtFullDepth());
      ser.write_type_6("minimum_swing_foot_clearance", data.getMinimumSwingFootClearance());
      ser.write_type_4("number_of_checks_per_swing", data.getNumberOfChecksPerSwing());
      ser.write_type_4("maximum_number_of_adjustment_attempts", data.getMaximumNumberOfAdjustmentAttempts());
      ser.write_type_6("maximum_waypoint_adjustment_distance", data.getMaximumWaypointAdjustmentDistance());
      ser.write_type_6("incremental_waypoint_adjustment_distance", data.getIncrementalWaypointAdjustmentDistance());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data)
   {
      data.setSequenceId(ser.read_type_4("sequence_id"));
      data.setSplitFractionProcessingEnabled(ser.read_type_7("split_fraction_processing_enabled"));
      data.setSwingOverRegionsEnabled(ser.read_type_7("swing_over_regions_enabled"));
      data.setStepHeightForLargeStepDown(ser.read_type_6("step_height_for_large_step_down"));
      data.setLargestStepDownHeight(ser.read_type_6("largest_step_down_height"));
      data.setTransferSplitFractionAtFullDepth(ser.read_type_6("transfer_split_fraction_at_full_depth"));
      data.setTransferWeightDistributionAtFullDepth(ser.read_type_6("transfer_weight_distribution_at_full_depth"));
      data.setMinimumSwingFootClearance(ser.read_type_6("minimum_swing_foot_clearance"));
      data.setNumberOfChecksPerSwing(ser.read_type_4("number_of_checks_per_swing"));
      data.setMaximumNumberOfAdjustmentAttempts(ser.read_type_4("maximum_number_of_adjustment_attempts"));
      data.setMaximumWaypointAdjustmentDistance(ser.read_type_6("maximum_waypoint_adjustment_distance"));
      data.setIncrementalWaypointAdjustmentDistance(ser.read_type_6("incremental_waypoint_adjustment_distance"));
   }

   public static void staticCopy(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket src, controller_msgs.msg.dds.FootstepPostProcessingParametersPacket dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.FootstepPostProcessingParametersPacket createData()
   {
      return new controller_msgs.msg.dds.FootstepPostProcessingParametersPacket();
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
   
   public void serialize(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.FootstepPostProcessingParametersPacket src, controller_msgs.msg.dds.FootstepPostProcessingParametersPacket dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public FootstepPostProcessingParametersPacketPubSubType newInstance()
   {
      return new FootstepPostProcessingParametersPacketPubSubType();
   }
}
