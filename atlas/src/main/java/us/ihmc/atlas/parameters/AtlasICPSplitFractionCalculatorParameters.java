package us.ihmc.atlas.parameters;

import us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner.CoPGeneration.SplitFractionCalculatorParametersReadOnly;

public class AtlasICPSplitFractionCalculatorParameters implements SplitFractionCalculatorParametersReadOnly
{
   public boolean calculateSplitFractionsFromPositions()
   {
      return true;
   }

   public boolean calculateSplitFractionsFromArea()
   {
      return false;
   }

   /** {@inheritDoc} */
   public double getDefaultTransferSplitFraction()
   {
      return 0.5;
   }

   /** {@inheritDoc} */

   public double getStepHeightForLargeStepDown()
   {
      return 0.1;
   }

   /** {@inheritDoc} */
   public double getLargestStepDownHeight()
   {
      return 0.175;
   }

   /** {@inheritDoc} */
   public double getTransferSplitFractionAtFullDepth()
   {
      return 0.3;
   }

   /** {@inheritDoc} */
   public double getTransferWeightDistributionAtFullDepth()
   {
      return 0.75;
   }

   /** {@inheritDoc} */
   public double getTransferFinalWeightDistributionAtFullDepth()
   {
      return 0.8;
   }

   /** {@inheritDoc} */
   public double getFractionLoadIfFootHasFullSupport()
   {
      return 0.5;
   }

   /** {@inheritDoc} */
   public double getFractionTimeOnFootIfFootHasFullSupport()
   {
      return 0.5;
   }

   /** {@inheritDoc} */
   public double getFractionLoadIfOtherFootHasNoWidth()
   {
      return 0.5;
   }

   /** {@inheritDoc} */
   public double getFractionTimeOnFootIfOtherFootHasNoWidth()
   {
      return 0.5;
   }
}
