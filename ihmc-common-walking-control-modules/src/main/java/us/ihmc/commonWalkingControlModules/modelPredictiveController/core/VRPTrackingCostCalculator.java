package us.ihmc.commonWalkingControlModules.modelPredictiveController.core;

import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.ioHandling.MPCContactPlane;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.ioHandling.MPCContactPoint;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.commands.VRPTrackingCommand;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to compute the cost functions for tracking a desired VRP function.
 * This tracking function can be computed as a convex quadratic cost term.
 *
 * TODO improve this cost calculator to allow tracking cubic VRP trajectories, rather than just linear ones.
 * TODO review class to make it more efficient for column-wise operations, such as what happens in the sparse matrices
 */
public class VRPTrackingCostCalculator
{
   private final LinearMPCIndexHandler indexHandler;
   private final double gravityZ;

   private final List<FrameVector3DReadOnly> allBasisVectors = new ArrayList<>();
   private final FrameVector3D vrpDelta = new FrameVector3D();
   private final FrameVector3D vrpStart = new FrameVector3D();

   public VRPTrackingCostCalculator(LinearMPCIndexHandler indexHandler, double gravityZ)
   {
      this.indexHandler = indexHandler;
      this.gravityZ = -Math.abs(gravityZ);
   }

   /**
    * Calculates the quadratic cost function for tracking a nominal VRP trajectory, which is specified by the {@link VRPTrackingCommand}
    *
    * @param costHessianToPack hessian of the quadratic cost function to pack
    * @param costGradientToPack gradient of the quadratic cost function to pack
    * @param objective objective object containing the desired VRP trajectory information
    * @return whether the computation was successful
    */
   public boolean calculateVRPTrackingObjective(DMatrix costHessianToPack, DMatrix costGradientToPack, VRPTrackingCommand objective)
   {
      int segmentNumber = objective.getSegmentNumber();
      int startCoMIdx = indexHandler.getComCoefficientStartIndex(segmentNumber, 0);
      int startRhoIdx = indexHandler.getRhoCoefficientStartIndex(segmentNumber);

      return calculateVRPTrackingObjectiveInternal(costHessianToPack, costGradientToPack, objective, startCoMIdx, startRhoIdx);
   }

   public boolean calculateCompactVRPTrackingObjective(DMatrix costHessianToPack, DMatrix costGradientToPack, VRPTrackingCommand objective)
   {
      return calculateVRPTrackingObjectiveInternal(costHessianToPack, costGradientToPack, objective, 0, LinearMPCIndexHandler.comCoefficientsPerSegment);
   }

   private boolean calculateVRPTrackingObjectiveInternal(DMatrix costHessianToPack,
                                                         DMatrix costGradientToPack,
                                                        VRPTrackingCommand objective,
                                                        int startCoMIdx,
                                                        int startRhoIdx)
   {
      double omega = objective.getOmega();
      double w2 = omega * omega;
      double w4 = w2 * w2;

      double t = objective.getSegmentDuration();
      double t2 = t * t;
      double t3 = t * t2;
      double t4 = t * t3;
      double t5 = t * t4;
      double t6 = t * t5;
      double t7 = t * t6;

      double c0c0 = t3 / 3.0;
      double c0c1 = 0.5 * t2;

      double gc0 = t4 / 8.0 - 0.5 * t2 / w2;
      double gc1 = t3 / 6.0 - t / w2;

      costHessianToPack.set(startCoMIdx, startCoMIdx, c0c0);
      costHessianToPack.set(startCoMIdx, startCoMIdx + 1, c0c1);
      costHessianToPack.set(startCoMIdx + 1, startCoMIdx, c0c1);
      costHessianToPack.set(startCoMIdx + 1, startCoMIdx + 1, t);

      costHessianToPack.set(startCoMIdx + 2, startCoMIdx + 2, c0c0);
      costHessianToPack.set(startCoMIdx + 2, startCoMIdx + 3, c0c1);
      costHessianToPack.set(startCoMIdx + 3, startCoMIdx + 2, c0c1);
      costHessianToPack.set(startCoMIdx + 3, startCoMIdx + 3, t);

      costHessianToPack.set(startCoMIdx + 4, startCoMIdx + 4, c0c0);
      costHessianToPack.set(startCoMIdx + 4, startCoMIdx + 5, c0c1);
      costHessianToPack.set(startCoMIdx + 5, startCoMIdx + 4, c0c1);
      costHessianToPack.set(startCoMIdx + 5, startCoMIdx + 5, t);

      costGradientToPack.set(startCoMIdx + 4, 0, gc0 * gravityZ);
      costGradientToPack.set(startCoMIdx + 5, 0, gc1 * gravityZ);

      allBasisVectors.clear();
      for (int contactPlaneIdx = 0; contactPlaneIdx < objective.getNumberOfContacts(); contactPlaneIdx++)
      {
         MPCContactPlane contactPlane = objective.getContactPlaneHelper(contactPlaneIdx);
         for (int contactPointIdx = 0; contactPointIdx < contactPlane.getNumberOfContactPoints(); contactPointIdx++)
         {
            MPCContactPoint contactPoint = contactPlane.getContactPointHelper(contactPointIdx);
            for (int i = 0; i < contactPoint.getRhoSize(); i++)
            {
               allBasisVectors.add(contactPoint.getBasisVector(i));
            }
         }
      }

      double a2a2 = t7 / 7.0 - 12.0 * t5 / (5.0 * w2) + 12.0 / w4 * t3;
      double a2a3 = t6/ 6.0 - 2.0 * t4 / w2 + 6.0 / w4 * t2;
      double a3a3 = t5 / 5.0 - 4.0 / 3.0 * t3 / w2 + 4.0 / w4 * t;

      double a2c0 = t5 / 5.0 - 2.0 * t3 / w2;
      double a3c0 = t4 / 4.0 - t2 / w2;
      double a2c1 = t4 / 4.0 - 3.0 * t2 / w2;
      double a3c1 = t3 / 3.0 - 2.0 * t / w2;

      double a2Delta = t4 / 5.0 - 2.0 * t2 / w2;
      double a3Delta = t3 / 4.0 - t / w2;
      double a2Start = a2c1;
      double a3Start = a3c1;

      double ga2 = t6 / 12.0 - t4 / w2 + 3.0 * t2 / w4;
      double ga3 = t5 / 10 - 2.0 * t3 / (3.0 * w2) + 2.0 / w4 * t;

      vrpStart.set(objective.getStartVRP());
      vrpDelta.sub(objective.getEndVRP(), objective.getStartVRP());

      // TODO review to see if the set vs add methods are correct
      for (int ordinal = 0; ordinal < 3; ordinal++)
      {
         int offset = 2 * ordinal + startCoMIdx;
         double c0 = t2 / 3.0 * vrpDelta.getElement(ordinal) + t2 / 2.0 * vrpStart.getElement(ordinal);
         double c1 = t / 2.0 * vrpDelta.getElement(ordinal) + t * vrpStart.getElement(ordinal);

         unsafe_add(costGradientToPack, offset, 0, -c0);
         unsafe_add(costGradientToPack, offset + 1, 0, -c1);
      }


      for (int i = 0; i < allBasisVectors.size(); i++)
      {
         int idxI = 4 * i + startRhoIdx + 2;

         FrameVector3DReadOnly basisVector = allBasisVectors.get(i);

         unsafe_add(costHessianToPack, idxI, idxI, a2a2);
         unsafe_add(costHessianToPack, idxI, idxI + 1, a2a3);
         unsafe_add(costHessianToPack, idxI + 1, idxI, a2a3);
         unsafe_add(costHessianToPack, idxI + 1, idxI + 1, a3a3);

         for (int j = i + 1; j < allBasisVectors.size(); j++)
         {
            FrameVector3DReadOnly otherBasisVector = allBasisVectors.get(j);

            double basisDot = basisVector.dot(otherBasisVector);

            int idxJ = 4 * j + startRhoIdx + 2;

            unsafe_add(costHessianToPack, idxI, idxJ, basisDot * a2a2);
            unsafe_add(costHessianToPack, idxI, idxJ + 1, basisDot * a2a3);
            unsafe_add(costHessianToPack, idxI + 1, idxJ, basisDot * a2a3);
            unsafe_add(costHessianToPack, idxI + 1, idxJ + 1, basisDot * a3a3);

            // we know it's symmetric, and this way we can avoid iterating as much
            unsafe_add(costHessianToPack, idxJ, idxI, basisDot * a2a2);
            unsafe_add(costHessianToPack, idxJ + 1, idxI, basisDot * a2a3);
            unsafe_add(costHessianToPack, idxJ, idxI + 1, basisDot * a2a3);
            unsafe_add(costHessianToPack, idxJ + 1, idxI + 1, basisDot * a3a3);
         }


         for (int ordinal = 0; ordinal < 3; ordinal++)
         {
            int offset = startCoMIdx + 2 * ordinal;
            double value = basisVector.getElement(ordinal);
            unsafe_add(costHessianToPack, offset, idxI, a2c0 * value);
            unsafe_add(costHessianToPack, offset, idxI + 1, a3c0 * value);
            unsafe_add(costHessianToPack, offset + 1, idxI, a2c1 * value);
            unsafe_add(costHessianToPack, offset + 1, idxI + 1, a3c1 * value);

            // symmetric...
            unsafe_add(costHessianToPack, idxI, offset, a2c0 * value);
            unsafe_add(costHessianToPack, idxI + 1, offset,  a3c0 * value);
            unsafe_add(costHessianToPack, idxI, offset + 1, a2c1 * value);
            unsafe_add(costHessianToPack, idxI + 1, offset + 1, a3c1 * value);
         }

         double basisDotDelta = vrpDelta.dot(basisVector);
         double basisDotStart = vrpStart.dot(basisVector);
         double basisDotG = basisVector.getZ() * gravityZ;

         unsafe_add(costGradientToPack, idxI, 0, -basisDotDelta * a2Delta - basisDotStart * a2Start + basisDotG * ga2);
         unsafe_add(costGradientToPack, idxI + 1, 0, -basisDotDelta * a3Delta - basisDotStart * a3Start + basisDotG * ga3);
      }

      return true;
   }

   private static  void unsafe_add(DMatrix matrixToPack, int row, int col, double value)
   {
      matrixToPack.unsafe_set(row, col, matrixToPack.unsafe_get(row, col) + value);
   }

}
