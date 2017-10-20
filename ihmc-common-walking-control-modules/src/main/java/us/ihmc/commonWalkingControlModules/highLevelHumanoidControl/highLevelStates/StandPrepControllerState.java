package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import us.ihmc.commonWalkingControlModules.configurations.HighLevelControllerParameters;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelController;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.math.trajectories.YoPolynomial;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.sensorProcessing.outputData.JointDesiredControlMode;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutput;
import us.ihmc.sensorProcessing.outputData.LowLevelOneDoFJointDesiredDataHolderReadOnly;
import us.ihmc.tools.lists.PairList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class StandPrepControllerState extends HighLevelControllerState
{
   private static final HighLevelController controllerState = HighLevelController.STAND_PREP_STATE;
   private static final double MINIMUM_TIME_DONE_WITH_STAND_PREP = 0.0;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final LowLevelOneDoFJointDesiredDataHolder lowLevelOneDoFJointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder();

   private final PairList<OneDoFJoint, TrajectoryData> jointsData = new PairList<>();

   private final YoDouble timeToPrepareForStanding = new YoDouble("timeToPrepareForStanding", registry);
   private final YoDouble minimumTimeDoneWithStandPrep = new YoDouble("minimumTimeDoneWithStandPrep", registry);

   public StandPrepControllerState(HighLevelHumanoidControllerToolbox controllerToolbox, HighLevelControllerParameters highLevelControllerParameters)
   {
      this(controllerToolbox, highLevelControllerParameters, MINIMUM_TIME_DONE_WITH_STAND_PREP);
   }

   public StandPrepControllerState(HighLevelHumanoidControllerToolbox controllerToolbox, HighLevelControllerParameters highLevelControllerParameters,
                                   double minimumTimeDoneWithStandPrep)
   {
      super(controllerState);

      this.timeToPrepareForStanding.set(highLevelControllerParameters.getTimeToMoveInStandPrep());
      this.minimumTimeDoneWithStandPrep.set(minimumTimeDoneWithStandPrep);

      WholeBodySetpointParameters standPrepParameters = highLevelControllerParameters.getStandPrepParameters();
      OneDoFJoint[] controlledJoints = controllerToolbox.getFullRobotModel().getOneDoFJoints();
      lowLevelOneDoFJointDesiredDataHolder.registerJointsWithEmptyData(controlledJoints);

      for (OneDoFJoint controlledJoint : controlledJoints)
      {
         String jointName = controlledJoint.getName();

         YoPolynomial trajectory = new YoPolynomial(jointName + "_StandPrepTrajectory", 4, registry);
         YoDouble standPrepFinalConfiguration = new YoDouble(jointName + "_StandPrepFinalConfiguration", registry);
         YoDouble standPrepDesiredConfiguration = new YoDouble(jointName + "_StandPrepDesiredConfiguration", registry);

         standPrepFinalConfiguration.set(standPrepParameters.getSetpoint(jointName));

         TrajectoryData jointData = new TrajectoryData(standPrepFinalConfiguration, standPrepDesiredConfiguration, trajectory);
         jointsData.add(controlledJoint, jointData);

         JointDesiredControlMode jointControlMode = highLevelControllerParameters.getJointDesiredControlMode(controlledJoint.getName(), controllerState);
         JointDesiredOutput jointDesiredOutput = lowLevelOneDoFJointDesiredDataHolder.getJointDesiredOutput(controlledJoint);
         jointDesiredOutput.setControlMode(jointControlMode);
         jointDesiredOutput.setStiffness(highLevelControllerParameters.getDesiredJointStiffness(controlledJoint.getName(), controllerState));
         jointDesiredOutput.setDamping(highLevelControllerParameters.getDesiredJointDamping(controlledJoint.getName(), controllerState));
      }

   }

   @Override
   public void doTransitionIntoAction()
   {
      for (int jointIndex = 0; jointIndex < jointsData.size(); jointIndex++)
      {
         OneDoFJoint joint = jointsData.get(jointIndex).getLeft();
         TrajectoryData trajectoryData = jointsData.get(jointIndex).getRight();
         YoDouble standPrepFinal = trajectoryData.getFinalJointConfiguration();
         YoPolynomial trajectory = trajectoryData.getJointTrajectory();

         double desiredFinalPosition = standPrepFinal.getDoubleValue();
         double desiredFinalVelocity = 0.0;

         double currentAngle = joint.getQ();
         double currentVelocity = 0.0;

         trajectory.setCubic(0.0, timeToPrepareForStanding.getDoubleValue(), currentAngle, currentVelocity, desiredFinalPosition, desiredFinalVelocity);
      }
   }

   @Override
   public void doAction()
   {
      double timeInTrajectory = MathTools.clamp(getTimeInCurrentState(), 0.0, timeToPrepareForStanding.getDoubleValue());

      for (int jointIndex = 0; jointIndex < jointsData.size(); jointIndex++)
      {
         OneDoFJoint joint = jointsData.get(jointIndex).getLeft();
         TrajectoryData trajectoryData = jointsData.get(jointIndex).getRight();

         YoPolynomial trajectory = trajectoryData.getJointTrajectory();
         YoDouble desiredPosition = trajectoryData.getDesiredJointConfiguration();

         trajectory.compute(timeInTrajectory);
         desiredPosition.set(trajectory.getPosition());

         JointDesiredOutput lowLevelJointData = lowLevelOneDoFJointDesiredDataHolder.getJointDesiredOutput(joint);
         lowLevelJointData.setDesiredPosition(desiredPosition.getDoubleValue());
         lowLevelJointData.setDesiredVelocity(trajectory.getVelocity());
         lowLevelJointData.setDesiredAcceleration(trajectory.getAcceleration());
      }
   }

   @Override
   public void doTransitionOutOfAction()
   {
      // Do nothing

   }

   @Override
   public boolean isDone()
   {
      return getTimeInCurrentState() > (timeToPrepareForStanding.getDoubleValue() + minimumTimeDoneWithStandPrep.getDoubleValue());
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public LowLevelOneDoFJointDesiredDataHolderReadOnly getOutputForLowLevelController()
   {
      return lowLevelOneDoFJointDesiredDataHolder;
   }

   @Override
   public void warmup(int iterations)
   {
   }

   private class TrajectoryData
   {
      private final YoDouble finalJointConfiguration;
      private final YoDouble desiredJointConfiguration;
      private final YoPolynomial jointTrajectory;

      public TrajectoryData(YoDouble finalJointConfiguration, YoDouble desiredJointConfiguration, YoPolynomial jointTrajectory)
      {
         this.finalJointConfiguration = finalJointConfiguration;
         this.desiredJointConfiguration = desiredJointConfiguration;
         this.jointTrajectory = jointTrajectory;
      }

      public YoDouble getFinalJointConfiguration()
      {
         return finalJointConfiguration;
      }

      public YoDouble getDesiredJointConfiguration()
      {
         return desiredJointConfiguration;
      }

      public YoPolynomial getJointTrajectory()
      {
         return jointTrajectory;
      }
   }

}
