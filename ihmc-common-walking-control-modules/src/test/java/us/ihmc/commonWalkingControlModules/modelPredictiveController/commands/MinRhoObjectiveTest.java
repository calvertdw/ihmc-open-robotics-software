package us.ihmc.commonWalkingControlModules.modelPredictiveController.commands;

import org.ejml.EjmlUnitTests;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ConstraintType;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.core.*;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.ioHandling.MPCContactPlane;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.ZeroConeRotationCalculator;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.matrixlib.NativeMatrix;
import us.ihmc.yoVariables.registry.YoRegistry;

import java.lang.annotation.Native;

import static org.junit.jupiter.api.Assertions.*;

public class MinRhoObjectiveTest
{
   @Test
   public void testCommandOptimizeAtTime()
   {
      double gravityZ = -9.81;
      double omega = 3.0;
      double mu = 0.8;
      double dt = 1e-3;

      ContactStateMagnitudeToForceMatrixHelper rhoHelper = new ContactStateMagnitudeToForceMatrixHelper(4, 4, new ZeroConeRotationCalculator());
      CoefficientJacobianMatrixHelper helper = new CoefficientJacobianMatrixHelper(4, 4);
      MPCContactPlane contactPlaneHelper = new MPCContactPlane(4, 4, new ZeroConeRotationCalculator());

      LinearMPCIndexHandler indexHandler = new LinearMPCIndexHandler(4);
      LinearMPCQPSolver solver = new LinearMPCQPSolver(indexHandler, dt, gravityZ, new YoRegistry("test"));

      FramePose3D contactPose = new FramePose3D();

      ConvexPolygon2DReadOnly contactPolygon = MPCTestHelper.createDefaultContact();

      rhoHelper.computeMatrices(contactPolygon, contactPose, 1e-8, 1e-10, mu);
      contactPlaneHelper.computeBasisVectors(contactPolygon, contactPose, mu);

      indexHandler.initialize(i -> contactPolygon.getNumberOfVertices(), 1);

      double timeOfConstraint = 0.7;
      double minRho = 0.001;

      RhoAccelerationObjectiveCommand command = new RhoAccelerationObjectiveCommand();

      command.setOmega(omega);
      command.setTimeOfObjective(timeOfConstraint);
      command.setSegmentNumber(0);
      command.setConstraintType(ConstraintType.GEQ_INEQUALITY);
      command.setScalarObjective(minRho);
      command.setUseScalarObjective(true);
      command.addContactPlaneHelper(contactPlaneHelper);

      double regularization = 1e-5;
      solver.initialize();
      solver.submitRhoValueCommand(command);
      solver.setComCoefficientRegularizationWeight(regularization);
      solver.setRhoCoefficientRegularizationWeight(regularization);

      solver.solve();

      NativeMatrix solvedObjectivePosition = new NativeMatrix(3, 1);

      NativeMatrix solution = solver.getSolution();
      DMatrixRMaj rhoSolution = new DMatrixRMaj(rhoHelper.getRhoSize() * 4, 1);

      NativeMatrix rhoValueVector = MPCTestHelper.computeRhoAccelerationVector(timeOfConstraint, omega, solution, contactPlaneHelper);

      MatrixTools.setMatrixBlock(rhoSolution, 0, 0, solution, 6, 0, rhoHelper.getRhoSize() * 4, 1, 1.0);

      helper.computeMatrices(timeOfConstraint, omega);

      solvedObjectivePosition.mult(MPCTestHelper.getContactValueJacobian(timeOfConstraint, omega, contactPlaneHelper), solution);

      DMatrixRMaj taskObjectiveExpected = new DMatrixRMaj(16, 1);
      CommonOps_DDRM.fill(taskObjectiveExpected, minRho);

      DMatrixRMaj taskJacobianExpected = new DMatrixRMaj(rhoHelper.getRhoSize(), indexHandler.getRhoCoefficientsInSegment(0));
      MatrixTools.setMatrixBlock(taskJacobianExpected, 0, 0, MPCTestHelper.getContactAccelerationJacobian(timeOfConstraint, omega, contactPlaneHelper), 0, 6, rhoHelper.getRhoSize(), indexHandler.getRhoCoefficientsInSegment(0), 1.0);

      EjmlUnitTests.assertEquals(taskJacobianExpected, solver.qpInputTypeA.taskJacobian, 1e-5);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, solver.qpInputTypeA.taskObjective, 1e-5);

      double omega2 = omega * omega;

      double a0 = omega2 * Math.exp(omega * timeOfConstraint);
      double a1 = omega2 * Math.exp(-omega * timeOfConstraint);
      double a2 = 6.0 * timeOfConstraint;
      double a3 = 2.0;

      DMatrixRMaj taskJacobianExpectedAlt = new DMatrixRMaj(rhoHelper.getRhoSize(), indexHandler.getTotalProblemSize());

      for (int rhoIdx  = 0; rhoIdx < rhoHelper.getRhoSize(); rhoIdx++)
      {
         int startColIdx = 6 + 4 * rhoIdx;
         double rhoValue = a0 * solution.get(startColIdx, 0);
         rhoValue += a1 * solution.get(startColIdx + 1, 0);
         rhoValue += a2 * solution.get(startColIdx + 2, 0);
         rhoValue += a3 * solution.get(startColIdx + 3, 0);

         assertTrue(rhoValue >= rhoValueVector.get(rhoIdx, 0));

         taskJacobianExpectedAlt.set(rhoIdx, startColIdx, a0);
         taskJacobianExpectedAlt.set(rhoIdx, startColIdx + 1, a1);
         taskJacobianExpectedAlt.set(rhoIdx, startColIdx + 2, a2);
         taskJacobianExpectedAlt.set(rhoIdx, startColIdx + 3, a3);
      }



      CommonOps_DDRM.scale(-1.0, taskJacobianExpectedAlt);
      CommonOps_DDRM.scale(-1.0, taskObjectiveExpected);
      EjmlUnitTests.assertEquals(taskJacobianExpectedAlt, solver.solverInput_Ain, 1e-5);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, solver.solverInput_bin, 1e-5);

      DMatrixRMaj solverInput_H_Expected = new DMatrixRMaj(taskJacobianExpectedAlt.getNumCols(), taskJacobianExpectedAlt.getNumCols());
      DMatrixRMaj solverInput_f_Expected = new DMatrixRMaj(taskJacobianExpectedAlt.getNumCols(), 1);

      MatrixTools.addDiagonal(solverInput_H_Expected, regularization);

      EjmlUnitTests.assertEquals(solverInput_H_Expected, solver.solverInput_H, 1e-10);
      EjmlUnitTests.assertEquals(solverInput_f_Expected, solver.solverInput_f, 1e-10);
   }

   @Test
   public void testCommandOptimizeBeginningAndEnd()
   {
      double gravityZ = -9.81;
      double omega = 3.0;
      double mu = 0.8;
      double dt = 1e-3;

      ContactStateMagnitudeToForceMatrixHelper rhoHelper = new ContactStateMagnitudeToForceMatrixHelper(4, 4, new ZeroConeRotationCalculator());
      CoefficientJacobianMatrixHelper helper = new CoefficientJacobianMatrixHelper(4, 4);
      MPCContactPlane contactPlaneHelper = new MPCContactPlane(4, 4, new ZeroConeRotationCalculator());

      LinearMPCIndexHandler indexHandler = new LinearMPCIndexHandler(4);
      LinearMPCQPSolver solver = new LinearMPCQPSolver(indexHandler, dt, gravityZ, new YoRegistry("test"));

      FramePose3D contactPose = new FramePose3D();

      ConvexPolygon2DReadOnly contactPolygon = MPCTestHelper.createDefaultContact();

      rhoHelper.computeMatrices(contactPolygon, contactPose, 1e-8, 1e-10, mu);
      contactPlaneHelper.computeBasisVectors(contactPolygon, contactPose, mu);

      indexHandler.initialize(i -> contactPolygon.getNumberOfVertices(), 1);

      double timeOfConstraint = 0.7;
      double minRho = 0.001;

      RhoAccelerationObjectiveCommand commandStart = new RhoAccelerationObjectiveCommand();

      commandStart.setOmega(omega);
      commandStart.setTimeOfObjective(0.0);
      commandStart.setSegmentNumber(0);
      commandStart.setConstraintType(ConstraintType.GEQ_INEQUALITY);
      commandStart.setScalarObjective(minRho);
      commandStart.setUseScalarObjective(true);
      commandStart.addContactPlaneHelper(contactPlaneHelper);

      RhoAccelerationObjectiveCommand commandEnd = new RhoAccelerationObjectiveCommand();
      commandEnd.setOmega(omega);
      commandEnd.setTimeOfObjective(timeOfConstraint);
      commandEnd.setSegmentNumber(0);
      commandEnd.setConstraintType(ConstraintType.GEQ_INEQUALITY);
      commandEnd.setScalarObjective(minRho);
      commandEnd.setUseScalarObjective(true);
      commandEnd.addContactPlaneHelper(contactPlaneHelper);

      double regularization = 1e-5;
      solver.initialize();
      solver.submitRhoValueCommand(commandStart);
      solver.submitRhoValueCommand(commandEnd);
      solver.setComCoefficientRegularizationWeight(regularization);
      solver.setRhoCoefficientRegularizationWeight(regularization);

      solver.solve();


      NativeMatrix solution = solver.getSolution();
      DMatrixRMaj rhoSolution = new DMatrixRMaj(rhoHelper.getRhoSize() * 4, 1);

      MatrixTools.setMatrixBlock(rhoSolution, 0, 0, solution, 6, 0, rhoHelper.getRhoSize() * 4, 1, 1.0);

      NativeMatrix rhoValueVectorStart = MPCTestHelper.computeRhoAccelerationVector(0.0, omega, solution, contactPlaneHelper);
      NativeMatrix rhoValueVectorEnd = MPCTestHelper.computeRhoAccelerationVector(timeOfConstraint, omega, solution, contactPlaneHelper);


      DMatrixRMaj taskObjectiveExpected = new DMatrixRMaj(2 * rhoHelper.getRhoSize(), 1);
      CommonOps_DDRM.fill(taskObjectiveExpected, minRho);

      DMatrixRMaj taskJacobianExpected = new DMatrixRMaj(2 * rhoHelper.getRhoSize(), indexHandler.getTotalProblemSize());

      MatrixTools.setMatrixBlock(taskJacobianExpected, 0, 0, MPCTestHelper.getContactAccelerationJacobian(0.0, omega, contactPlaneHelper), 0, 0, rhoHelper.getRhoSize(), indexHandler.getTotalProblemSize(), 1.0);
      MatrixTools.setMatrixBlock(taskJacobianExpected, rhoHelper.getRhoSize(), 0, MPCTestHelper.getContactAccelerationJacobian(timeOfConstraint, omega, contactPlaneHelper), 0, 0, rhoHelper.getRhoSize(), indexHandler.getTotalProblemSize(), 1.0);

      CommonOps_DDRM.scale(-1.0, taskJacobianExpected);
      CommonOps_DDRM.scale(-1.0, taskObjectiveExpected);
      EjmlUnitTests.assertEquals(taskJacobianExpected, solver.solverInput_Ain, 1e-5);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, solver.solverInput_bin, 1e-5);

      double omega2 = omega * omega;

      double a0Start = omega2;
      double a0End = omega2 * Math.exp(omega * timeOfConstraint);
      double a1Start = omega2;
      double a1End = omega2 * Math.exp(-omega * timeOfConstraint);
      double a2Start = 0.0;
      double a2End = 6.0 * timeOfConstraint;
      double a3Start = 2.0;
      double a3End = 2.0;

      for (int rhoIdx  = 0; rhoIdx < rhoHelper.getRhoSize(); rhoIdx++)
      {
         int startColIdx = 6 + 4 * rhoIdx;
         double rhoValueStart = a0Start * solution.get(startColIdx, 0);
         rhoValueStart += a1Start * solution.get(startColIdx + 1, 0);
         rhoValueStart += a2Start * solution.get(startColIdx + 2, 0);
         rhoValueStart += a3Start * solution.get(startColIdx + 3, 0);
         double rhoValueEnd = a0End * solution.get(startColIdx, 0);
         rhoValueEnd += a1End * solution.get(startColIdx + 1, 0);
         rhoValueEnd += a2End * solution.get(startColIdx + 2, 0);
         rhoValueEnd += a3End * solution.get(startColIdx + 3, 0);

         assertTrue(rhoValueStart >= rhoValueVectorStart.get(rhoIdx, 0));
         assertTrue(rhoValueEnd >= rhoValueVectorEnd.get(rhoIdx, 0));
      }
   }

   @Test
   public void testCommandOptimizeBeginningAndEnd2Segments()
   {
      double gravityZ = -9.81;
      double omega = 3.0;
      double mu = 0.8;
      double dt = 1e-3;

      ContactStateMagnitudeToForceMatrixHelper rhoHelper = new ContactStateMagnitudeToForceMatrixHelper(4, 4, new ZeroConeRotationCalculator());
      CoefficientJacobianMatrixHelper helper = new CoefficientJacobianMatrixHelper(4, 4);
      MPCContactPlane contactPlaneHelper = new MPCContactPlane(4, 4, new ZeroConeRotationCalculator());

      LinearMPCIndexHandler indexHandler = new LinearMPCIndexHandler(4);
      LinearMPCQPSolver solver = new LinearMPCQPSolver(indexHandler, dt, gravityZ, new YoRegistry("test"));

      FramePose3D contactPose = new FramePose3D();

      ConvexPolygon2DReadOnly contactPolygon = MPCTestHelper.createDefaultContact();

      rhoHelper.computeMatrices(contactPolygon, contactPose, 1e-8, 1e-10, mu);
      contactPlaneHelper.computeBasisVectors(contactPolygon, contactPose, mu);

      indexHandler.initialize(i -> contactPolygon.getNumberOfVertices(), 2);

      double timeOfConstraint = 0.7;
      double minRho = 0.001;

      RhoAccelerationObjectiveCommand commandStart1 = new RhoAccelerationObjectiveCommand();
      commandStart1.setOmega(omega);
      commandStart1.setTimeOfObjective(0.0);
      commandStart1.setSegmentNumber(0);
      commandStart1.setConstraintType(ConstraintType.GEQ_INEQUALITY);
      commandStart1.setScalarObjective(minRho);
      commandStart1.setUseScalarObjective(true);
      commandStart1.addContactPlaneHelper(contactPlaneHelper);

      RhoAccelerationObjectiveCommand commandEnd1 = new RhoAccelerationObjectiveCommand();
      commandEnd1.setOmega(omega);
      commandEnd1.setTimeOfObjective(timeOfConstraint);
      commandEnd1.setSegmentNumber(0);
      commandEnd1.setConstraintType(ConstraintType.GEQ_INEQUALITY);
      commandEnd1.setScalarObjective(minRho);
      commandEnd1.setUseScalarObjective(true);
      commandEnd1.addContactPlaneHelper(contactPlaneHelper);

      RhoAccelerationObjectiveCommand commandStart2 = new RhoAccelerationObjectiveCommand();
      commandStart2.setOmega(omega);
      commandStart2.setTimeOfObjective(0.0);
      commandStart2.setSegmentNumber(1);
      commandStart2.setConstraintType(ConstraintType.GEQ_INEQUALITY);
      commandStart2.setScalarObjective(minRho);
      commandStart2.setUseScalarObjective(true);
      commandStart2.addContactPlaneHelper(contactPlaneHelper);

      RhoAccelerationObjectiveCommand commandEnd2 = new RhoAccelerationObjectiveCommand();
      commandEnd2.setOmega(omega);
      commandEnd2.setTimeOfObjective(timeOfConstraint);
      commandEnd2.setSegmentNumber(1);
      commandEnd2.setConstraintType(ConstraintType.GEQ_INEQUALITY);
      commandEnd2.setScalarObjective(minRho);
      commandEnd2.setUseScalarObjective(true);
      commandEnd2.addContactPlaneHelper(contactPlaneHelper);

      double regularization = 1e-5;
      solver.initialize();
      solver.submitRhoValueCommand(commandStart1);
      solver.submitRhoValueCommand(commandEnd1);
      solver.submitRhoValueCommand(commandStart2);
      solver.submitRhoValueCommand(commandEnd2);
      solver.setComCoefficientRegularizationWeight(regularization);
      solver.setRhoCoefficientRegularizationWeight(regularization);

      solver.solve();

      DMatrixRMaj rhoValueVectorStart1 = new DMatrixRMaj(rhoHelper.getRhoSize(), 1);
      DMatrixRMaj rhoValueVectorStart2 = new DMatrixRMaj(rhoHelper.getRhoSize(), 1);
      DMatrixRMaj rhoValueVectorEnd1 = new DMatrixRMaj(rhoHelper.getRhoSize(), 1);
      DMatrixRMaj rhoValueVectorEnd2 = new DMatrixRMaj(rhoHelper.getRhoSize(), 1);

      NativeMatrix solution = solver.getSolution();
      DMatrixRMaj rhoSolution1 = new DMatrixRMaj(rhoHelper.getRhoSize() * 4, 1);
      DMatrixRMaj rhoSolution2 = new DMatrixRMaj(rhoHelper.getRhoSize() * 4, 1);

      MatrixTools.setMatrixBlock(rhoSolution1, 0, 0, solution, indexHandler.getRhoCoefficientStartIndex(0), 0, rhoHelper.getRhoSize() * 4, 1, 1.0);
      MatrixTools.setMatrixBlock(rhoSolution2, 0, 0, solution, indexHandler.getRhoCoefficientStartIndex(1), 0, rhoHelper.getRhoSize() * 4, 1, 1.0);

      helper.computeMatrices(0.0, omega);
      CommonOps_DDRM.mult(helper.getPositionJacobianMatrix(), rhoSolution1, rhoValueVectorStart1);
      CommonOps_DDRM.mult(helper.getPositionJacobianMatrix(), rhoSolution2, rhoValueVectorStart2);
      helper.computeMatrices(timeOfConstraint, omega);
      CommonOps_DDRM.mult(helper.getPositionJacobianMatrix(), rhoSolution1, rhoValueVectorEnd1);
      CommonOps_DDRM.mult(helper.getPositionJacobianMatrix(), rhoSolution2, rhoValueVectorEnd2);

      DMatrixRMaj taskObjectiveExpected = new DMatrixRMaj(4 * rhoHelper.getRhoSize(), 1);
      CommonOps_DDRM.fill(taskObjectiveExpected, minRho);

      DMatrixRMaj taskJacobianExpected = new DMatrixRMaj(4 * rhoHelper.getRhoSize(), indexHandler.getTotalProblemSize());

      double omega2 = omega * omega;

      double a0Start = omega2;
      double a0End = omega2 * Math.exp(omega * timeOfConstraint);
      double a1Start = omega2;
      double a1End = omega2 * Math.exp(-omega * timeOfConstraint);
      double a2Start = 0.0;
      double a2End = 6.0 * timeOfConstraint;
      double a3Start = 2.0;
      double a3End = 2.0;

      for (int rhoIdxStart1  = 0; rhoIdxStart1 < rhoHelper.getRhoSize(); rhoIdxStart1++)
      {
         int startColIdx1 = indexHandler.getRhoCoefficientStartIndex(0) + 4 * rhoIdxStart1;
         int startColIdx2 = indexHandler.getRhoCoefficientStartIndex(1) + 4 * rhoIdxStart1;

         double rhoValueStart1 = a0Start * solution.get(startColIdx1, 0);
         rhoValueStart1 += a1Start * solution.get(startColIdx1 + 1, 0);
         rhoValueStart1 += a2Start * solution.get(startColIdx1 + 2, 0);
         rhoValueStart1 += a3Start * solution.get(startColIdx1 + 3, 0);
         double rhoValueEnd1 = a0End * solution.get(startColIdx1, 0);
         rhoValueEnd1 += a1End * solution.get(startColIdx1 + 1, 0);
         rhoValueEnd1 += a2End * solution.get(startColIdx1 + 2, 0);
         rhoValueEnd1 += a3End * solution.get(startColIdx1 + 3, 0);
         double rhoValueStart2 = a0Start * solution.get(startColIdx2, 0);
         rhoValueStart2 += a1Start * solution.get(startColIdx2 + 1, 0);
         rhoValueStart2 += a2Start * solution.get(startColIdx2 + 2, 0);
         rhoValueStart2 += a3Start * solution.get(startColIdx2 + 3, 0);
         double rhoValueEnd2 = a0End * solution.get(startColIdx2, 0);
         rhoValueEnd2 += a1End * solution.get(startColIdx2 + 1, 0);
         rhoValueEnd2 += a2End * solution.get(startColIdx2 + 2, 0);
         rhoValueEnd2 += a3End * solution.get(startColIdx2 + 3, 0);

         int rhoIdxEnd1 = rhoIdxStart1 + rhoHelper.getRhoSize();
         int rhoIdxStart2 = rhoIdxEnd1 + rhoHelper.getRhoSize();
         int rhoIdxEnd2 = rhoIdxStart2 + rhoHelper.getRhoSize();

         assertTrue(rhoValueStart1 >= rhoValueVectorStart1.get(rhoIdxStart1));
         assertTrue(rhoValueEnd1 >= rhoValueVectorEnd1.get(rhoIdxStart1));
         assertTrue(rhoValueStart2 >= rhoValueVectorStart2.get(rhoIdxStart1));
         assertTrue(rhoValueEnd2 >= rhoValueVectorEnd2.get(rhoIdxStart1));

         taskJacobianExpected.set(rhoIdxStart1, startColIdx1, a0Start);
         taskJacobianExpected.set(rhoIdxStart1, startColIdx1 + 1, a1Start);
         taskJacobianExpected.set(rhoIdxStart1, startColIdx1 + 2, a2Start);
         taskJacobianExpected.set(rhoIdxStart1, startColIdx1 + 3, a3Start);

         taskJacobianExpected.set(rhoIdxStart2, startColIdx2, a0Start);
         taskJacobianExpected.set(rhoIdxStart2, startColIdx2 + 1, a1Start);
         taskJacobianExpected.set(rhoIdxStart2, startColIdx2 + 2, a2Start);
         taskJacobianExpected.set(rhoIdxStart2, startColIdx2 + 3, a3Start);

         taskJacobianExpected.set(rhoIdxEnd1, startColIdx1, a0End);
         taskJacobianExpected.set(rhoIdxEnd1, startColIdx1 + 1, a1End);
         taskJacobianExpected.set(rhoIdxEnd1, startColIdx1 + 2, a2End);
         taskJacobianExpected.set(rhoIdxEnd1, startColIdx1 + 3, a3End);

         taskJacobianExpected.set(rhoIdxEnd2, startColIdx2, a0End);
         taskJacobianExpected.set(rhoIdxEnd2, startColIdx2 + 1, a1End);
         taskJacobianExpected.set(rhoIdxEnd2, startColIdx2 + 2, a2End);
         taskJacobianExpected.set(rhoIdxEnd2, startColIdx2 + 3, a3End);
      }

      CommonOps_DDRM.scale(-1.0, taskJacobianExpected);
      CommonOps_DDRM.scale(-1.0, taskObjectiveExpected);
      EjmlUnitTests.assertEquals(taskJacobianExpected, solver.solverInput_Ain, 1e-5);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, solver.solverInput_bin, 1e-5);

      DMatrixRMaj solverInput_H_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), taskJacobianExpected.getNumCols());
      DMatrixRMaj solverInput_f_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), 1);

      MatrixTools.addDiagonal(solverInput_H_Expected, regularization);

      EjmlUnitTests.assertEquals(solverInput_H_Expected, solver.solverInput_H, 1e-10);
      EjmlUnitTests.assertEquals(solverInput_f_Expected, solver.solverInput_f, 1e-10);
   }
}
