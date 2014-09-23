package us.ihmc.steppr.hardware.state.slowSensors;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class MotorTemperature implements StepprSlowSensor
{
   private final DoubleYoVariable motorTemperature;
   
   public MotorTemperature(String name, YoVariableRegistry parentRegistry)
   {
      motorTemperature = new DoubleYoVariable(name + "MotorTemperature", parentRegistry);
   }

   @Override
   public void update(int value)
   {
      motorTemperature.set(value);
   }
   
}
