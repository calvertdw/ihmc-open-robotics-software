package us.ihmc.commonWalkingControlModules.capturePoint.optimization.simpleController;

import org.junit.Test;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.SimpleICPOptimizationQPSolver;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.SimpleICPOptimizationSolutionHandler;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.TupleTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePose;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.exceptions.NoConvergenceException;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.FAST})
public class SimpleICPOptimizationSolutionHandlerTest
{
   private YoVariableRegistry registry = new YoVariableRegistry("robert");

   private ICPOptimizationParameters parameters;
   private SimpleICPOptimizationSolutionHandler solutionHandler;
   private SimpleICPOptimizationQPSolver solver;

   private void setupTest(double deadbandSize)
   {
      setupTest(deadbandSize, 0.02);
   }

   private void setupTest(double deadbandSize, double resolution)
   {
      parameters = new TestICPOptimizationParameters(deadbandSize, resolution);
      solutionHandler = new SimpleICPOptimizationSolutionHandler(parameters, false, "test", registry);
      solver = new SimpleICPOptimizationQPSolver(parameters, 4, false);
   }

   private Footstep createFootsteps(double length, double width, int numberOfSteps)
   {
      ArrayList<Footstep> upcomingFootsteps = new ArrayList<>();
      RobotSide robotSide = RobotSide.LEFT;
      for (int i = 0; i < numberOfSteps; i++)
      {
         FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame());

         footstepPose.setPosition(length * (i + 1), robotSide.negateIfRightSide(0.5 * width), 0.0);
         upcomingFootsteps.add(new Footstep(robotSide, footstepPose, false));

         FramePoint2D referenceFootstepPosition = new FramePoint2D();
         footstepPose.getPosition2dIncludingFrame(referenceFootstepPosition);

         robotSide = robotSide.getOppositeSide();
      }

      return upcomingFootsteps.get(0);
   }

   private FramePoint2D createReferenceLocations(Footstep upcomingFootstep)
   {
      FramePoint2D referenceLocation = new FramePoint2D();
      upcomingFootstep.getPosition2d(referenceLocation);

      return referenceLocation;
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testWellWithinDeadband()
   {
      double scale = 0.2;
      double deadbandSize = 0.05;
      runDeadbandTest(scale, deadbandSize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testALittleWithinDeadband()
   {
      double scale = 0.9;
      double deadbandSize = 0.05;
      runDeadbandTest(scale, deadbandSize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testJustWithinDeadband()
   {
      double scale = 0.99;
      double deadbandSize = 0.05;
      runDeadbandTest(scale, deadbandSize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testRightOnDeadband()
   {
      double scale = 1.0;
      double deadbandSize = 0.05;
      runDeadbandTest(scale, deadbandSize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testJustOutsideDeadband()
   {
      double scale = 1.01;
      double deadbandSize = 0.05;
      runDeadbandTest(scale, deadbandSize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testALittleOutsideDeadband()
   {
      double scale = 1.05;
      double deadbandSize = 0.05;
      runDeadbandTest(scale, deadbandSize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testWellOutsideDeadband()
   {
      double scale = 1.5;
      double deadbandSize = 0.05;
      runDeadbandTest(scale, deadbandSize);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testWithinDeadbandResolution() throws NoConvergenceException
   {
      double scale = 1.1;
      double deadbandSize = 0.05;
      double deadbandResolution = 0.02;

      setupTest(deadbandSize, deadbandResolution);

      double stepLength = 0.5;
      double stanceWidth = 0.2;
      int numberOfSteps = 3;
      YoFramePose foostepSolution = new YoFramePose("footstepSolution", ReferenceFrame.getWorldFrame(), registry);
      YoFramePoint2d unclippedFootstepSolution = new YoFramePoint2d("unclippedFootstepSolution", ReferenceFrame.getWorldFrame(), registry);
      FramePose foostepPose = new FramePose();
      FramePoint2D foostepXYSolution = new FramePoint2D();

      Footstep upcomingFootstep = createFootsteps(stepLength, stanceWidth, numberOfSteps);
      FramePoint2D referenceFootstepPosition = createReferenceLocations(upcomingFootstep);

      double recursionMultiplier = Math.exp(-3.0 * 0.5);
      setupFeedbackTask(10000.0, stanceWidth);
      setupFootstepAdjustmentTask(5.0, recursionMultiplier, referenceFootstepPosition);

      FrameVector2D currentICPError = new FrameVector2D(ReferenceFrame.getWorldFrame(), scale * deadbandSize, 0.0);
      currentICPError.scale(recursionMultiplier);

      FramePoint2D perfectCMP = new FramePoint2D(ReferenceFrame.getWorldFrame(), -0.1, 0.0);
      solver.compute(currentICPError, perfectCMP);

      solutionHandler.extractFootstepSolution(foostepSolution, unclippedFootstepSolution, upcomingFootstep, 1, solver);
      FrameVector2D copFeedback = new FrameVector2D();
      solver.getCoPFeedbackDifference(copFeedback);

      // first solution should be just outside the deadband
      FramePoint2D expectedUnclippedSolution = new FramePoint2D(referenceFootstepPosition);
      expectedUnclippedSolution.add(scale * deadbandSize, 0.0);
      FramePoint2D expectedClippedSolution = new FramePoint2D(referenceFootstepPosition);


      FrameVector2D clippedAdjustment = new FrameVector2D(ReferenceFrame.getWorldFrame(), (scale - 1.0) * deadbandSize, 0.0);
      expectedClippedSolution.add(clippedAdjustment);

      FrameVector2D adjustment = new FrameVector2D();
      adjustment.set(scale * deadbandSize, 0.0);

      foostepSolution.getFramePose(foostepPose);
      foostepPose.getPosition2dIncludingFrame(foostepXYSolution);

      assertTrue(foostepXYSolution.epsilonEquals(expectedClippedSolution, 1e-3));
      assertTrue(unclippedFootstepSolution.epsilonEquals(expectedUnclippedSolution, 1e-3));
      assertEquals(true, solutionHandler.wasFootstepAdjusted());
      assertTrue(TupleTools.epsilonEquals(clippedAdjustment, solutionHandler.getClippedFootstepAdjustment(), 1e-3));
      assertTrue(TupleTools.epsilonEquals(adjustment, solutionHandler.getFootstepAdjustment(), 1e-3));

      // this should now produce a test within the next adjustment
      currentICPError = new FrameVector2D(ReferenceFrame.getWorldFrame(), scale * deadbandSize + 0.5 * deadbandResolution, 0.0);
      currentICPError.scale(recursionMultiplier);

      solver.compute(currentICPError, perfectCMP);

      solutionHandler.extractFootstepSolution(foostepSolution, unclippedFootstepSolution, upcomingFootstep, 1, solver);

      // new solution should be clipped to the same value
      expectedUnclippedSolution = new FramePoint2D(referenceFootstepPosition);
      expectedUnclippedSolution.add(scale * deadbandSize + 0.5 * deadbandResolution, 0.0);

      adjustment = new FrameVector2D();
      adjustment.set(scale * deadbandSize + 0.5 * deadbandResolution, 0.0);

      foostepSolution.getFramePose(foostepPose);
      foostepPose.getPosition2dIncludingFrame(foostepXYSolution);

      assertTrue(foostepXYSolution.epsilonEquals(expectedClippedSolution, 1e-3));
      assertTrue(unclippedFootstepSolution.epsilonEquals(expectedUnclippedSolution, 1e-3));
      assertEquals(false, solutionHandler.wasFootstepAdjusted());
      assertTrue(TupleTools.epsilonEquals(clippedAdjustment, solutionHandler.getClippedFootstepAdjustment(), 1e-3));
      assertTrue(TupleTools.epsilonEquals(adjustment, solutionHandler.getFootstepAdjustment(), 1e-3));

      // test zeroing stuff out
      solutionHandler.zeroAdjustment();
      assertFalse(solutionHandler.wasFootstepAdjusted());
      assertEquals(0.0, solutionHandler.getFootstepAdjustment().length(), 1e-3);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testOutsideDeadbandResolution() throws NoConvergenceException
   {
      double scale = 1.1;
      double deadbandSize = 0.05;
      double deadbandResolution = 0.02;

      setupTest(deadbandSize, deadbandResolution);

      double stepLength = 0.5;
      double stanceWidth = 0.2;
      int numberOfSteps = 3;
      YoFramePose foostepSolution = new YoFramePose("footstepSolution", ReferenceFrame.getWorldFrame(), registry);
      YoFramePoint2d unclippedFootstepSolution = new YoFramePoint2d("unclippedFootstepSolution", ReferenceFrame.getWorldFrame(), registry);
      FramePose footstepPose = new FramePose();
      FramePoint2D footstepXYSolution = new FramePoint2D();

      Footstep upcomingFootstep = createFootsteps(stepLength, stanceWidth, numberOfSteps);
      FramePoint2D referenceFootstepPosition = createReferenceLocations(upcomingFootstep);

      double recursionMultiplier = Math.exp(-3.0 * 0.5);
      setupFeedbackTask(10000.0, stanceWidth);
      setupFootstepAdjustmentTask(5.0, recursionMultiplier, referenceFootstepPosition);

      FrameVector2D currentICPError = new FrameVector2D(ReferenceFrame.getWorldFrame(), scale * deadbandSize, 0.0);
      currentICPError.scale(recursionMultiplier);

      FramePoint2D perfectCMP = new FramePoint2D(ReferenceFrame.getWorldFrame(), -0.1, 0.0);
      solver.compute(currentICPError, perfectCMP);

      solutionHandler.extractFootstepSolution(foostepSolution, unclippedFootstepSolution, upcomingFootstep, 1, solver);
      FrameVector2D copFeedback = new FrameVector2D();
      solver.getCoPFeedbackDifference(copFeedback);

      // first solution should be just outside the deadband
      FramePoint2D expectedUnclippedSolution = new FramePoint2D(referenceFootstepPosition);
      expectedUnclippedSolution.add(scale * deadbandSize, 0.0);
      FramePoint2D expectedClippedSolution = new FramePoint2D(referenceFootstepPosition);


      FrameVector2D clippedAdjustment = new FrameVector2D(ReferenceFrame.getWorldFrame(), (scale - 1.0) * deadbandSize, 0.0);
      expectedClippedSolution.add(clippedAdjustment);

      FrameVector2D adjustment = new FrameVector2D();
      adjustment.set(scale * deadbandSize, 0.0);

      foostepSolution.getFramePose(footstepPose);
      footstepPose.getPosition2dIncludingFrame(footstepXYSolution);

      assertTrue(footstepXYSolution.epsilonEquals(expectedClippedSolution, 1e-3));
      assertTrue(unclippedFootstepSolution.epsilonEquals(expectedUnclippedSolution, 1e-3));
      assertEquals(true, solutionHandler.wasFootstepAdjusted());
      assertTrue(TupleTools.epsilonEquals(clippedAdjustment, solutionHandler.getClippedFootstepAdjustment(), 1e-3));
      assertTrue(TupleTools.epsilonEquals(adjustment, solutionHandler.getFootstepAdjustment(), 1e-3));

      // this should now produce a test within the next adjustment
      currentICPError = new FrameVector2D(ReferenceFrame.getWorldFrame(), scale * deadbandSize + 1.5 * deadbandResolution, 0.0);
      currentICPError.scale(recursionMultiplier);

      solver.compute(currentICPError, perfectCMP);

      solutionHandler.extractFootstepSolution(foostepSolution, unclippedFootstepSolution, upcomingFootstep, 1, solver);

      // new solution should be clipped to the same value
      expectedUnclippedSolution = new FramePoint2D(referenceFootstepPosition);
      adjustment = new FrameVector2D(ReferenceFrame.getWorldFrame(), scale * deadbandSize + 1.5 * deadbandResolution, 0.0);
      expectedUnclippedSolution.add(adjustment);

      expectedClippedSolution = new FramePoint2D(referenceFootstepPosition);
      clippedAdjustment = new FrameVector2D(ReferenceFrame.getWorldFrame(), scale * deadbandSize + 1.5 * deadbandResolution - deadbandSize, 0.0);
      expectedClippedSolution.add(clippedAdjustment);

      foostepSolution.getFramePose(footstepPose);
      footstepPose.getPosition2dIncludingFrame(footstepXYSolution);

      assertTrue(footstepXYSolution.epsilonEquals(expectedClippedSolution, 1e-3));
      assertTrue(unclippedFootstepSolution.epsilonEquals(expectedUnclippedSolution, 1e-3));
      assertEquals(true, solutionHandler.wasFootstepAdjusted());
      assertTrue(TupleTools.epsilonEquals(clippedAdjustment, solutionHandler.getClippedFootstepAdjustment(), 1e-3));
      assertTrue(TupleTools.epsilonEquals(adjustment, solutionHandler.getFootstepAdjustment(), 1e-3));

      // test zeroing stuff out
      solutionHandler.zeroAdjustment();
      assertFalse(solutionHandler.wasFootstepAdjusted());
      assertEquals(0.0, solutionHandler.getFootstepAdjustment().length(), 1e-3);
   }


   private void runDeadbandTest(double scale, double deadbandSize)
   {
      setupTest(deadbandSize);

      double stepLength = 0.5;
      double stanceWidth = 0.2;
      int numberOfSteps = 3;

      YoFramePose footstepSolution = new YoFramePose("footstepSolution", ReferenceFrame.getWorldFrame(), registry);
      YoFramePoint2d unclippedFootstepSolution = new YoFramePoint2d("unclippedFootstepSolution", ReferenceFrame.getWorldFrame(), registry);
      FramePose footstepPose = new FramePose();
      FramePoint2D footstepXYSolution = new FramePoint2D();

      Footstep upcomingFootstep = createFootsteps(stepLength, stanceWidth, numberOfSteps);
      FramePoint2D referenceFootstepPosition = createReferenceLocations(upcomingFootstep);

      double recursionMultiplier = Math.exp(-3.0 * 0.5);
      setupFeedbackTask(10000.0, stanceWidth);
      setupFootstepAdjustmentTask(5.0, recursionMultiplier, referenceFootstepPosition);

      FrameVector2D currentICPError = new FrameVector2D(ReferenceFrame.getWorldFrame(), scale * deadbandSize, 0.0);
      currentICPError.scale(recursionMultiplier);

      FramePoint2D perfectCMP = new FramePoint2D(ReferenceFrame.getWorldFrame(), -0.1, 0.0);
      try
      {
         solver.compute(currentICPError, perfectCMP);
      }
      catch (NoConvergenceException e)
      {
      }

      solutionHandler.extractFootstepSolution(footstepSolution, unclippedFootstepSolution, upcomingFootstep, 1, solver);
      FrameVector2D copFeedback = new FrameVector2D();
      solver.getCoPFeedbackDifference(copFeedback);

      // this should be within the deadband
      FramePoint2D expectedUnclippedSolution = new FramePoint2D(referenceFootstepPosition);
      expectedUnclippedSolution.add(scale * deadbandSize, 0.0);
      FramePoint2D expectedClippedSolution = new FramePoint2D(referenceFootstepPosition);

      boolean wasAdjusted = scale > 1.0;

      FrameVector2D clippedAdjustment = new FrameVector2D();
      if (wasAdjusted)
         clippedAdjustment.set((scale - 1.0) * deadbandSize, 0.0);
      expectedClippedSolution.add(clippedAdjustment);

      FrameVector2D adjustment = new FrameVector2D();
      adjustment.set(scale * deadbandSize, 0.0);

      footstepSolution.getFramePose(footstepPose);
      footstepPose.getPosition2dIncludingFrame(footstepXYSolution);

      assertTrue(footstepXYSolution.epsilonEquals(expectedClippedSolution, 1e-3));
      assertTrue(unclippedFootstepSolution.epsilonEquals(expectedUnclippedSolution, 1e-3));
      assertEquals(wasAdjusted, solutionHandler.wasFootstepAdjusted());
      assertTrue(TupleTools.epsilonEquals(clippedAdjustment, solutionHandler.getClippedFootstepAdjustment(), 1e-3));
      assertTrue(TupleTools.epsilonEquals(adjustment, solutionHandler.getFootstepAdjustment(), 1e-3));

      // test zeroing stuff out
      solutionHandler.zeroAdjustment();
      assertFalse(solutionHandler.wasFootstepAdjusted());
      assertEquals(0.0, solutionHandler.getFootstepAdjustment().length(), 1e-3);
   }

   private void setupFeedbackTask(double weight, double stanceWidth)
   {
      double footLength = 0.2;
      double footWidth = 0.1;

      FrameConvexPolygon2d supportPolygon = new FrameConvexPolygon2d();
      supportPolygon.addVertex(new Point2D(0.5 * footLength, 0.5 * footWidth - 0.5 * stanceWidth));
      supportPolygon.addVertex(new Point2D(0.5 * footLength, -0.5 * footWidth - 0.5 * stanceWidth));
      supportPolygon.addVertex(new Point2D(-0.5 * footLength, 0.5 * footWidth - 0.5 * stanceWidth));
      supportPolygon.addVertex(new Point2D(-0.5 * footLength, -0.5 * footWidth - 0.5 * stanceWidth));
      supportPolygon.update();

      double feedbackGain = 2.5;
      double dynamicsWeight = 100000.0;

      solver.setFeedbackConditions(feedbackGain, weight, dynamicsWeight);
      solver.addSupportPolygon(supportPolygon);
   }

   private void setupFootstepAdjustmentTask(double weight, double recursionMultiplier, FramePoint2D referenceFootstepPosition)
   {
      solver.setFootstepAdjustmentConditions(recursionMultiplier, weight, referenceFootstepPosition);
   }


   private class TestICPOptimizationParameters extends ICPOptimizationParameters
   {
      private final double deadbandSize;
      private final double resolution;

      public TestICPOptimizationParameters(double deadbandSize, double resolution)
      {
         this.deadbandSize = deadbandSize;
         this.resolution = resolution;
      }

      @Override public boolean useSimpleOptimization()
      {
         return false;
      }

      @Override public int getMaximumNumberOfFootstepsToConsider()
      {
         return 5;
      }

      @Override public int numberOfFootstepsToConsider()
      {
         return 0;
      }

      @Override public double getForwardFootstepWeight()
      {
         return 5.0;
      }

      @Override public double getLateralFootstepWeight()
      {
         return 5.0;
      }

      @Override public double getFootstepRegularizationWeight()
      {
         return 0.0001;
      }

      @Override public double getFeedbackForwardWeight()
      {
         return 2.0;
      }

      @Override public double getFeedbackLateralWeight()
      {
         return 2.0;
      }

      @Override public double getFeedbackRegularizationWeight()
      {
         return 0.0001;
      }

      @Override public double getFeedbackParallelGain()
      {
         return 2.0;
      }

      @Override public double getFeedbackOrthogonalGain()
      {
         return 3.0;
      }

      @Override public double getDynamicRelaxationWeight()
      {
         return 1000.0;
      }

      @Override public double getDynamicRelaxationDoubleSupportWeightModifier()
      {
         return 1.0;
      }

      @Override public double getAngularMomentumMinimizationWeight()
      {
         return 0.5;
      }

      @Override public boolean scaleStepRegularizationWeightWithTime()
      {
         return false;
      }

      @Override public boolean scaleFeedbackWeightWithGain()
      {
         return false;
      }

      @Override public boolean scaleUpcomingStepWeights()
      {
         return false;
      }

      @Override public boolean useFeedbackRegularization()
      {
         return false;
      }

      @Override public boolean useStepAdjustment()
      {
         return true;
      }

      @Override public boolean useTimingOptimization()
      {
         return false;
      }

      @Override public boolean useAngularMomentum()
      {
         return false;
      }

      @Override public boolean useFootstepRegularization()
      {
         return false;
      }

      @Override public double getMinimumFootstepWeight()
      {
         return 0.0001;
      }

      @Override public double getMinimumFeedbackWeight()
      {
         return 0.0001;
      }

      @Override
      public double getFootstepSolutionResolution()
      {
         return resolution;
      }
      @Override
      public double getMinimumTimeRemaining()
      {
         return 0.001;
      }

      @Override public double getAdjustmentDeadband()
      {
         return deadbandSize;
      }
   };
}
