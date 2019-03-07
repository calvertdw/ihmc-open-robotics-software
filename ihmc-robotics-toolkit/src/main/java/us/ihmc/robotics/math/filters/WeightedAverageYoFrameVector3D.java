package us.ihmc.robotics.math.filters;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.yoVariables.providers.BooleanProvider;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFrameVector3D;

import java.util.ArrayList;
import java.util.List;

public class WeightedAverageYoFrameVector3D extends YoFrameVector3D
{
   private final List<DoubleProvider> booleanWeights = new ArrayList<>();
   private final List<YoFrameVector3D> frameVectorsToAverage = new ArrayList<>();


   public WeightedAverageYoFrameVector3D(String name, ReferenceFrame referenceFrame, YoVariableRegistry registry)
   {
      super(name, referenceFrame, registry);
   }

   public void addFrameVector3DToAverage(DoubleProvider booleanWeight, YoFrameVector3D frameVectorToAverage)
   {
      booleanWeights.add(booleanWeight);
      frameVectorToAverage.checkReferenceFrameMatch(getReferenceFrame());
      frameVectorsToAverage.add(frameVectorToAverage);
   }

   public void update()
   {
      int numberOfBooleans = booleanWeights.size();

      setToZero();
      double totalWeight = 0.0;

      for (int i = 0; i < numberOfBooleans; i++)
      {
         double weight = booleanWeights.get(i).getValue();
         scaleAdd(weight, frameVectorsToAverage.get(i), this);
         totalWeight += weight;
      }

      if (totalWeight <= 0.0)
         throw new RuntimeException("Invalid weights in the weighted average variable.");

      scale(1.0 / totalWeight);
   }
}
