package us.ihmc.commonWalkingControlModules.controllerCore.command;

import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.RootJointDesiredConfigurationData;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.RootJointDesiredConfigurationDataReadOnly;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputList;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputListBasics;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputListReadOnly;

public class ControllerCoreOutput implements ControllerCoreOutputReadOnly
{
   private final CenterOfPressureDataHolder centerOfPressureDataHolder;
   private final FrameVector3D linearMomentumRate = new FrameVector3D();
   private final RootJointDesiredConfigurationData rootJointDesiredConfigurationData = new RootJointDesiredConfigurationData();
   private final JointDesiredOutputListBasics lowLevelOneDoFJointDesiredDataHolder;

   /**
    * Do not use this constructor.
    * <p>
    * This is a simple command that is only holding on to data. Use the getters of this class to get its data. This
    * constructor should be removed after the thread refactor summer 2019.
    */
   @Deprecated
   public ControllerCoreOutput(CenterOfPressureDataHolder centerOfPressureDataHolder, OneDoFJointBasics[] controlledOneDoFJoints,
                               JointDesiredOutputList lowLevelControllerOutput)
   {
      this.centerOfPressureDataHolder = centerOfPressureDataHolder;
      linearMomentumRate.setToNaN(ReferenceFrame.getWorldFrame());
      if (lowLevelControllerOutput != null)
         lowLevelOneDoFJointDesiredDataHolder = lowLevelControllerOutput;
      else
         lowLevelOneDoFJointDesiredDataHolder = new JointDesiredOutputList(controlledOneDoFJoints);
   }

   public ControllerCoreOutput()
   {
      centerOfPressureDataHolder = new CenterOfPressureDataHolder();
      lowLevelOneDoFJointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder();
   }

   public void setCenterOfPressureData(CenterOfPressureDataHolder centerOfPressureDataHolder)
   {
      this.centerOfPressureDataHolder.set(centerOfPressureDataHolder);
   }

   public void setDesiredCenterOfPressure(FramePoint2D cop, RigidBodyBasics rigidBody)
   {
      centerOfPressureDataHolder.setCenterOfPressure(cop, rigidBody);
   }

   @Override
   public void getDesiredCenterOfPressure(FramePoint2D copToPack, RigidBodyBasics rigidBody)
   {
      centerOfPressureDataHolder.getCenterOfPressure(copToPack, rigidBody);
   }

   public void setLinearMomentumRate(FrameVector3DReadOnly linearMomentumRate)
   {
      this.linearMomentumRate.setIncludingFrame(linearMomentumRate);
   }

   @Override
   public void getLinearMomentumRate(FrameVector3D linearMomentumRateToPack)
   {
      linearMomentumRateToPack.setIncludingFrame(linearMomentumRate);
   }

   public void setRootJointDesiredConfigurationData(RootJointDesiredConfigurationDataReadOnly rootJointDesiredConfigurationData)
   {
      this.rootJointDesiredConfigurationData.set(rootJointDesiredConfigurationData);
   }

   @Override
   public RootJointDesiredConfigurationDataReadOnly getRootJointDesiredConfigurationData()
   {
      return rootJointDesiredConfigurationData;
   }

   public void setLowLevelOneDoFJointDesiredDataHolder(JointDesiredOutputListReadOnly lowLevelOneDoFJointDesiredDataHolder)
   {
      this.lowLevelOneDoFJointDesiredDataHolder.overwriteWith(lowLevelOneDoFJointDesiredDataHolder);
   }

   @Override
   public JointDesiredOutputListReadOnly getLowLevelOneDoFJointDesiredDataHolder()
   {
      return lowLevelOneDoFJointDesiredDataHolder;
   }

   public void set(ControllerCoreOutput other)
   {
      centerOfPressureDataHolder.set(other.centerOfPressureDataHolder);
      linearMomentumRate.setIncludingFrame(other.linearMomentumRate);
      rootJointDesiredConfigurationData.set(other.rootJointDesiredConfigurationData);
      lowLevelOneDoFJointDesiredDataHolder.overwriteWith(other.lowLevelOneDoFJointDesiredDataHolder);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
      {
         return true;
      }
      else if (obj instanceof ControllerCoreOutput)
      {
         ControllerCoreOutput other = (ControllerCoreOutput) obj;
         if (!centerOfPressureDataHolder.equals(other.centerOfPressureDataHolder))
            return false;
         if (!linearMomentumRate.equals(other.linearMomentumRate))
            return false;
         if (!rootJointDesiredConfigurationData.equals(other.rootJointDesiredConfigurationData))
            return false;
         if (!lowLevelOneDoFJointDesiredDataHolder.equals(other.lowLevelOneDoFJointDesiredDataHolder))
            return false;
         return true;
      }
      else
      {
         return false;
      }
   }
}
