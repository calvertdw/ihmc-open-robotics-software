package us.ihmc.footstepPlanning.graphSearch.parameters;

public interface FootstepPlannerCostParameters
{
   /**
    * When using a cost based planning approach this value defined how the yaw of a footstep will be
    * weighted in comparison to its position.
    */
   default double getYawWeight()
   {
      return 0.1;
   }

   /**
    * <p>
    * This value defined how the forward (or backward) displacement of a footstep will be weighted in
    * comparison to its position.
    * </p>
    * <p>
    *    Note that when using a Euclidean distance, this weight is averaged with the value returned by
    *    {@link #getLateralWeight()}
    * </p>
    */
   default double getForwardWeight()
   {
      return 1.0;
   }

   /**
    * <p>
    * This value defined how the lateral displacement of a footstep will be weighted in comparison to
    * its position.
    * </p>
    * <p>
    *    Note that when using a Euclidean distance, this weight is averaged with the value returned by
    *    {@link #getForwardWeight()}
    * </p>
    */
   default double getLateralWeight()
   {
      return 1.0;
   }

   /**
    * When using a cost based planning approach this value defines the cost that is added for each step
    * taken. Setting this value to a high number will favor plans with less steps.
    */
   default double getCostPerStep()
   {
      return 0.15;
   }

   /**
    * When using a cost based planning approach this value defines how the height change when stepping
    * up will be weighted.
    */
   default double getStepUpWeight()
   {
      return 0.0;
   }

   /**
    * When using a cost based planning approach this value defines how the height change when stepping
    * down will be weighted.
    */
   default double getStepDownWeight()
   {
      return 0.0;
   }

   /**
    * When using a cost based planning approach this value defines how the roll will be weighted.
    */
   default double getRollWeight()
   {
      return 0.0;
   }

   /**
    * When using a cost based planning approach this value defines how the pitch will be weighted.
    */
   default double getPitchWeight()
   {
      return 0.0;
   }
}
