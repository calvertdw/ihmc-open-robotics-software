package us.ihmc.atlas.roughTerrainWalking;

import org.junit.Test;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.atlas.parameters.AtlasSimpleICPOptimizationParameters;
import us.ihmc.atlas.parameters.AtlasWalkingControllerParameters;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.roughTerrainWalking.AvatarPushRecoveryOverGapTest;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.FAST, IntegrationCategory.VIDEO})
public class AtlasPushRecoveryOverGapTest extends AvatarPushRecoveryOverGapTest
{
   @Override
   @ContinuousIntegrationTest(estimatedDuration = 30.0)
   @Test(timeout = 70000)
   public void testNoPush() throws SimulationExceededMaximumTimeException
   {
      super.testNoPush();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 30.0)
   @Test(timeout = 70000)
   public void testForwardPush() throws SimulationExceededMaximumTimeException
   {
      super.testForwardPush();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 30.0)
   @Test(timeout = 70000)
   public void testSidePush() throws SimulationExceededMaximumTimeException
   {
      super.testSidePush();
   }

   @Override
   public DRCRobotModel getRobotModel()
   {
      AtlasRobotModel atlasRobotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.SCS, false)
      {
         @Override
         public WalkingControllerParameters getWalkingControllerParameters()
         {
            return new AtlasWalkingControllerParameters(RobotTarget.SCS, getJointMap(), getContactPointParameters())
            {
               @Override
               public boolean useOptimizationBasedICPController()
               {
                  return true;
               }

               @Override
               public double getMinimumSwingTimeForDisturbanceRecovery()
               {
                  return 0.5;
               }

               @Override
               public ICPOptimizationParameters getICPOptimizationParameters()
               {
                  return new AtlasSimpleICPOptimizationParameters(false)
                  {
                     @Override
                     public boolean useAngularMomentum()
                     {
                        return true;
                     }

                     @Override
                     public boolean useStepAdjustment()
                     {
                        return true;
                     }
                  };
               }
            };

         }
      };

      return atlasRobotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }
}
