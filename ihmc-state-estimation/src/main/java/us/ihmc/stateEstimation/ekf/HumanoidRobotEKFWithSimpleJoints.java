package us.ihmc.stateEstimation.ekf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.mecano.multiBodySystem.OneDoFJoint;
import us.ihmc.mecano.multiBodySystem.interfaces.FloatingJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.tools.MultiBodySystemTools;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorRawOutputMapReadOnly;
import us.ihmc.stateEstimation.humanoid.StateEstimatorController;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class HumanoidRobotEKFWithSimpleJoints implements StateEstimatorController
{
   private final SensorOutputMapReadOnly processedSensorOutput;
   private final List<OneDoFJoint> simpleJoints;

   private final LeggedRobotEKF leggedRobotEKF;

   public HumanoidRobotEKFWithSimpleJoints(FullHumanoidRobotModel estimatorFullRobotModel, Collection<String> imuNames,
                                           SideDependentList<String> footForceSensorNames, SensorRawOutputMapReadOnly sensorOutput, double dt, double gravity,
                                           SensorOutputMapReadOnly processedSensorOutput, YoGraphicsListRegistry graphicsListRegistry)
   {
      this.processedSensorOutput = processedSensorOutput;

      JointBasics[] chestSubtreeJoints = MultiBodySystemTools.collectSubtreeJoints(estimatorFullRobotModel.getChest());
      simpleJoints = Arrays.asList(MultiBodySystemTools.filterJoints(chestSubtreeJoints, OneDoFJoint.class));
      if (simpleJoints.size() != chestSubtreeJoints.length)
      {
         throw new RuntimeException("Can only handle OneDoFJoints in a robot.");
      }

      List<OneDoFJointBasics> jointsForEKF = new ArrayList<>();
      for (OneDoFJointBasics oneDoFJoint : estimatorFullRobotModel.getOneDoFJoints())
      {
         if (!simpleJoints.contains(oneDoFJoint))
         {
            jointsForEKF.add(oneDoFJoint);
         }
      }

      Map<String, ReferenceFrame> forceSensorMap = new HashMap<>();
      SideDependentList<MovingReferenceFrame> soleFrames = estimatorFullRobotModel.getSoleFrames();
      for (RobotSide robotSide : RobotSide.values)
      {
         forceSensorMap.put(footForceSensorNames.get(robotSide), soleFrames.get(robotSide));
      }

      FloatingJointBasics rootJoint = estimatorFullRobotModel.getRootJoint();
      leggedRobotEKF = new LeggedRobotEKF(rootJoint, jointsForEKF, imuNames, forceSensorMap, sensorOutput, dt, gravity, graphicsListRegistry);
   }

   @Override
   public void doControl()
   {
      for (int jointIdx = 0; jointIdx < simpleJoints.size(); jointIdx++)
      {
         OneDoFJoint simpleJoint = simpleJoints.get(jointIdx);
         simpleJoint.setQ(processedSensorOutput.getJointPositionProcessedOutput(simpleJoint));
         simpleJoint.setQd(processedSensorOutput.getJointVelocityProcessedOutput(simpleJoint));
      }

      leggedRobotEKF.doControl();
   }

   @Override
   public void initialize()
   {
      leggedRobotEKF.initialize();
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return leggedRobotEKF.getYoVariableRegistry();
   }

   @Override
   public String getName()
   {
      return getClass().getSimpleName();
   }

   @Override
   public String getDescription()
   {
      return getName();
   }

   @Override
   public void initializeEstimator(RigidBodyTransform rootJointTransform)
   {
      leggedRobotEKF.initializeEstimator(rootJointTransform);
   }
}
