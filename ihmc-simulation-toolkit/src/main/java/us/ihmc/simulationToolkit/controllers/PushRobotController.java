package us.ihmc.simulationToolkit.controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JButton;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphic;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicVector;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.stateMachine.core.StateTransitionCondition;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.FloatingRootJointRobot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.RobotController;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;

public class PushRobotController implements RobotController
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoRegistry registry;
   private final YoDouble pushDuration;
   private final YoDouble pushForceMagnitude;
   private final YoFrameVector3D pushDirection;
   private final YoFrameVector3D pushForce;
   private final YoDouble pushTimeSwitch;
   private final YoInteger pushNumber;
   private final YoBoolean isBeingPushed;
   private final YoDouble pushDelay;

   private final YoDouble yoTime;

   private StateTransitionCondition pushCondition = null;
   private final ExternalForcePoint forcePoint;
   private final Vector3D forceVector = new Vector3D();

   private final YoGraphicVector forceVisualizer;

   private final LinkedList<DelayedPush> delayedPushs = new LinkedList<>();

   public PushRobotController(FloatingRootJointRobot pushableRobot, FullHumanoidRobotModel fullRobotModel)
   {
      this(pushableRobot, fullRobotModel.getChest().getParentJoint().getName(), new Vector3D(0, 0, 0.3), 0.005);
   }

   public PushRobotController(FloatingRootJointRobot pushableRobot, String jointNameToApplyForce, Vector3DReadOnly forcePointOffset)
   {
      this(pushableRobot, jointNameToApplyForce, forcePointOffset, 0.005);
   }

   public PushRobotController(FloatingRootJointRobot pushableRobot, String jointNameToApplyForce, Vector3DReadOnly forcePointOffset, double visualScale)
   {
      yoTime = pushableRobot.getYoTime();
      registry = new YoRegistry(jointNameToApplyForce + "_" + getClass().getSimpleName());
      forcePoint = new ExternalForcePoint(jointNameToApplyForce + "_externalForcePoint", forcePointOffset, pushableRobot);

      pushDuration = new YoDouble(jointNameToApplyForce + "_pushDuration", registry);
      pushForceMagnitude = new YoDouble(jointNameToApplyForce + "_pushMagnitude", registry);
      pushDirection = new YoFrameVector3D(jointNameToApplyForce + "_pushDirection", worldFrame, registry);
      pushForce = new YoFrameVector3D(jointNameToApplyForce + "_pushForce", worldFrame, registry);
      pushTimeSwitch = new YoDouble(jointNameToApplyForce + "_pushTimeSwitch", registry);
      pushNumber = new YoInteger(jointNameToApplyForce + "_pushNumber", registry);
      isBeingPushed = new YoBoolean(jointNameToApplyForce + "_isBeingPushed", registry);
      pushDelay = new YoDouble(jointNameToApplyForce + "_pushDelay", registry);

      pushableRobot.getJoint(jointNameToApplyForce).addExternalForcePoint(forcePoint);
      pushableRobot.setController(this);

      pushTimeSwitch.set(Double.NEGATIVE_INFINITY);
      pushForceMagnitude.set(0.0);

      forceVisualizer = new YoGraphicVector(jointNameToApplyForce
            + "_pushForce", forcePoint.getYoPosition(), forcePoint.getYoForce(), visualScale, YoAppearance.DarkBlue());
   }

   public YoGraphic getForceVisualizer()
   {
      return forceVisualizer;
   }

   public int getPushNumber()
   {
      return pushNumber.getIntegerValue();
   }

   public void setPushDuration(double duration)
   {
      pushDuration.set(duration);
   }

   public void setPushForceMagnitude(double magnitude)
   {
      pushForceMagnitude.set(magnitude);
   }

   public void setPushForceDirection(Vector3DReadOnly direction)
   {
      pushDirection.set(direction);
   }

   public void setPushDelay(double delay)
   {
      pushDelay.set(delay);
   }

   public void addPushButtonToSCS(final SimulationConstructionSet scs)
   {
      if (scs != null)
      {
         JButton button = new JButton("PushRobot");
         button.setToolTipText("Click to push the robot as defined in the variables 'pushDirection' and 'pushMagnitude'");

         ActionListener listener = new ActionListener()
         {
            @Override
            public void actionPerformed(ActionEvent e)
            {
               pushCondition = null;
               applyForce();
            }
         };

         button.addActionListener(listener);
         scs.addButton(button);
      }
   }

   public void applyForce(Vector3DReadOnly direction, double magnitude, double duration)
   {
      applyForceDelayed(null, 0.0, direction, magnitude, duration);
   }

   public void applyForceDelayed(StateTransitionCondition pushCondition, double timeDelay, Vector3DReadOnly direction, double magnitude, double duration)
   {
      this.pushCondition = pushCondition;
      setPushDuration(duration);
      setPushForceDirection(direction);
      setPushForceMagnitude(magnitude);
      setPushDelay(timeDelay);
      applyForce();
   }

   public void queueForceDelayed(StateTransitionCondition pushCondition, double timeDelay, Vector3DReadOnly direction, double magnitude, double duration)
   {
      if (delayedPushs.isEmpty())
         applyForceDelayed(pushCondition, timeDelay, direction, magnitude, duration);
      else
         delayedPushs.add(new DelayedPush(pushCondition, timeDelay, direction, magnitude, duration));
   }

   private void applyForce()
   {
      double length = pushDirection.length();
      if (length > 1e-5)
      {
         pushForce.set(pushDirection);
         pushForce.normalize();
         pushForce.scale(pushForceMagnitude.getDoubleValue());
         if (pushCondition == null)
         {
            pushTimeSwitch.set(yoTime.getDoubleValue());
         }
      }
      else
      {
         pushForce.setToZero();
         pushTimeSwitch.set(Double.NEGATIVE_INFINITY);
      }

      pushNumber.increment();
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public void doControl()
   {
      if (pushCondition != null)
      {
         if (pushCondition.testCondition(yoTime.getDoubleValue()))
         {
            pushTimeSwitch.set(yoTime.getDoubleValue() + pushDelay.getDoubleValue());
            pushCondition = null;
         }
      }

      if (yoTime.getDoubleValue() <= pushTimeSwitch.getDoubleValue() + pushDuration.getDoubleValue()
            && yoTime.getDoubleValue() >= pushTimeSwitch.getDoubleValue())
      {
         isBeingPushed.set(true);
         forceVector.set(pushForce);
         pushNumber.decrement();
      }
      else
      {
         if (isBeingPushed.getValue() && !delayedPushs.isEmpty())
         {
            DelayedPush delayedPush = delayedPushs.pollFirst();
            applyForceDelayed(delayedPush.pushCondition, delayedPush.timeDelay, delayedPush.direction, delayedPush.magnitude, delayedPush.duration);
         }

         isBeingPushed.set(false);
         forceVector.set(0.0, 0.0, 0.0);
      }

      forcePoint.setForce(forceVector);
   }

   @Override
   public YoRegistry getYoRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return registry.getName();
   }

   @Override
   public String getDescription()
   {
      return registry.getName();
   }

   private static class DelayedPush
   {
      private StateTransitionCondition pushCondition;
      private double timeDelay;
      private Vector3DReadOnly direction;
      private double magnitude;
      private double duration;

      public DelayedPush(StateTransitionCondition pushCondition, double timeDelay, Vector3DReadOnly direction, double magnitude, double duration)
      {
         this.pushCondition = pushCondition;
         this.timeDelay = timeDelay;
         this.direction = direction;
         this.magnitude = magnitude;
         this.duration = duration;
      }
   }
}