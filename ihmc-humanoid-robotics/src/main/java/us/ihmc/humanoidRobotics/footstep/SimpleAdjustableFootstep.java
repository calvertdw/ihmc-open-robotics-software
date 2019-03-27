package us.ihmc.humanoidRobotics.footstep;

public class SimpleAdjustableFootstep extends SimpleFootstep
{
   private boolean isAdjustable;

   public void setIsAdjustable(boolean isAdjustable)
   {
      this.isAdjustable = isAdjustable;
   }

   public boolean getIsAdjustable()
   {
      return isAdjustable;
   }

   public void set(SimpleAdjustableFootstep other)
   {
      isAdjustable = other.isAdjustable;
      super.set(other);
   }

   public void set(Footstep other)
   {
      setIsAdjustable(other.getIsAdjustable());
      setRobotSide(other.getRobotSide());
      setSoleFramePose(other.getFootstepPose());
      setFoothold(other.getPredictedContactPoints());
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
      {
         return true;
      }
      else if (obj instanceof SimpleAdjustableFootstep)
      {
         SimpleAdjustableFootstep other = (SimpleAdjustableFootstep) obj;
         if (isAdjustable ^ other.isAdjustable)
            return false;
         return super.equals(obj);
      }
      else
      {
         return false;
      }
   }
}
