package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * This message is part of the IHMC multi-contact controller API.
       * Published by the IHMC controller, this message carries minimal information relative
       * to the balance status of the robot.
       * All the information here is expressed in the world frame.
       */
public class MultiContactBalanceStatus extends Packet<MultiContactBalanceStatus> implements Settable<MultiContactBalanceStatus>, EpsilonComparable<MultiContactBalanceStatus>
{
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long sequence_id_;
   /**
            * This is the measured position in world of the robot's capture point. Only x and y coordinates are relevant.
            */
   public us.ihmc.euclid.tuple3D.Point3D capture_point_2d_;
   /**
            * This is the measured position in world of the robot's center of mass.
            */
   public us.ihmc.euclid.tuple3D.Point3D center_of_mass_3d_;
   /**
            * List of the active contact points expressed in world frame. Only x and y coordinates are relevant.
            */
   public us.ihmc.idl.IDLSequence.Object<us.ihmc.euclid.tuple3D.Point3D>  support_polygon_;
   /**
            * List of the active contact points expressed in local body-fixed frame.
            */
   public us.ihmc.idl.IDLSequence.Object<us.ihmc.euclid.tuple3D.Point3D>  contact_points_in_body_;
   /**
            * List of the rigid-bodies in contact. This list's size and ordering corresponds to the support_polygon and contact_point_in_body lists.
            */
   public us.ihmc.idl.IDLSequence.Integer  support_rigid_body_ids_;

   public MultiContactBalanceStatus()
   {
      capture_point_2d_ = new us.ihmc.euclid.tuple3D.Point3D();
      center_of_mass_3d_ = new us.ihmc.euclid.tuple3D.Point3D();
      support_polygon_ = new us.ihmc.idl.IDLSequence.Object<us.ihmc.euclid.tuple3D.Point3D> (16, new geometry_msgs.msg.dds.PointPubSubType());
      contact_points_in_body_ = new us.ihmc.idl.IDLSequence.Object<us.ihmc.euclid.tuple3D.Point3D> (16, new geometry_msgs.msg.dds.PointPubSubType());
      support_rigid_body_ids_ = new us.ihmc.idl.IDLSequence.Integer (16, "type_2");


   }

   public MultiContactBalanceStatus(MultiContactBalanceStatus other)
   {
      this();
      set(other);
   }

   public void set(MultiContactBalanceStatus other)
   {
      sequence_id_ = other.sequence_id_;

      geometry_msgs.msg.dds.PointPubSubType.staticCopy(other.capture_point_2d_, capture_point_2d_);
      geometry_msgs.msg.dds.PointPubSubType.staticCopy(other.center_of_mass_3d_, center_of_mass_3d_);
      support_polygon_.set(other.support_polygon_);
      contact_points_in_body_.set(other.contact_points_in_body_);
      support_rigid_body_ids_.set(other.support_rigid_body_ids_);
   }

   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public void setSequenceId(long sequence_id)
   {
      sequence_id_ = sequence_id;
   }
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long getSequenceId()
   {
      return sequence_id_;
   }


   /**
            * This is the measured position in world of the robot's capture point. Only x and y coordinates are relevant.
            */
   public us.ihmc.euclid.tuple3D.Point3D getCapturePoint2d()
   {
      return capture_point_2d_;
   }


   /**
            * This is the measured position in world of the robot's center of mass.
            */
   public us.ihmc.euclid.tuple3D.Point3D getCenterOfMass3d()
   {
      return center_of_mass_3d_;
   }


   /**
            * List of the active contact points expressed in world frame. Only x and y coordinates are relevant.
            */
   public us.ihmc.idl.IDLSequence.Object<us.ihmc.euclid.tuple3D.Point3D>  getSupportPolygon()
   {
      return support_polygon_;
   }


   /**
            * List of the active contact points expressed in local body-fixed frame.
            */
   public us.ihmc.idl.IDLSequence.Object<us.ihmc.euclid.tuple3D.Point3D>  getContactPointsInBody()
   {
      return contact_points_in_body_;
   }


   /**
            * List of the rigid-bodies in contact. This list's size and ordering corresponds to the support_polygon and contact_point_in_body lists.
            */
   public us.ihmc.idl.IDLSequence.Integer  getSupportRigidBodyIds()
   {
      return support_rigid_body_ids_;
   }


   public static Supplier<MultiContactBalanceStatusPubSubType> getPubSubType()
   {
      return MultiContactBalanceStatusPubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return MultiContactBalanceStatusPubSubType::new;
   }

   @Override
   public boolean epsilonEquals(MultiContactBalanceStatus other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (!this.capture_point_2d_.epsilonEquals(other.capture_point_2d_, epsilon)) return false;
      if (!this.center_of_mass_3d_.epsilonEquals(other.center_of_mass_3d_, epsilon)) return false;
      if (this.support_polygon_.size() != other.support_polygon_.size()) { return false; }
      else
      {
         for (int i = 0; i < this.support_polygon_.size(); i++)
         {  if (!this.support_polygon_.get(i).epsilonEquals(other.support_polygon_.get(i), epsilon)) return false; }
      }

      if (this.contact_points_in_body_.size() != other.contact_points_in_body_.size()) { return false; }
      else
      {
         for (int i = 0; i < this.contact_points_in_body_.size(); i++)
         {  if (!this.contact_points_in_body_.get(i).epsilonEquals(other.contact_points_in_body_.get(i), epsilon)) return false; }
      }

      if (!us.ihmc.idl.IDLTools.epsilonEqualsIntegerSequence(this.support_rigid_body_ids_, other.support_rigid_body_ids_, epsilon)) return false;


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof MultiContactBalanceStatus)) return false;

      MultiContactBalanceStatus otherMyClass = (MultiContactBalanceStatus) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if (!this.capture_point_2d_.equals(otherMyClass.capture_point_2d_)) return false;
      if (!this.center_of_mass_3d_.equals(otherMyClass.center_of_mass_3d_)) return false;
      if (!this.support_polygon_.equals(otherMyClass.support_polygon_)) return false;
      if (!this.contact_points_in_body_.equals(otherMyClass.contact_points_in_body_)) return false;
      if (!this.support_rigid_body_ids_.equals(otherMyClass.support_rigid_body_ids_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("MultiContactBalanceStatus {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("capture_point_2d=");
      builder.append(this.capture_point_2d_);      builder.append(", ");
      builder.append("center_of_mass_3d=");
      builder.append(this.center_of_mass_3d_);      builder.append(", ");
      builder.append("support_polygon=");
      builder.append(this.support_polygon_);      builder.append(", ");
      builder.append("contact_points_in_body=");
      builder.append(this.contact_points_in_body_);      builder.append(", ");
      builder.append("support_rigid_body_ids=");
      builder.append(this.support_rigid_body_ids_);
      builder.append("}");
      return builder.toString();
   }
}
