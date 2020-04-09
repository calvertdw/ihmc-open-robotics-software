package us.ihmc.commonWalkingControlModules.heightPlanning;

import org.junit.jupiter.api.Test;
import us.ihmc.commonWalkingControlModules.desiredFootStep.NewTransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.heightPlanning.BetterLookAheadCoMHeightTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.heightPlanning.CoMHeightPartialDerivativesData;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFramePoint3D;

import static us.ihmc.robotics.Assert.assertTrue;

public class BetterLookAheadCoMHeightTrajectoryGeneratorTest
{
   private static final boolean visualize = true;
   private static final double minimumHeight = 0.75;
   private static double nominalHeight = 0.8;
   private static final double maximumHeight = 0.95;
   private static final double doubleSupportIn = 0.3;

   @Test
   public void testFlat()
   {
      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, nominalHeight);
      FramePoint3D start = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.125, 0.0);
      FramePoint3D end = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.5, -0.125, 0.0);

      transferToAndNextFootstepsData.setTransferToPosition(end);

      runTest(start, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   @Test
   public void testLongStep()
   {
      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), -0.1, 0.0, nominalHeight);
      FramePoint3D start = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.125, 0.0);
      FramePoint3D end = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.75, -0.125, 0.0);
      FramePoint3D endCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.35, 0.0, nominalHeight);

      transferToAndNextFootstepsData.setTransferToPosition(end);
      transferToAndNextFootstepsData.setComAtEndOfState(endCoM);

      runTest(start, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData, false);
   }

   @Test
   public void testFlatKindOfWeird()
   {
      nominalHeight = minimumHeight - 0.05;
      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, nominalHeight);
      FramePoint3D start = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.125, 0.0);
      FramePoint3D end = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.5, -0.125, 0.0);

      transferToAndNextFootstepsData.setTransferToPosition(end);

      runTest(start, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   @Test
   public void testFlatEasyMultiStep()
   {
      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 0.919);
      FramePoint3D transferFrom = new FramePoint3D(ReferenceFrame.getWorldFrame(), -0.007, 0.164, 0.0);
      FramePoint3D transferTo = new FramePoint3D(ReferenceFrame.getWorldFrame(), -0.007, -0.164, 0.0);
      FramePoint3D nextFootstep = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.2, 0.2, 0.0);

      transferToAndNextFootstepsData.setTransferToPosition(transferTo);

      runTest(transferFrom, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   @Test
   public void testFlatMultiStep()
   {
      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 0.919);
      FramePoint3D transferFrom = new FramePoint3D(ReferenceFrame.getWorldFrame(), -0.007, 0.164, 0.0);
      FramePoint3D transferTo = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.4, -0.164, 0.0);
      FramePoint3D nextFootstep = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.8, 0.2, 0.0);

      transferToAndNextFootstepsData.setTransferToPosition(transferTo);

      runTest(transferFrom, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   @Test
   public void testTrickyCaseGoingUp()
   {
      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.57273, -0.02010, 0.80325);
      FramePoint3D transferFrom = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.60080, -0.19949, -0.00120);
      FramePoint3D transferTo = new FramePoint3D(ReferenceFrame.getWorldFrame(), 1.0, 0.2, 0.0);
      FramePoint3D nextFootstep = new FramePoint3D(ReferenceFrame.getWorldFrame(), 1.0, -0.2, 0.151);

      transferToAndNextFootstepsData.setTransferToPosition(transferTo);

      runTest(transferFrom, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   @Test
   public void testBigSteppingDown()
   {
      double stepDownHeight = -0.4;

      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, -stepDownHeight + nominalHeight);
      FramePoint3D start = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.125, -stepDownHeight);
      FramePoint3D end = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.2, -0.125, 0.0);

      transferToAndNextFootstepsData.setTransferToPosition(end);

      runTest(start, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   @Test
   public void testSteppingDown()
   {
      double stepDownHeight = -0.2;

      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, -stepDownHeight + nominalHeight);
      FramePoint3D start = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.125, -stepDownHeight);
      FramePoint3D end = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.2, -0.125, 0.0);

      transferToAndNextFootstepsData.setTransferToPosition(end);

      runTest(start, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   @Test
   public void testSteppingUp()
   {
      double stepHeight = 0.2;

      NewTransferToAndNextFootstepsData transferToAndNextFootstepsData = new NewTransferToAndNextFootstepsData();
      FramePoint3D startCoM = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, nominalHeight);
      FramePoint3D start = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.0, 0.125, 0.0);
      FramePoint3D end = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.2, -0.125, stepHeight);

      transferToAndNextFootstepsData.setTransferToPosition(end);

      runTest(start, startCoM, RobotSide.RIGHT, transferToAndNextFootstepsData);
   }

   private void runTest(FramePoint3DReadOnly startFoot,
                        FramePoint3DReadOnly startPosition,
                        RobotSide stepSide,
                        NewTransferToAndNextFootstepsData transferToAndNextFootstepsData)
   {
      runTest(startFoot, startPosition, stepSide, transferToAndNextFootstepsData, true);
   }

   private void runTest(FramePoint3DReadOnly startFoot,
                        FramePoint3DReadOnly startPosition,
                        RobotSide stepSide,
                        NewTransferToAndNextFootstepsData transferToAndNextFootstepsData,
                        boolean inTransfer)
   {
      YoGraphicsListRegistry graphicsListRegistry = new YoGraphicsListRegistry();
      YoVariableRegistry registry = new YoVariableRegistry("test");
      Robot robot = new Robot("dummy");

      SideDependentList<PoseReferenceFrame> soleFrames = new SideDependentList<>();
      for (RobotSide robotSide : RobotSide.values)
      {
         soleFrames.put(robotSide, new PoseReferenceFrame(robotSide.getLowerCaseName() + "Frame", ReferenceFrame.getWorldFrame()));
      }
      PoseReferenceFrame comFrame = new PoseReferenceFrame("comFrame", ReferenceFrame.getWorldFrame());
      BetterLookAheadCoMHeightTrajectoryGenerator lookAhead = new BetterLookAheadCoMHeightTrajectoryGenerator(minimumHeight,
                                                                                                              nominalHeight,
                                                                                                              maximumHeight,
                                                                                                              0.0,
                                                                                                              doubleSupportIn,
                                                                                                              comFrame,
                                                                                                              soleFrames,
                                                                                                              robot.getYoTime(),
                                                                                                              graphicsListRegistry,
                                                                                                              registry);

      RobotSide supportSide = stepSide.getOppositeSide();

      soleFrames.get(supportSide).setPositionAndUpdate(startFoot);
      comFrame.setPositionAndUpdate(startPosition);

      lookAhead.reset();
      lookAhead.setSupportLeg(supportSide);
      lookAhead.initialize(transferToAndNextFootstepsData, 0.0, inTransfer);

      CoMHeightPartialDerivativesData data = new CoMHeightPartialDerivativesData();

      YoFramePoint3D yoStartFoot = new YoFramePoint3D("startFoot", ReferenceFrame.getWorldFrame(), registry);
      YoFramePoint3D yoEndFoot = new YoFramePoint3D("endFoot", ReferenceFrame.getWorldFrame(), registry);
      yoStartFoot.set(startFoot);
      yoStartFoot.setY(0.0);
      yoEndFoot.set(transferToAndNextFootstepsData.getTransferToPosition());
      yoEndFoot.setY(0.0);

      AppearanceDefinition startMaxBallAppearance = YoAppearance.Red();
      AppearanceDefinition startMinBallAppearance = YoAppearance.Red();
      AppearanceDefinition endMaxBallAPpearan = YoAppearance.Blue();
      AppearanceDefinition endMinBallAPpearan = YoAppearance.Blue();
      startMaxBallAppearance.setTransparency(0.9);
      startMinBallAppearance.setTransparency(0.95);
      endMaxBallAPpearan.setTransparency(0.9);
      endMinBallAPpearan.setTransparency(0.95);
      YoGraphicPosition startMinBall = new YoGraphicPosition("startFootMin", yoStartFoot, minimumHeight, startMinBallAppearance);
      YoGraphicPosition startMaxBall = new YoGraphicPosition("startFootMax", yoStartFoot, maximumHeight, startMaxBallAppearance);
      YoGraphicPosition endMaxBall = new YoGraphicPosition("endFootMax", yoEndFoot, maximumHeight, endMaxBallAPpearan);
      YoGraphicPosition endMinBall = new YoGraphicPosition("endFootMin", yoEndFoot, minimumHeight, endMinBallAPpearan);

      graphicsListRegistry.registerYoGraphic("testStartMin", startMinBall);
      graphicsListRegistry.registerYoGraphic("testStartMax", startMaxBall);
      graphicsListRegistry.registerYoGraphic("testEndMin", endMinBall);
      graphicsListRegistry.registerYoGraphic("testEndMax", endMaxBall);

      if (visualize)
      {
         SimulationConstructionSet scs = new SimulationConstructionSet();
         scs.setRobot(robot);
         scs.getRootRegistry().addChild(registry);

         scs.addYoGraphicsListRegistry(graphicsListRegistry);
         scs.startOnAThread();
         scs.tickAndUpdate();

         ThreadTools.sleepForever();
      }

      for (double alpha = 0.0; alpha <= 1.0; alpha += 0.01)
      {

         FramePoint3D queryPoint = new FramePoint3D();
         FramePoint3D endPosition = new FramePoint3D(transferToAndNextFootstepsData.getTransferToPosition());
         endPosition.setY(0.0);
         endPosition.addZ(nominalHeight);
         queryPoint.interpolate(startPosition, endPosition, alpha);
         lookAhead.solve(data, queryPoint, inTransfer);

         FramePoint3D currentCoM = new FramePoint3D(queryPoint);
         FramePoint3D upcomingPosition = new FramePoint3D(transferToAndNextFootstepsData.getTransferToPosition());
         currentCoM.setZ(data.getComHeight());
         currentCoM.changeFrame(soleFrames.get(supportSide));
         currentCoM.setY(0.0);
         upcomingPosition.changeFrame(soleFrames.get(supportSide));
         upcomingPosition.setY(0.0);

         if (alpha < (inTransfer ? 0.5 : lookAhead.getDoubleSupportPercentageIn()))
         {
            double distance = currentCoM.distanceFromOrigin();
            assertTrue(distance + " < " + maximumHeight + " failed at " + alpha, distance < maximumHeight + 1e-3);
            assertTrue(distance + " > " + minimumHeight + " failed at " + alpha, distance > minimumHeight - 1e-3);
         }
         else
         {

            double distance = currentCoM.distance(upcomingPosition);
            assertTrue(distance + " < " + maximumHeight + " failed at " + alpha, distance < maximumHeight + 1e-3);
            assertTrue(distance + " > " + minimumHeight + " failed at " + alpha, distance > minimumHeight - 1e-3);
         }
      }
   }
}
