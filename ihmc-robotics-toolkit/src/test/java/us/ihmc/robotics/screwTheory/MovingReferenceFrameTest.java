package us.ihmc.robotics.screwTheory;

import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Test;

import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.mecano.multiBodySystem.OneDoFJoint;
import us.ihmc.mecano.multiBodySystem.PrismaticJoint;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.Twist;
import us.ihmc.mecano.tools.JointStateType;
import us.ihmc.mecano.tools.MultiBodySystemRandomTools;

public class MovingReferenceFrameTest
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   @After
   public void tearDown()
   {
      ReferenceFrameTools.clearWorldFrameTree();
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAgainstTwistCalculatorWithPrismaticChainRobot() throws Exception
   {
      Random random = new Random(435423L);

      int numberOfJoints = 100;
      List<PrismaticJoint> joints = MultiBodySystemRandomTools.nextPrismaticJointChain(random, numberOfJoints);

      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, joints.get(0).getPredecessor());
      Twist actualTwist = new Twist();
      Twist expectedTwist = new Twist();

      for (int i = 0; i < 100; i++)
      {
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -1.0, 1.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, -1.0, 1.0, joints);
         joints.get(0).getPredecessor().updateFramesRecursively();

         twistCalculator.compute();

         for (int jointIndex = 0; jointIndex < numberOfJoints; jointIndex++)
         {
            OneDoFJoint joint = joints.get(jointIndex);
            RigidBodyBasics body = joint.getSuccessor();
            MovingReferenceFrame bodyFrame = body.getBodyFixedFrame();

            twistCalculator.getTwistOfBody(body, expectedTwist);
            bodyFrame.getTwistOfFrame(actualTwist);
            TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-5);
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAgainstTwistCalculatorWithChainRobot() throws Exception
   {
      Random random = new Random(435423L);

      int numberOfJoints = 100;
      List<OneDoFJoint> joints = MultiBodySystemRandomTools.nextOneDoFJointChain(random, numberOfJoints);

      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, joints.get(0).getPredecessor());
      Twist actualTwist = new Twist();
      Twist expectedTwist = new Twist();

      for (int i = 0; i < 100; i++)
      {
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -1.0, 1.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, -1.0, 1.0, joints);
         joints.get(0).getPredecessor().updateFramesRecursively();

         twistCalculator.compute();

         for (int jointIndex = 0; jointIndex < numberOfJoints; jointIndex++)
         {
            OneDoFJoint joint = joints.get(jointIndex);
            RigidBodyBasics body = joint.getSuccessor();
            MovingReferenceFrame bodyFrame = body.getBodyFixedFrame();

            twistCalculator.getTwistOfBody(body, expectedTwist);
            bodyFrame.getTwistOfFrame(actualTwist);
            TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-5);
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAgainstTwistCalculatorWithTreeRobot() throws Exception
   {
      Random random = new Random(435423L);

      int numberOfJoints = 100;
      List<OneDoFJoint> joints = MultiBodySystemRandomTools.nextOneDoFJointTree(random, numberOfJoints);

      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, joints.get(0).getPredecessor());
      Twist actualTwist = new Twist();
      Twist expectedTwist = new Twist();

      for (int i = 0; i < 100; i++)
      {
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -1.0, 1.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, -1.0, 1.0, joints);
         joints.get(0).getPredecessor().updateFramesRecursively();

         twistCalculator.compute();

         for (int jointIndex = 0; jointIndex < numberOfJoints; jointIndex++)
         {
            OneDoFJoint joint = joints.get(jointIndex);
            RigidBodyBasics body = joint.getSuccessor();
            MovingReferenceFrame bodyFrame = body.getBodyFixedFrame();

            twistCalculator.getTwistOfBody(body, expectedTwist);
            bodyFrame.getTwistOfFrame(actualTwist);
            TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-5);

            twistCalculator.getTwistOfBody(body, expectedTwist);
            expectedTwist.setBodyFrame(joint.getFrameAfterJoint());
            expectedTwist.changeFrame(joint.getFrameAfterJoint());
            joint.getFrameAfterJoint().getTwistOfFrame(actualTwist);
            TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-5);

            for (JointBasics child : body.getChildrenJoints())
            {
               twistCalculator.getTwistOfBody(body, expectedTwist);
               expectedTwist.setBodyFrame(child.getFrameBeforeJoint());
               expectedTwist.changeFrame(child.getFrameBeforeJoint());
               child.getFrameBeforeJoint().getTwistOfFrame(actualTwist);
               TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-5);
            }
         }
      }
   }
}
