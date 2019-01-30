package us.ihmc.footstepPlanning.graphSearch.graph;

import us.ihmc.robotics.geometry.AngleTools;

public class LatticeNode
{
   public static final double gridSizeXY = 0.05;
   public static final double gridSizeYaw = Math.PI / 18.0;

   private final int xIndex;
   private final int yIndex;
   private final int yawIndex;

   private final int hashCode;

   public LatticeNode(double x, double y, double yaw)
   {
      this.xIndex = (int) Math.round(x / gridSizeXY);
      this.yIndex = (int) Math.round(y / gridSizeXY);
      this.yawIndex = (int) Math.round(AngleTools.trimAngleMinusPiToPi(yaw) / gridSizeYaw);
      hashCode = computeHashCode(this);
   }

   public int getXIndex()
   {
      return xIndex;
   }

   public int getYIndex()
   {
      return yIndex;
   }

   public int getYawIndex()
   {
      return yawIndex;
   }

   private static int computeHashCode(LatticeNode cell)
   {
      int result = 1;
      int prime = 31;
      result += prime * cell.xIndex;
      result += prime * cell.yIndex;
      result += prime * cell.yawIndex;
      return result;
   }

   @Override
   public int hashCode()
   {
      return hashCode;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;
      else if (obj == this)
         return true;

      if (getClass() != obj.getClass())
         return false;
      LatticeNode other = (LatticeNode) obj;
      if (xIndex != other.xIndex)
         return false;
      if (yIndex != other.yIndex)
         return false;
      if (yawIndex != other.yawIndex)
         return false;
      return true;
   }

}
