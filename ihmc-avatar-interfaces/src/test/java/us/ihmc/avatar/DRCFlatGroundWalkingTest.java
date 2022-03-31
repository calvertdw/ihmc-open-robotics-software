package us.ihmc.avatar;

import static us.ihmc.robotics.Assert.assertTrue;
import static us.ihmc.robotics.Assert.fail;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.factory.AvatarSimulation;
import us.ihmc.avatar.testTools.scs2.SCS2AvatarTestingSimulation;
import us.ihmc.avatar.testTools.scs2.SCS2AvatarTestingSimulationFactory;
import us.ihmc.avatar.testTools.scs2.SCS2RunsSameWayTwiceVerifier;
import us.ihmc.commonWalkingControlModules.desiredFootStep.footstepGenerator.HeadingAndVelocityEvaluationScriptParameters;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.robotDataLogger.RobotVisualizer;
import us.ihmc.robotics.Assert;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationConstructionSetTools.util.environments.FlatGroundEnvironment;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

@Tag("video")
public abstract class DRCFlatGroundWalkingTest implements MultiRobotTestInterface
{
   private SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();
   private SCS2AvatarTestingSimulation simulationTestHelper;

   /**
    * TODO Need to implement a specific test for that. As the footstep generator for flat ground
    * walking keeps changing the upcoming footsteps on the fly, the ICP planner ends up creating
    * discontinuities. But this is an expected behavior.
    */
   private static final boolean CHECK_ICP_CONTINUITY = false;

   private static final double yawingTimeDuration = 0.5;
   private static final double standingTimeDuration = 1.0;
   private static final double defaultWalkingTimeDuration = BambooTools.isEveryCommitBuild() ? 45.0 : 90.0;
   private static final boolean useVelocityAndHeadingScript = true;

   @BeforeEach
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @AfterEach
   public void destroySimulationAndRecycleMemory()
   {
      // Do this here in case a test fails. That way the memory will be recycled.
      if (simulationTestHelper != null)
      {
         simulationTestHelper.finishTest();
         simulationTestHelper = null;
      }

      simulationTestingParameters = null;
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @Override
   public abstract DRCRobotModel getRobotModel();

   public abstract boolean doPelvisWarmup();

   public boolean getUsePerfectSensors()
   {
      return false;
   }

   @Tag("humanoid-flat-ground")
   @Test
   public void testFlatGroundWalking()
   {
      runFlatGroundWalking();
   }

   public void runFlatGroundWalking()
   {
      DRCRobotModel robotModel = getRobotModel();
      boolean doPelvisWarmup = doPelvisWarmup();
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());
      simulationTestingParameters.setUsePefectSensors(getUsePerfectSensors());

      FlatGroundEnvironment flatGround = new FlatGroundEnvironment();
      SCS2AvatarTestingSimulationFactory simulationTestHelperFactory = SCS2AvatarTestingSimulationFactory.createDefaultTestSimulationFactory(robotModel,
                                                                                                                                             flatGround,
                                                                                                                                             simulationTestingParameters);
      simulationTestHelperFactory.setDefaultHighLevelHumanoidControllerFactory(useVelocityAndHeadingScript, getWalkingScriptParameters());
      simulationTestHelperFactory.getHighLevelHumanoidControllerFactory().createUserDesiredControllerCommandGenerator();
      simulationTestHelper = simulationTestHelperFactory.createAvatarTestingSimulation();
      simulationTestHelper.start();

      if (CHECK_ICP_CONTINUITY)
         simulationTestHelper.addDesiredICPContinuityAssertion(3.0 * robotModel.getControllerDT());

      setupCameraForUnitTest();
      simulateAndAssertGoodWalking(simulationTestHelper, doPelvisWarmup);

      //      if (simulationTestingParameters.getCheckNothingChangedInSimulation())
      //         simulationTestHelper.checkNothingChanged();

      simulationTestHelper.createVideo(getSimpleRobotName(), 2);
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   @Test
   public void testReset()
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      DRCRobotModel robotModel = getRobotModel();
      SCS2AvatarTestingSimulationFactory simulationTestHelperFactory = SCS2AvatarTestingSimulationFactory.createDefaultTestSimulationFactory(robotModel,
                                                                                                                                             new FlatGroundEnvironment(),
                                                                                                                                             simulationTestingParameters);
      simulationTestHelperFactory.setDefaultHighLevelHumanoidControllerFactory(useVelocityAndHeadingScript, getWalkingScriptParameters());
      simulationTestHelper = simulationTestHelperFactory.createAvatarTestingSimulation();
      simulationTestHelper.start();

      ((YoBoolean) simulationTestHelper.findVariable("walkCSG")).set(true);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertTrue(simulationTestHelper.simulateAndWait(1.0));
         simulationTestHelper.resetRobot(false);
      }
   }

   private void simulateAndAssertGoodWalking(SCS2AvatarTestingSimulation simulationTestHelper, boolean doPelvisYawWarmup)
   {
      YoBoolean walk = (YoBoolean) simulationTestHelper.findVariable("walkCSG");
      YoDouble comError = (YoDouble) simulationTestHelper.findVariable("positionError_comHeight");
      if (comError == null)
      {
         comError = (YoDouble) simulationTestHelper.findVariable("pelvisErrorPositionZ");
      }
      YoBoolean userUpdateDesiredPelvisPose = (YoBoolean) simulationTestHelper.findVariable("userUpdateDesiredPelvisPose");
      YoBoolean userDoPelvisPose = (YoBoolean) simulationTestHelper.findVariable("userDoPelvisPose");
      YoDouble userDesiredPelvisPoseYaw = (YoDouble) simulationTestHelper.findVariable("userDesiredPelvisPoseYaw");
      YoDouble userDesiredPelvisPoseTrajectoryTime = (YoDouble) simulationTestHelper.findVariable("userDesiredPelvisPoseTrajectoryTime");
      YoDouble icpErrorX = (YoDouble) simulationTestHelper.findVariable("icpErrorX");
      YoDouble icpErrorY = (YoDouble) simulationTestHelper.findVariable("icpErrorY");

      YoDouble controllerICPErrorX = (YoDouble) simulationTestHelper.findVariable("controllerICPErrorX");
      YoDouble controllerICPErrorY = (YoDouble) simulationTestHelper.findVariable("controllerICPErrorY");

      simulationTestHelper.simulateAndWait(standingTimeDuration);

      walk.set(false);

      if (doPelvisYawWarmup)
      {
         userDesiredPelvisPoseTrajectoryTime.set(0.0);
         userUpdateDesiredPelvisPose.set(true);
         simulationTestHelper.simulateAndWait(0.1);

         double startingYaw = userDesiredPelvisPoseYaw.getDoubleValue();
         userDesiredPelvisPoseYaw.set(startingYaw + Math.PI / 4.0);
         userDoPelvisPose.set(true);

         simulationTestHelper.simulateAndWait(yawingTimeDuration);

         double icpError;
         if (icpErrorX != null && icpErrorY != null)
            icpError = Math.sqrt(icpErrorX.getDoubleValue() * icpErrorX.getDoubleValue() + icpErrorY.getDoubleValue() * icpErrorY.getDoubleValue());
         else
            icpError = Math.sqrt(controllerICPErrorX.getDoubleValue() * controllerICPErrorX.getDoubleValue()
                  + controllerICPErrorY.getDoubleValue() * controllerICPErrorY.getDoubleValue());
         assertTrue(icpError < 0.005);

         userDesiredPelvisPoseYaw.set(startingYaw);
         userDoPelvisPose.set(true);
         simulationTestHelper.simulateAndWait(yawingTimeDuration + 0.3);

         if (icpErrorX != null && icpErrorY != null)
            icpError = Math.sqrt(icpErrorX.getDoubleValue() * icpErrorX.getDoubleValue() + icpErrorY.getDoubleValue() * icpErrorY.getDoubleValue());
         else
            icpError = Math.sqrt(controllerICPErrorX.getDoubleValue() * controllerICPErrorX.getDoubleValue()
                  + controllerICPErrorY.getDoubleValue() * controllerICPErrorY.getDoubleValue());
         assertTrue(icpError < 0.005);
      }

      walk.set(true);

      double timeIncrement = 1.0;

      while (simulationTestHelper.getSimulationTime() - standingTimeDuration < defaultWalkingTimeDuration)
      {
         simulationTestHelper.simulateAndWait(timeIncrement);
         if (Math.abs(comError.getDoubleValue()) > 0.06)
            fail("Math.abs(comError.getDoubleValue()) > 0.06: " + comError.getDoubleValue() + " at t = " + simulationTestHelper.getSimulationTime());
      }
   }

   //TODO: Get rid of the stuff below and use a test helper.....

   @AfterEach
   public void destroyOtherStuff()
   {
      if (avatarSimulation != null)
      {
         avatarSimulation.dispose();
         avatarSimulation = null;
      }

      if (robotVisualizer != null)
      {
         robotVisualizer.close();
         robotVisualizer = null;
      }
   }

   private AvatarSimulation avatarSimulation;
   private RobotVisualizer robotVisualizer;

   protected void setupAndTestFlatGroundSimulationTrackTwice(DRCRobotModel robotModel)
   {
      SCS2AvatarTestingSimulation simulationTestHelperOne = setupFlatGroundSimulationTrackForSameWayTwiceVerifier(robotModel);
      SCS2AvatarTestingSimulation simulationTestHelperTwo = setupFlatGroundSimulationTrackForSameWayTwiceVerifier(robotModel);

      double walkingTimeDuration = 20.0;
      SCS2RunsSameWayTwiceVerifier verifier = new SCS2RunsSameWayTwiceVerifier(simulationTestHelperOne,
                                                                               simulationTestHelperTwo,
                                                                               standingTimeDuration,
                                                                               walkingTimeDuration);

      checkSimulationRunsSameWayTwice(verifier);

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   private SCS2AvatarTestingSimulation setupFlatGroundSimulationTrackForSameWayTwiceVerifier(DRCRobotModel robotModel)
   {
      SCS2AvatarTestingSimulationFactory simulationTestHelperFactory = SCS2AvatarTestingSimulationFactory.createDefaultTestSimulationFactory(robotModel,
                                                                                                                                             new FlatGroundEnvironment(),
                                                                                                                                             simulationTestingParameters);
      simulationTestHelperFactory.setDefaultHighLevelHumanoidControllerFactory(useVelocityAndHeadingScript, getWalkingScriptParameters());
      SCS2AvatarTestingSimulation simulationTestHelper = simulationTestHelperFactory.createAvatarTestingSimulation();
      setupCameraForUnitTest();

      return simulationTestHelper;
   }

   private void checkSimulationRunsSameWayTwice(SCS2RunsSameWayTwiceVerifier verifier)
   {
      ArrayList<String> stringsToIgnore = new ArrayList<String>();
      stringsToIgnore.add("nano");
      stringsToIgnore.add("milli");
      stringsToIgnore.add("Timer");
      stringsToIgnore.add("actualControl");
      stringsToIgnore.add("actualEstimator");
      stringsToIgnore.add("totalDelay");
      stringsToIgnore.add("Time");

      double maxPercentDifference = 0.000001;
      assertTrue("Simulation did not run same way twice!", verifier.verifySimRunsSameWayTwice(maxPercentDifference, stringsToIgnore));
   }

   private void setupCameraForUnitTest()
   {
      simulationTestHelper.setCamera(new Point3D(0.6, 0.4, 1.1), new Point3D(-0.15, 10.0, 3.0));
   }

   public SimulationTestingParameters getSimulationTestingParameters()
   {
      return simulationTestingParameters;
   }

   public HeadingAndVelocityEvaluationScriptParameters getWalkingScriptParameters()
   {
      return null;
   }
}
