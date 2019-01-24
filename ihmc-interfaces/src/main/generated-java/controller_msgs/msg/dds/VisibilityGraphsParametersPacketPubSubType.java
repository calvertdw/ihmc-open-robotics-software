package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "VisibilityGraphsParametersPacket" defined in "VisibilityGraphsParametersPacket_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from VisibilityGraphsParametersPacket_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit VisibilityGraphsParametersPacket_.idl instead.
*
*/
public class VisibilityGraphsParametersPacketPubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.VisibilityGraphsParametersPacket>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::VisibilityGraphsParametersPacket_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.VisibilityGraphsParametersPacket data) throws java.io.IOException
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

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);

      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data, int current_alignment)
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


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);


      current_alignment += 8 + us.ihmc.idl.CDR.alignment(current_alignment, 8);



      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      cdr.write_type_4(data.getSequenceId());

      cdr.write_type_6(data.getMaxInterRegionConnectionLength());

      cdr.write_type_6(data.getNormalZThresholdForAccessibleRegions());

      cdr.write_type_6(data.getPreferredExtrusionDistance());

      cdr.write_type_6(data.getExtrusionDistance());

      cdr.write_type_6(data.getExtrusionDistanceIfNotTooHighToStep());

      cdr.write_type_6(data.getTooHighToStepDistance());

      cdr.write_type_6(data.getClusterResolution());

      cdr.write_type_6(data.getExplorationDistanceFromStartGoal());

      cdr.write_type_6(data.getPlanarRegionMinArea());

      cdr.write_type_4(data.getPlanarRegionMinSize());

      cdr.write_type_6(data.getRegionOrthogonalAngle());

      cdr.write_type_6(data.getSearchHostRegionEpsilon());

   }

   public static void read(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      data.setSequenceId(cdr.read_type_4());
      	
      data.setMaxInterRegionConnectionLength(cdr.read_type_6());
      	
      data.setNormalZThresholdForAccessibleRegions(cdr.read_type_6());
      	
      data.setPreferredExtrusionDistance(cdr.read_type_6());
      	
      data.setExtrusionDistance(cdr.read_type_6());
      	
      data.setExtrusionDistanceIfNotTooHighToStep(cdr.read_type_6());
      	
      data.setTooHighToStepDistance(cdr.read_type_6());
      	
      data.setClusterResolution(cdr.read_type_6());
      	
      data.setExplorationDistanceFromStartGoal(cdr.read_type_6());
      	
      data.setPlanarRegionMinArea(cdr.read_type_6());
      	
      data.setPlanarRegionMinSize(cdr.read_type_4());
      	
      data.setRegionOrthogonalAngle(cdr.read_type_6());
      	
      data.setSearchHostRegionEpsilon(cdr.read_type_6());
      	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_4("sequence_id", data.getSequenceId());
      ser.write_type_6("max_inter_region_connection_length", data.getMaxInterRegionConnectionLength());
      ser.write_type_6("normal_z_threshold_for_accessible_regions", data.getNormalZThresholdForAccessibleRegions());
      ser.write_type_6("preferred_extrusion_distance", data.getPreferredExtrusionDistance());
      ser.write_type_6("extrusion_distance", data.getExtrusionDistance());
      ser.write_type_6("extrusion_distance_if_not_too_high_to_step", data.getExtrusionDistanceIfNotTooHighToStep());
      ser.write_type_6("too_high_to_step_distance", data.getTooHighToStepDistance());
      ser.write_type_6("cluster_resolution", data.getClusterResolution());
      ser.write_type_6("exploration_distance_from_start_goal", data.getExplorationDistanceFromStartGoal());
      ser.write_type_6("planar_region_min_area", data.getPlanarRegionMinArea());
      ser.write_type_4("planar_region_min_size", data.getPlanarRegionMinSize());
      ser.write_type_6("region_orthogonal_angle", data.getRegionOrthogonalAngle());
      ser.write_type_6("search_host_region_epsilon", data.getSearchHostRegionEpsilon());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.VisibilityGraphsParametersPacket data)
   {
      data.setSequenceId(ser.read_type_4("sequence_id"));
      data.setMaxInterRegionConnectionLength(ser.read_type_6("max_inter_region_connection_length"));
      data.setNormalZThresholdForAccessibleRegions(ser.read_type_6("normal_z_threshold_for_accessible_regions"));
      data.setPreferredExtrusionDistance(ser.read_type_6("preferred_extrusion_distance"));
      data.setExtrusionDistance(ser.read_type_6("extrusion_distance"));
      data.setExtrusionDistanceIfNotTooHighToStep(ser.read_type_6("extrusion_distance_if_not_too_high_to_step"));
      data.setTooHighToStepDistance(ser.read_type_6("too_high_to_step_distance"));
      data.setClusterResolution(ser.read_type_6("cluster_resolution"));
      data.setExplorationDistanceFromStartGoal(ser.read_type_6("exploration_distance_from_start_goal"));
      data.setPlanarRegionMinArea(ser.read_type_6("planar_region_min_area"));
      data.setPlanarRegionMinSize(ser.read_type_4("planar_region_min_size"));
      data.setRegionOrthogonalAngle(ser.read_type_6("region_orthogonal_angle"));
      data.setSearchHostRegionEpsilon(ser.read_type_6("search_host_region_epsilon"));
   }

   public static void staticCopy(controller_msgs.msg.dds.VisibilityGraphsParametersPacket src, controller_msgs.msg.dds.VisibilityGraphsParametersPacket dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.VisibilityGraphsParametersPacket createData()
   {
      return new controller_msgs.msg.dds.VisibilityGraphsParametersPacket();
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
   
   public void serialize(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.VisibilityGraphsParametersPacket data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.VisibilityGraphsParametersPacket src, controller_msgs.msg.dds.VisibilityGraphsParametersPacket dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public VisibilityGraphsParametersPacketPubSubType newInstance()
   {
      return new VisibilityGraphsParametersPacketPubSubType();
   }
}
