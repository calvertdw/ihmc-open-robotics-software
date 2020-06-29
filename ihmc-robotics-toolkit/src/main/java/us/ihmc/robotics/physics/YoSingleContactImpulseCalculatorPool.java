package us.ihmc.robotics.physics;

import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.commons.lists.SupplierBuilder;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.mecano.algorithms.ForwardDynamicsCalculator;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class YoSingleContactImpulseCalculatorPool
{
   private RecyclingArrayList<YoSingleContactImpulseCalculator> pool;

   public YoSingleContactImpulseCalculatorPool(int initialCapacity, String prefix, ReferenceFrame rootFrame, RigidBodyBasics rootBodyA,
                                               ForwardDynamicsCalculator forwardDynamicsCalculatorA, RigidBodyBasics rootBodyB,
                                               ForwardDynamicsCalculator forwardDynamicsCalculatorB, YoGraphicsListRegistry yoGraphicsListRegistry,
                                               YoVariableRegistry registry)
   {
      pool = new RecyclingArrayList<>(initialCapacity, SupplierBuilder.indexedSupplier(identifier ->
      {
         YoSingleContactImpulseCalculator calculator = new YoSingleContactImpulseCalculator(prefix,
                                                                                            identifier,
                                                                                            rootFrame,
                                                                                            rootBodyA,
                                                                                            forwardDynamicsCalculatorA,
                                                                                            rootBodyB,
                                                                                            forwardDynamicsCalculatorB,
                                                                                            registry);
         if (yoGraphicsListRegistry != null)
            calculator.setupGraphics(yoGraphicsListRegistry);
         return calculator;
      }));
   }

   public YoSingleContactImpulseCalculator nextAvailable()
   {
      return pool.add();
   }

   public void clear()
   {
      for (int i = 0; i < pool.size(); i++)
      {
         pool.get(i).clear();
      }
      pool.clear();
   }
}
