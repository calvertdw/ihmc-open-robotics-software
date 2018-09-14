package us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner.AMGeneration;

import us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner.CoPGeneration.CoPPointsInFoot;
import us.ihmc.commonWalkingControlModules.configurations.SmoothCMPPlannerParameters;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFrameVector3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;

import java.util.List;

public interface AngularMomentumTrajectoryGeneratorInterface 
{
   void clear();

   void update(double currentTime);

   void getDesiredAngularMomentum(FixedFrameVector3DBasics desiredAngMomToPack, FixedFrameVector3DBasics desiredTorqueToPack);

   void initializeForDoubleSupport(double currentTime, boolean isStanding);

   void initializeForSingleSupport(double currentTime);

   void computeReferenceAngularMomentumStartingFromDoubleSupport(boolean atAStop);

   void computeReferenceAngularMomentumStartingFromSingleSupport();

   List<AngularMomentumTrajectory> getTransferAngularMomentumTrajectories();

   List<AngularMomentumTrajectory> getSwingAngularMomentumTrajectories();

   void initializeParameters(SmoothCMPPlannerParameters smoothCMPPlannerParameters, double totalMass, double gravityZ);

   void addCopAndComSetpointsToPlan(List<CoPPointsInFoot> copLocations, List<? extends FramePoint3DReadOnly> comInitialPositions,
                                    List<? extends FramePoint3DReadOnly> comFinalPositions, List<? extends FrameVector3DReadOnly> comInitialVelocities,
                                    List<? extends FrameVector3DReadOnly> comFinalVelocities, List<? extends FrameVector3DReadOnly> comInitialAccelerations,
                                    List<? extends FrameVector3DReadOnly> comFinalAccelerations, int numberOfRegisteredFootsteps);
}
