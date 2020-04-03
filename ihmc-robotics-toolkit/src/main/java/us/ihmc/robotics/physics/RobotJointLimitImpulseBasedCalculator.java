package us.ihmc.robotics.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.mecano.algorithms.ForwardDynamicsCalculator;
import us.ihmc.mecano.algorithms.MultiBodyResponseCalculator;
import us.ihmc.mecano.algorithms.interfaces.RigidBodyTwistProvider;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointReadOnly;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;

public class RobotJointLimitImpulseBasedCalculator implements ImpulseBasedConstraintCalculator
{
   private static final int matrixInitialSize = 40;

   public enum ActiveLimit
   {
      LOWER(DoubleUnaryOperator.identity()), UPPER(value -> -value);

      private final DoubleUnaryOperator signOperator;

      ActiveLimit(DoubleUnaryOperator signOperator)
      {
         this.signOperator = signOperator;
      }

      double transform(double value)
      {
         return signOperator.applyAsDouble(value);
      }
   };

   private double springConstant = 5.0;
   private final List<OneDoFJointBasics> jointsAtLimit = new ArrayList<>();
   private final List<ActiveLimit> activeLimits = new ArrayList<>();

   private boolean isInitialized = false;
   private boolean isImpulseZero = false;
   private boolean isInertiaUpToDate = false;

   private JointStateProvider externalJointTwistModifier;

   private final DenseMatrix64F jointVelocityNoImpulse = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocityDueToOtherImpulse = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocity = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocityPrevious = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocityUpdate = new DenseMatrix64F(matrixInitialSize, 1);

   private final DenseMatrix64F solverInput_A = new DenseMatrix64F(matrixInitialSize, matrixInitialSize);
   private final DenseMatrix64F solverInput_b = new DenseMatrix64F(matrixInitialSize, 1);
   private final LinearComplementarityProblemSolver solver = new LinearComplementarityProblemSolver();

   private final DenseMatrix64F impulse = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F impulsePrevious = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F impulseUpdate = new DenseMatrix64F(matrixInitialSize, 1);

   private final RigidBodyBasics rootBody;
   private final double dt;
   private final ForwardDynamicsCalculator forwardDynamicsCalculator;

   private MultiBodyResponseCalculator responseCalculator;

   public RobotJointLimitImpulseBasedCalculator(RigidBodyBasics rootBody, double dt, ForwardDynamicsCalculator forwardDynamicsCalculator)
   {
      this.rootBody = rootBody;
      this.dt = dt;
      this.forwardDynamicsCalculator = forwardDynamicsCalculator;

      responseCalculator = new MultiBodyResponseCalculator(forwardDynamicsCalculator);
   }

   @Override
   public void reset()
   {
      isInitialized = false;
   }

   @Override
   public void initialize()
   {
      if (isInitialized)
         return;

      jointsAtLimit.clear();
      activeLimits.clear();

      for (JointBasics joint : rootBody.childrenSubtreeIterable())
      {
         if (joint instanceof OneDoFJointBasics)
         {
            OneDoFJointBasics oneDoFJoint = (OneDoFJointBasics) joint;
            ActiveLimit activeLimit = computeActiveLimit(oneDoFJoint);

            if (activeLimit != null)
            {
               jointsAtLimit.add(oneDoFJoint);
               activeLimits.add(activeLimit);
            }
         }
      }

      jointVelocityNoImpulse.reshape(jointsAtLimit.size(), 1);
      jointVelocityDueToOtherImpulse.reshape(jointsAtLimit.size(), 1);
      jointVelocity.reshape(jointsAtLimit.size(), 1);
      jointVelocityPrevious.reshape(jointsAtLimit.size(), 1);
      jointVelocityUpdate.reshape(jointsAtLimit.size(), 1);
      solverInput_A.reshape(jointsAtLimit.size(), jointsAtLimit.size());
      solverInput_b.reshape(jointsAtLimit.size(), 1);
      impulse.reshape(jointsAtLimit.size(), 1);
      impulsePrevious.reshape(jointsAtLimit.size(), 1);
      impulseUpdate.reshape(jointsAtLimit.size(), 1);

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         OneDoFJointBasics joint = jointsAtLimit.get(i);
         ActiveLimit activeLimit = activeLimits.get(i);

         double qd = joint.getQd() + dt * forwardDynamicsCalculator.getComputedJointAcceleration(joint).get(0);

         if (activeLimit == ActiveLimit.LOWER)
         {
            double distanceToLowerLimit = joint.getQ() - joint.getJointLimitLower();
            qd += springConstant * distanceToLowerLimit;
         }
         else
         {
            double distanceToUpperLimit = joint.getQ() - joint.getJointLimitUpper();
            qd += springConstant * distanceToUpperLimit;
         }
         jointVelocityNoImpulse.set(i, qd);
      }

      isInertiaUpToDate = false;
      isInitialized = true;
   }

   private ActiveLimit computeActiveLimit(OneDoFJointReadOnly joint)
   {
      double q = joint.getQ();
      double qd = joint.getQd();
      double qdd = forwardDynamicsCalculator.getComputedJointAcceleration(joint).get(0);
      double projected_q = q + dt * qd + 0.5 * dt * dt * qdd;
      if (projected_q <= joint.getJointLimitLower())
         return ActiveLimit.LOWER;
      else if (projected_q >= joint.getJointLimitUpper())
         return ActiveLimit.UPPER;
      else
         return null;
   }

   @Override
   public void updateImpulse(double alpha)
   {
      boolean isFirstUpdate = !isInitialized;
      initialize();

      if (jointsAtLimit.isEmpty())
      {
         isImpulseZero = true;
         return;
      }

      if (externalJointTwistModifier != null)
      {
         for (int i = 0; i < jointsAtLimit.size(); i++)
         {
            jointVelocityDueToOtherImpulse.set(i, externalJointTwistModifier.getJointState(jointsAtLimit.get(i)));
         }
         CommonOps.add(jointVelocityNoImpulse, jointVelocityDueToOtherImpulse, jointVelocity);
      }
      else
      {
         jointVelocityDueToOtherImpulse.zero();
         jointVelocity.set(jointVelocityNoImpulse);
      }

      if (isFirstUpdate)
      {
         jointVelocityPrevious.set(jointVelocity);
         jointVelocityUpdate.set(jointVelocity);
      }
      else
      {
         CommonOps.subtract(jointVelocity, jointVelocityPrevious, jointVelocityUpdate);
         jointVelocityPrevious.set(jointVelocity);
      }

      if (!isInertiaUpToDate)
      {
         responseCalculator.reset();
         for (int i = 0; i < jointsAtLimit.size(); i++)
         {
            OneDoFJointBasics joint = jointsAtLimit.get(i);
            ActiveLimit activeLimit = activeLimits.get(i);
            responseCalculator.applyJointImpulse(joint, 1.0);

            for (int j = i; j < jointsAtLimit.size(); j++)
            {
               OneDoFJointBasics otherJoint = jointsAtLimit.get(j);
               ActiveLimit otherActiveLimit = activeLimits.get(j);
               double a = responseCalculator.getJointTwistChange(otherJoint);
               /*
                * The LCP solver only handles positive impulse/velocity, so we mirror the problem for joints on the
                * upper limit which need negative impulse/velocity.
                */
               a = activeLimit.transform(otherActiveLimit.transform(a));
               solverInput_A.set(j, i, a);
               solverInput_A.set(i, j, a); // Using symmetry property
            }

            responseCalculator.reset();
         }
         isInertiaUpToDate = true;
      }

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         ActiveLimit activeLimit = activeLimits.get(i);
         solverInput_b.set(i, activeLimit.transform(jointVelocity.get(i)));
      }

      DenseMatrix64F solverOutput_f = solver.solve(solverInput_A, solverInput_b);

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         ActiveLimit activeLimit = activeLimits.get(i);
         impulse.set(i, activeLimit.transform(solverOutput_f.get(i)));
      }

      if (isFirstUpdate)
      {
         impulseUpdate.set(impulse);
      }
      else
      {
         CommonOps.add(1.0 - alpha, impulsePrevious, alpha, impulse, impulse);
      }

      isImpulseZero = NormOps.normP2(impulse) < 1.0e-12;

      if (isImpulseZero)
      {
         responseCalculator.reset();
      }
      else
      {
         for (int i = 0; i < jointsAtLimit.size(); i++)
         {
            double jointImpulse = impulse.get(i);
            if (!EuclidCoreTools.isZero(jointImpulse, 1.0e-12))
               responseCalculator.applyJointImpulse(jointsAtLimit.get(i), jointImpulse);
         }
      }

      impulsePrevious.set(impulse);
   }

   public void setSpringConstant(double springConstant)
   {
      this.springConstant = springConstant;
   }

   public List<OneDoFJointBasics> getJointsAtLimit()
   {
      return jointsAtLimit;
   }

   public List<ActiveLimit> getActiveLimits()
   {
      return activeLimits;
   }

   public DenseMatrix64F getImpulse()
   {
      return impulse;
   }

   @Override
   public double getImpulseUpdate()
   {
      return NormOps.normP2(impulseUpdate);
   }

   @Override
   public double getVelocityUpdate()
   {
      return NormOps.normP2(jointVelocityUpdate);
   }

   @Override
   public boolean isConstraintActive()
   {
      return !isImpulseZero;
   }

   @Override
   public double getDT()
   {
      return dt;
   }

   public ForwardDynamicsCalculator getForwardDynamicsCalculator()
   {
      return forwardDynamicsCalculator;
   }

   public DenseMatrix64F computeJointVelocityChange()
   {
      return isConstraintActive() ? responseCalculator.propagateImpulse() : null;
   }

   @Override
   public void setExternalTwistModifier(JointStateProvider externalJointTwistModifier)
   {
      this.externalJointTwistModifier = externalJointTwistModifier;
   }

   @Override
   public int getNumberOfRobotsInvolved()
   {
      return 1;
   }

   @Override
   public RigidBodyBasics getRootBody(int index)
   {
      return rootBody;
   }

   @Override
   public RigidBodyTwistProvider getRigidBodyTwistChangeProvider(int index)
   {
      return responseCalculator.getTwistChangeProvider();
   }

   @Override
   public JointStateProvider getJointTwistChangeProvider(int index)
   {
      return JointStateProvider.toJointTwistProvider(responseCalculator);
   }

   @Override
   public DenseMatrix64F getJointVelocityChange(int index)
   {
      return isConstraintActive() ? responseCalculator.propagateImpulse() : null;
   }
}
