package us.ihmc.simulationToolkit.controllers;

import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointReadOnly;
import us.ihmc.robotics.controllers.PIDController;
import us.ihmc.robotics.controllers.pidGains.PIDGainsReadOnly;
import us.ihmc.robotics.controllers.pidGains.implementations.YoPIDGains;
import us.ihmc.scs2.definition.controller.interfaces.Controller;
import us.ihmc.scs2.definition.state.interfaces.OneDoFJointStateBasics;
import us.ihmc.sensorProcessing.outputData.LowLevelActuatorMode;
import us.ihmc.sensorProcessing.outputData.LowLevelStateReadOnly;
import us.ihmc.yoVariables.registry.YoRegistry;

public class LowLevelActuatorSimulator implements Controller
{
   private final String name = getClass().getSimpleName();
   private final YoRegistry registry;
   private final PIDController jointController;

   private final OneDoFJointReadOnly simulatedJoint;
   private final OneDoFJointStateBasics controllerOutput;
   private final LowLevelStateReadOnly actuatorDesireds;

   private LowLevelActuatorMode actuatorMode;

   private final YoPIDGains gains;
   private final double controlDT;

   public LowLevelActuatorSimulator(OneDoFJointReadOnly simulatedJoint,
                                    OneDoFJointStateBasics controllerOutput,
                                    LowLevelStateReadOnly jointDesiredOutput,
                                    double controlDT)
   {
      this(simulatedJoint, controllerOutput, jointDesiredOutput, controlDT, 0.0, 0.0, LowLevelActuatorMode.EFFORT);
   }

   public LowLevelActuatorSimulator(OneDoFJointReadOnly simulatedJoint,
                                    OneDoFJointStateBasics controllerOutput,
                                    LowLevelStateReadOnly jointDesiredOutput,
                                    double controlDT,
                                    LowLevelActuatorMode actuatorMode)
   {
      this(simulatedJoint, controllerOutput, jointDesiredOutput, controlDT, 0.0, 0.0, actuatorMode);
   }

   public LowLevelActuatorSimulator(OneDoFJointReadOnly simulatedJoint,
                                    OneDoFJointStateBasics controllerOutput,
                                    LowLevelStateReadOnly jointDesiredOutput,
                                    double controlDT,
                                    double kp,
                                    double kd,
                                    LowLevelActuatorMode actuatorMode)
   {
      this.controlDT = controlDT;
      this.actuatorMode = actuatorMode;
      registry = new YoRegistry(simulatedJoint.getName() + name);
      gains = new YoPIDGains(simulatedJoint.getName() + "Actuator", registry);
      jointController = new PIDController(gains, simulatedJoint.getName() + "LowLevelActuatorSimulator", registry);

      gains.setKp(kp);
      gains.setKd(kd);

      this.simulatedJoint = simulatedJoint;
      this.controllerOutput = controllerOutput;
      this.actuatorDesireds = jointDesiredOutput;
   }

   public void setKp(double kp)
   {
      gains.setKp(kp);
   }

   public void setKd(double kd)
   {
      gains.setKd(kd);
   }

   public void setGains(PIDGainsReadOnly gains)
   {
      this.gains.set(gains);
   }

   public void setActuatorMode(LowLevelActuatorMode actuatorMode)
   {
      this.actuatorMode = actuatorMode;
   }

   @Override
   public void doControl()
   {
      double outputEffort = 0.0;

      switch (actuatorMode)
      {
         case POSITION:
            if (!actuatorDesireds.isPositionValid())
               throw new RuntimeException("Joint " + simulatedJoint.getName() + " cannot be in position control mode without a valid position.");

            double currentPosition = simulatedJoint.getQ();
            double desiredPosition = actuatorDesireds.getPosition();

            double desiredRate = 0.0;
            if (actuatorDesireds.isVelocityValid())
               desiredRate = actuatorDesireds.getVelocity();
            double currentRate = simulatedJoint.getQd();

            outputEffort = jointController.computeForAngles(currentPosition, desiredPosition, currentRate, desiredRate, controlDT);

            break;
         case VELOCITY:
            if (!actuatorDesireds.isVelocityValid())
               throw new RuntimeException("Joint " + simulatedJoint.getName() + " cannot be in velocity control mode without a valid velocity.");
            currentRate = simulatedJoint.getQd();
            desiredRate = actuatorDesireds.getVelocity();

            outputEffort = jointController.compute(currentRate, desiredRate, 0.0, 0.0, controlDT);
            break;
         case EFFORT:
            currentPosition = simulatedJoint.getQ();
            desiredPosition = actuatorDesireds.isPositionValid() ? actuatorDesireds.getPosition() : currentPosition;

            currentRate = simulatedJoint.getQd();
            desiredRate = actuatorDesireds.isVelocityValid() ? actuatorDesireds.getVelocity() : 0.0;

            double feedbackEffort = jointController.compute(currentPosition, desiredPosition, currentRate, desiredRate, controlDT);

            outputEffort = feedbackEffort;
            if (actuatorDesireds.isEffortValid())
               outputEffort += actuatorDesireds.getEffort();
            break;
         case DISABLED:
            break;
         default:
            throw new RuntimeException("This hasn't been implemented yet.");
      }
      controllerOutput.setEffort(outputEffort);
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public YoRegistry getYoRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return name;
   }
}
