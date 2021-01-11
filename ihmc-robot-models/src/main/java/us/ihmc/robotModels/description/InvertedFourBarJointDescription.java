package us.ihmc.robotModels.description;

import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.robotDescription.LoopClosurePinConstraintDescription;
import us.ihmc.robotics.robotDescription.OneDoFJointDescription;
import us.ihmc.robotics.robotDescription.PinJointDescription;

public class InvertedFourBarJointDescription extends OneDoFJointDescription
{
   private PinJointDescription[] fourBarJoints;
   private LoopClosurePinConstraintDescription fourBarClosure;
   private int masterJointIndex;

   public InvertedFourBarJointDescription(String name)
   {
      super(name, new Vector3D(Double.NaN, Double.NaN, Double.NaN), new Vector3D(Double.NaN, Double.NaN, Double.NaN));
   }

   public InvertedFourBarJointDescription(InvertedFourBarJointDescription other)
   {
      super(other);
      if (other.fourBarJoints != null)
      {
         fourBarJoints = new PinJointDescription[other.fourBarJoints.length];
         for (int i = 0; i < fourBarJoints.length; i++)
         {
            if (other.fourBarJoints[i] != null)
               fourBarJoints[i] = new PinJointDescription(other.fourBarJoints[i]);
         }
      }

      if (other.fourBarClosure != null)
         fourBarClosure = new LoopClosurePinConstraintDescription(other.fourBarClosure);

      masterJointIndex = other.masterJointIndex;
   }

   public void setMasterJointIndex(int masterJointIndex)
   {
      this.masterJointIndex = masterJointIndex;
   }

   public void setFourBarJoints(PinJointDescription[] fourBarJoints)
   {
      this.fourBarJoints = fourBarJoints;
   }

   public void setFourBarClosure(LoopClosurePinConstraintDescription fourBarClosure)
   {
      this.fourBarClosure = fourBarClosure;
   }

   public int getMasterJointIndex()
   {
      return masterJointIndex;
   }

   public PinJointDescription[] getFourBarJoints()
   {
      return fourBarJoints;
   }

   public LoopClosurePinConstraintDescription getFourBarClosure()
   {
      return fourBarClosure;
   }

   @Override
   public InvertedFourBarJointDescription copy()
   {
      return new InvertedFourBarJointDescription(this);
   }
}
