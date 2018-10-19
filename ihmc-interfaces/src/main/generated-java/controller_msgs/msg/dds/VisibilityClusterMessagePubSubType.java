package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "VisibilityClusterMessage" defined in "VisibilityClusterMessage_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from VisibilityClusterMessage_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit VisibilityClusterMessage_.idl instead.
*
*/
public class VisibilityClusterMessagePubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.VisibilityClusterMessage>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::VisibilityClusterMessage_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.VisibilityClusterMessage data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.VisibilityClusterMessage data) throws java.io.IOException
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

      current_alignment += geometry_msgs.msg.dds.TransformPubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);for(int i0 = 0; i0 < 100; ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);}
      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);for(int i0 = 0; i0 < 100; ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);}
      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);for(int i0 = 0; i0 < 100; ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PointPubSubType.getMaxCdrSerializedSize(current_alignment);}

      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.VisibilityClusterMessage data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.VisibilityClusterMessage data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += geometry_msgs.msg.dds.TransformPubSubType.getCdrSerializedSize(data.getTransformToWorld(), current_alignment);

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      for(int i0 = 0; i0 < data.getRawPointsInLocal().size(); ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getRawPointsInLocal().get(i0), current_alignment);}

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      for(int i0 = 0; i0 < data.getNavigableExtrusionsInLocal().size(); ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getNavigableExtrusionsInLocal().get(i0), current_alignment);}

      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      for(int i0 = 0; i0 < data.getNonNavigableExtrusionsInLocal().size(); ++i0)
      {
          current_alignment += geometry_msgs.msg.dds.PointPubSubType.getCdrSerializedSize(data.getNonNavigableExtrusionsInLocal().get(i0), current_alignment);}


      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.VisibilityClusterMessage data, us.ihmc.idl.CDR cdr)
   {
      geometry_msgs.msg.dds.TransformPubSubType.write(data.getTransformToWorld(), cdr);
      if(data.getRawPointsInLocal().size() <= 100)
      cdr.write_type_e(data.getRawPointsInLocal());else
          throw new RuntimeException("raw_points_in_local field exceeds the maximum length");

      if(data.getNavigableExtrusionsInLocal().size() <= 100)
      cdr.write_type_e(data.getNavigableExtrusionsInLocal());else
          throw new RuntimeException("navigable_extrusions_in_local field exceeds the maximum length");

      if(data.getNonNavigableExtrusionsInLocal().size() <= 100)
      cdr.write_type_e(data.getNonNavigableExtrusionsInLocal());else
          throw new RuntimeException("non_navigable_extrusions_in_local field exceeds the maximum length");

   }

   public static void read(controller_msgs.msg.dds.VisibilityClusterMessage data, us.ihmc.idl.CDR cdr)
   {
      geometry_msgs.msg.dds.TransformPubSubType.read(data.getTransformToWorld(), cdr);	
      cdr.read_type_e(data.getRawPointsInLocal());	
      cdr.read_type_e(data.getNavigableExtrusionsInLocal());	
      cdr.read_type_e(data.getNonNavigableExtrusionsInLocal());	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.VisibilityClusterMessage data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_a("transform_to_world", new geometry_msgs.msg.dds.TransformPubSubType(), data.getTransformToWorld());

      ser.write_type_e("raw_points_in_local", data.getRawPointsInLocal());
      ser.write_type_e("navigable_extrusions_in_local", data.getNavigableExtrusionsInLocal());
      ser.write_type_e("non_navigable_extrusions_in_local", data.getNonNavigableExtrusionsInLocal());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.VisibilityClusterMessage data)
   {
      ser.read_type_a("transform_to_world", new geometry_msgs.msg.dds.TransformPubSubType(), data.getTransformToWorld());

      ser.read_type_e("raw_points_in_local", data.getRawPointsInLocal());
      ser.read_type_e("navigable_extrusions_in_local", data.getNavigableExtrusionsInLocal());
      ser.read_type_e("non_navigable_extrusions_in_local", data.getNonNavigableExtrusionsInLocal());
   }

   public static void staticCopy(controller_msgs.msg.dds.VisibilityClusterMessage src, controller_msgs.msg.dds.VisibilityClusterMessage dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.VisibilityClusterMessage createData()
   {
      return new controller_msgs.msg.dds.VisibilityClusterMessage();
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
   
   public void serialize(controller_msgs.msg.dds.VisibilityClusterMessage data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.VisibilityClusterMessage data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.VisibilityClusterMessage src, controller_msgs.msg.dds.VisibilityClusterMessage dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public VisibilityClusterMessagePubSubType newInstance()
   {
      return new VisibilityClusterMessagePubSubType();
   }
}
