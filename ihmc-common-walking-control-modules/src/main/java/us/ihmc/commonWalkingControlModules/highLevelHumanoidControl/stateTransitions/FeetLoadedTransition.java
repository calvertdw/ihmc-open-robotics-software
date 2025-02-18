package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.stateTransitions;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.robotics.math.filters.SimpleMovingAverageFilteredYoVariable;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.sensors.ForceSensorDataHolderReadOnly;
import us.ihmc.robotics.sensors.ForceSensorDataReadOnly;
import us.ihmc.robotics.stateMachine.core.StateTransitionCondition;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class FeetLoadedTransition implements StateTransitionCondition
{
   protected final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private static final double MINIMUM_WEIGHT_FRACTION = 1.0 / 6.0;
   private static final double TIME_WINDOW = 3.0;

   private final SideDependentList<ForceSensorDataReadOnly> footSensors = new SideDependentList<>();

   private final YoBoolean areFeetLoaded;
   private final YoDouble weightPerFootForLoaded;

   private final SideDependentList<YoDouble> prepFootFzs = new SideDependentList<>();
   private final SideDependentList<SimpleMovingAverageFilteredYoVariable> prepFootFzAverages = new SideDependentList<>();

   public FeetLoadedTransition(ForceSensorDataHolderReadOnly forceSensorDataHolder,
                               SideDependentList<String> feetForceSensors,
                               double controlDT,
                               double gravityZ,
                               double totalMass,
                               YoRegistry parentRegistry)
   {
      for (RobotSide robotSide : RobotSide.values)
         footSensors.put(robotSide, forceSensorDataHolder.getData(feetForceSensors.get(robotSide)));

      int windowSize = (int) Math.floor(TIME_WINDOW / controlDT);

      areFeetLoaded = new YoBoolean("areFeetLoaded", registry);
      weightPerFootForLoaded = new YoDouble("weightPerFootForLoaded", registry);
      weightPerFootForLoaded.set(gravityZ * totalMass * MINIMUM_WEIGHT_FRACTION);

      for (RobotSide robotSide : RobotSide.values)
      {
         YoDouble prepFootFz = new YoDouble("prep" + robotSide.getCamelCaseName() + "FootFz", registry);
         SimpleMovingAverageFilteredYoVariable prepFootFzAverage = new SimpleMovingAverageFilteredYoVariable("prep" + robotSide.getCamelCaseName()
                                                                                                             + "FootFzAverage",
                                                                                                             windowSize,
                                                                                                             prepFootFz,
                                                                                                             registry);

         prepFootFzs.put(robotSide, prepFootFz);
         prepFootFzAverages.put(robotSide, prepFootFzAverage);
      }

      parentRegistry.addChild(registry);
   }

   private final Wrench temporaryFootWrench = new Wrench();

   private boolean areFeetLoaded()
   {
      double averageWeight = 0.0;
      for (RobotSide robotSide : RobotSide.values)
      {
         YoDouble prepFootFz = prepFootFzs.get(robotSide);
         SimpleMovingAverageFilteredYoVariable prepFootFzAverage = prepFootFzAverages.get(robotSide);

         temporaryFootWrench.setIncludingFrame(footSensors.get(robotSide).getWrench());
         temporaryFootWrench.changeFrame(ReferenceFrame.getWorldFrame());
         prepFootFz.set(temporaryFootWrench.getLinearPartZ());
         prepFootFzAverage.update();

         averageWeight += prepFootFzAverage.getDoubleValue();
      }
      areFeetLoaded.set((averageWeight > 2.0 * weightPerFootForLoaded.getDoubleValue()));

      return areFeetLoaded.getBooleanValue();
   }

   @Override
   public boolean testCondition(double timeInState)
   {
      return areFeetLoaded();
   }
}
