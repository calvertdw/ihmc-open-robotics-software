package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import java.util.List;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyInverseDynamicsSolver;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.WrenchMatrixCalculator;
import us.ihmc.mecano.algorithms.GeometricJacobianCalculator;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.robotics.contactable.ContactablePlaneBody;

/**
 * Equation of dynamics:
 * 
 * <pre>
 * &tau; = H qdd + C + &sum;<sub>i</sub> J<sup>T</sup><sub>i</sub> F<sub>ext,i</sub>
 * </pre>
 * 
 * where:
 * <ul>
 * <li><tt>qdd</tt> and <tt>&tau;</tt> are the vectors of the joint accelerations and torques.
 * <li><tt>H</tt> is the mass-matrix of the multi-body system.
 * <li><tt>C</tt> is the vector of gravity, centrifugal, and Coriolis forces.
 * <li><tt>J<sub>i</sub></tt> is the Jacobian for the end-effector <tt>i</tt> that is subject to an
 * external wrench.
 * <li><tt>F<sup>ext,i</tt> is the external wrench to the end-effector <tt>i</tt>.
 * </ul>
 * In the context of the {@link WholeBodyInverseDynamicsSolver}, some of the external wrenches
 * <tt>F<sup>ext</tt> are part of the optimization, these are formulated as:
 * 
 * <pre>
 * F<sub>ext</sub> = Q &rho;
 * </pre>
 * 
 * This calculator can be used to compute the matrix <tt>J<sup>T</sup> Q</tt> which maps from
 * contact magnitudes <tt>&rho;</tt>. This is necessary for including joint torque as an objective
 * in the optimization.
 */
public class ContactWrenchMatrixCalculator
{
   private final WrenchMatrixCalculator wrenchMatrixCalculator;
   private final List<? extends ContactablePlaneBody> contactablePlaneBodies;
   private final JointIndexHandler jointIndexHandler;
   private final GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();

   private final RigidBodyBasics rootBody;

   private final int numberOfDoFs;

   private final DenseMatrix64F tmpCompactContactForceJacobianMatrix;
   private final DenseMatrix64F tmpFullContactForceJacobianMatrix;

   public ContactWrenchMatrixCalculator(RigidBodyBasics rootBody, List<? extends ContactablePlaneBody> contactablePlaneBodies,
                                        WrenchMatrixCalculator wrenchMatrixCalculator, JointIndexHandler jointIndexHandler)
   {
      this.rootBody = rootBody;
      this.contactablePlaneBodies = contactablePlaneBodies;
      this.wrenchMatrixCalculator = wrenchMatrixCalculator;
      this.jointIndexHandler = jointIndexHandler;

      numberOfDoFs = jointIndexHandler.getNumberOfDoFs();
      int rhoSize = wrenchMatrixCalculator.getRhoSize();

      tmpFullContactForceJacobianMatrix = new DenseMatrix64F(rhoSize, numberOfDoFs);
      tmpCompactContactForceJacobianMatrix = new DenseMatrix64F(rhoSize, numberOfDoFs);
   }

   /**
    * Computes the Jacobian matrix <tt>Q<sup>T</sup>J</tt>.
    * 
    * @param contactForceJacobianToPack
    */
   public void computeContactForceJacobian(DenseMatrix64F contactForceJacobianToPack)
   {
      int contactForceStartIndex = 0;

      for (int bodyIndex = 0; bodyIndex < contactablePlaneBodies.size(); bodyIndex++)
      {
         RigidBodyBasics rigidBody = contactablePlaneBodies.get(bodyIndex).getRigidBody();
         jacobianCalculator.setKinematicChain(rootBody, rigidBody);
         jacobianCalculator.setJacobianFrame(wrenchMatrixCalculator.getJacobianFrame());
         DenseMatrix64F contactableBodyJacobianMatrix = jacobianCalculator.getJacobianMatrix();

         DenseMatrix64F rhoJacobianMatrix = wrenchMatrixCalculator.getRhoJacobianMatrix(rigidBody);

         int rhoSize = rhoJacobianMatrix.getNumCols();

         tmpCompactContactForceJacobianMatrix.reshape(rhoSize, contactableBodyJacobianMatrix.getNumCols());
         CommonOps.multTransA(rhoJacobianMatrix, contactableBodyJacobianMatrix, tmpCompactContactForceJacobianMatrix);
         CommonOps.changeSign(tmpCompactContactForceJacobianMatrix);

         jointIndexHandler.compactBlockToFullBlock(jacobianCalculator.getJointsFromBaseToEndEffector(),
                                                   tmpCompactContactForceJacobianMatrix,
                                                   tmpFullContactForceJacobianMatrix);
         CommonOps.extract(tmpFullContactForceJacobianMatrix, 0, rhoSize, 0, numberOfDoFs, contactForceJacobianToPack, contactForceStartIndex, 0);

         contactForceStartIndex += rhoSize;
      }
   }
}
