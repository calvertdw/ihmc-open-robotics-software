package us.ihmc.robotics.screwTheory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ejml.data.DenseMatrix64F;
import org.junit.Before;
import org.junit.Test;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.mecano.multiBodySystem.PlanarJoint;
import us.ihmc.mecano.multiBodySystem.PrismaticJoint;
import us.ihmc.mecano.multiBodySystem.RevoluteJoint;
import us.ihmc.mecano.multiBodySystem.RigidBody;
import us.ihmc.mecano.multiBodySystem.SixDoFJoint;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.SpatialAcceleration;
import us.ihmc.mecano.tools.JointStateType;
import us.ihmc.mecano.tools.MultiBodySystemRandomTools;
import us.ihmc.mecano.tools.MultiBodySystemRandomTools.RandomFloatingRevoluteJointChain;
import us.ihmc.mecano.tools.MultiBodySystemTools;
import us.ihmc.robotics.random.RandomGeometry;

public class ScrewToolsTest
{
   private static final Vector3D X = new Vector3D(1.0, 0.0, 0.0);
   private static final Vector3D Y = new Vector3D(0.0, 1.0, 0.0);
   private static final Vector3D Z = new Vector3D(0.0, 0.0, 1.0);

   private RigidBodyBasics elevator;
   private Random random;
   private List<RigidBodyBasics> firstLevelSubTrees;
   private List<RigidBodyBasics> secondLevelSubTrees;
   private Set<RigidBodyBasics> exclusions;
   private Set<JointBasics> exclusionsJoints;
   private ArrayList<RevoluteJoint> joints;

   protected static final double epsilon = 1e-10;
   protected ReferenceFrame theFrame = ReferenceFrame.constructARootFrame("theFrame");
   protected ReferenceFrame aFrame = ReferenceFrame.constructARootFrame("aFrame");

   @Before
   public void setUp()
   {
      elevator = new RigidBody("elevator", ReferenceFrame.getWorldFrame());
      random = new Random(1986L);

      setUpRandomTree(elevator);

      firstLevelSubTrees = new ArrayList<RigidBodyBasics>();

      for (JointBasics childJoint : elevator.getChildrenJoints())
      {
         firstLevelSubTrees.add(childJoint.getSuccessor());
      }

      secondLevelSubTrees = new ArrayList<RigidBodyBasics>();
      for(int i = 0; i < 3; i++)
      {
         for (JointBasics childJoint : firstLevelSubTrees.get(i).getChildrenJoints())
         {
            secondLevelSubTrees.add(childJoint.getSuccessor());
         }
      }

      exclusions = new LinkedHashSet<RigidBodyBasics>();
      exclusionsJoints = new LinkedHashSet<JointBasics>();
      exclusions.add(firstLevelSubTrees.get(1));

      for (JointBasics excludedJoint : firstLevelSubTrees.get(1).getChildrenJoints())
      {
         exclusionsJoints.add(excludedJoint);
      }

      JointBasics[] subtreeJoints = ScrewTools.computeSubtreeJoints(firstLevelSubTrees.get(2));
      RigidBodyBasics[] lastSubTree = MultiBodySystemTools.collectSuccessors(subtreeJoints);
      RigidBodyBasics halfwayDownLastSubTree = lastSubTree[3];
      exclusions.add(halfwayDownLastSubTree);

      for (JointBasics excludedJoint : halfwayDownLastSubTree.getChildrenJoints())
      {
         exclusionsJoints.add(excludedJoint);
      }
   }

   private void setUpRandomTree(RigidBodyBasics elevator)
   {
      joints = new ArrayList<RevoluteJoint>();

      Vector3D[] jointAxes1 = {X, Y, Z, Y, X};
      joints.addAll(MultiBodySystemRandomTools.nextRevoluteJointChain(random, "chainA", elevator, jointAxes1));

      Vector3D[] jointAxes2 = {Z, X, Y, X, X};
      joints.addAll(MultiBodySystemRandomTools.nextRevoluteJointChain(random, "chainB", elevator, jointAxes2));

      Vector3D[] jointAxes3 = {Y, Y, X, X, X};
      joints.addAll(MultiBodySystemRandomTools.nextRevoluteJointChain(random, "chainC", elevator, jointAxes3));
   }

   private Set<RigidBodyBasics> getExcludedRigidBodies()
   {
      Set<RigidBodyBasics> excludedBodies = new LinkedHashSet<RigidBodyBasics>();
      for (RigidBodyBasics rigidBody : exclusions)
      {
         excludedBodies.add(rigidBody);
         RigidBodyBasics[] subTree = MultiBodySystemTools.collectSuccessors(ScrewTools.computeSubtreeJoints(rigidBody));
         excludedBodies.addAll(Arrays.asList(subTree));
      }

      return excludedBodies;
   }

   private Set<JointBasics> getExcludedJoints()
   {
      Set<RigidBodyBasics> excludedBodies = getExcludedRigidBodies();
      Set<JointBasics> excludedJoints = new LinkedHashSet<JointBasics>();
      for (RigidBodyBasics rigidBody : excludedBodies)
      {
         excludedJoints.addAll(rigidBody.getChildrenJoints());
      }

      return excludedJoints;
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAddRevoluteJoint_String_RigidBody_Vector3d_Vector3d()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      chain.nextState(random, JointStateType.CONFIGURATION, JointStateType.VELOCITY);

      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());
      RigidBodyBasics[] partialBodiesArray = ScrewTools.computeSubtreeSuccessors(chain.getElevator());
      RigidBodyBasics[] bodiesArray = new RigidBodyBasics[partialBodiesArray.length + 1];
      bodiesArray[0] = chain.getElevator();
      for(int i = 0; i < partialBodiesArray.length; i++)
      {
         bodiesArray[i+1] = partialBodiesArray[i];
      }

      String jointName = "joint";
      RigidBodyBasics parentBody = bodiesArray[bodiesArray.length - 1];
      Vector3D jointOffset = RandomGeometry.nextVector3D(random, 5.0);
      Vector3D jointAxis = RandomGeometry.nextVector3D(random, 5.0);

      RevoluteJoint joint = new RevoluteJoint(jointName, parentBody, jointOffset, jointAxis);

      assertEquals("Should be equal", jointName, joint.getName());
      assertTrue(parentBody.equals(joint.getPredecessor()));
      assertTrue(jointAxis.equals(joint.getJointAxis()));
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAddRevoluteJoint_String_RigidBody_Transform3D_Vector3d()
   {
      String jointName = "joint";
      RigidBodyBasics parentBody = new RigidBody("body", ReferenceFrame.getWorldFrame());
      RigidBodyTransform transformToParent = EuclidCoreRandomTools.nextRigidBodyTransform(random);
      Vector3D jointAxis = RandomGeometry.nextVector3D(random, 5.0);

      RevoluteJoint joint = new RevoluteJoint(jointName, parentBody, transformToParent, jointAxis);

      assertEquals("Should be equal", jointName, joint.getName());
      assertTrue(parentBody.equals(joint.getPredecessor()));
      assertTrue(jointAxis.equals(joint.getJointAxis()));
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAddPrismaticJoint_String_RigidBody_Vector3d_Vector3d()
   {
      String jointName = "joint";
      RigidBodyBasics parentBody = new RigidBody("body", ReferenceFrame.getWorldFrame());
      Vector3D jointOffset = RandomGeometry.nextVector3D(random, 5.0);
      Vector3D jointAxis = RandomGeometry.nextVector3D(random, 5.0);

      PrismaticJoint joint = new PrismaticJoint(jointName, parentBody, jointOffset, jointAxis);

      assertEquals("Should be equal", jointName, joint.getName());
      assertTrue(parentBody.equals(joint.getPredecessor()));
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAddPrismaticJoint_String_RigidBody_Transform3D_Vector3d()
   {
      String jointName = "joint";
      RigidBodyBasics parentBody = new RigidBody("body", ReferenceFrame.getWorldFrame());
      RigidBodyTransform transformToParent = EuclidCoreRandomTools.nextRigidBodyTransform(random);
      Vector3D jointAxis = RandomGeometry.nextVector3D(random, 5.0);

      PrismaticJoint joint = new PrismaticJoint(jointName, parentBody, transformToParent, jointAxis);

      assertEquals("Should be equal", jointName, joint.getName());
      assertTrue(parentBody.equals(joint.getPredecessor()));
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAddRigidBody_String_InverseDynamicsJoint_Matrix3d_double_Vector3d()
   {
      String name = "body";
      RigidBodyBasics predecessor = new RigidBody("Predecessor", theFrame);
      PlanarJoint parentJoint = new PlanarJoint(name, predecessor);
      Matrix3D momentOfInertia = new Matrix3D();
      double mass = random.nextDouble();

      RigidBodyBasics body = new RigidBody(name, parentJoint, momentOfInertia, mass, X);

      assertEquals("Should be equal", name, body.getName());
      assertTrue(parentJoint.equals(body.getParentJoint()));
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAddRigidBody_String_InverseDynamicsJoint_Matrix3d_double_Transform3D()
   {
      String name = "body";
      RigidBodyBasics predecessor = new RigidBody("Predecessor", theFrame);
      PlanarJoint parentJoint = new PlanarJoint(name, predecessor);
      Matrix3D momentOfInertia = new Matrix3D();
      double mass = random.nextDouble();
      RigidBodyTransform inertiaPose = new RigidBodyTransform();

      RigidBodyBasics body = new RigidBody(name, parentJoint, momentOfInertia, mass, inertiaPose);

      assertEquals("Should be equal", name, body.getName());
      assertTrue(parentJoint.equals(body.getParentJoint()));
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeSuccessors()
   {  
      int numJoints = 3;
      RigidBodyBasics[] bodyArray = new RigidBodyBasics[numJoints];
      for (int i = 0; i < numJoints; i++)
      {
         JointBasics joint = joints.get(i);
         bodyArray[i] = joint.getSuccessor();
      }
      JointBasics[] joints1 = {joints.get(0), joints.get(1), joints.get(2)};

      RigidBodyBasics[] bodies = MultiBodySystemTools.collectSuccessors(joints1);

      assertEquals("Should be equal", bodyArray.length, bodies.length);
      for(int i = 0; i < bodies.length; i++)
      {
         assertTrue(bodies[i].equals(bodyArray[i]));
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeSupportAndSubtreeSuccessors_RigidBody()
   {
      int numberOfBodiesOnChain = 6;
      int numberOfBodies = 16;
      RigidBodyBasics[] successors = ScrewTools.computeSupportAndSubtreeSuccessors(secondLevelSubTrees.get(0));
      assertEquals(numberOfBodiesOnChain - 1, successors.length);

      successors = ScrewTools.computeSupportAndSubtreeSuccessors(elevator);
      assertEquals(numberOfBodies - 1, successors.length);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeSupportAndSubtreeJoints_RigidBody()
   {
      int numberOfJointsOnChain = 5;
      int numberOfJoints = 15;
      JointBasics [] successors = ScrewTools.computeSupportAndSubtreeJoints(secondLevelSubTrees.get(0));
      assertEquals(numberOfJointsOnChain, successors.length);

      successors = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      assertEquals(numberOfJoints, successors.length);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeSupportJoints_RigidBody()
   {
      JointBasics[] supportJoints = ScrewTools.computeSupportJoints(elevator);
      assertTrue(elevator.isRootBody());
      assertEquals(0, supportJoints.length);

      int jointsSupportingSecondLevelSubTree = 2, numberOfChainsUsed = 2;

      supportJoints = ScrewTools.computeSupportJoints(secondLevelSubTrees.get(0), secondLevelSubTrees.get(1));

      assertEquals(jointsSupportingSecondLevelSubTree * numberOfChainsUsed, supportJoints.length);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeSubtreeJoints_RigidBody()
   {
      List<RigidBodyBasics> bodies = new ArrayList<RigidBodyBasics>();
      bodies.add(elevator);
      bodies.add(elevator);

      JointBasics[] fromBodies = ScrewTools.computeSubtreeJoints(elevator, elevator);
      JointBasics[] fromBodiesList = ScrewTools.computeSubtreeJoints(bodies);

      assertEquals("These should be equal", fromBodies.length, fromBodiesList.length);
      for(int i = 0; i < fromBodies.length; i++)
      {
         assertTrue(fromBodies[i].equals(fromBodiesList[i]));
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeSubtreeJoints_RigidBodyLIST()
   {
      ArrayList<RigidBodyBasics> rootBodies = new ArrayList<RigidBodyBasics>();
      rootBodies.add(elevator);
      JointBasics[] subtreeJoints = ScrewTools.computeSubtreeJoints(rootBodies);

      ArrayList<JointBasics> subtree = new ArrayList<JointBasics>();
      ArrayList<RigidBodyBasics> rigidBodyStack = new ArrayList<RigidBodyBasics>();
      rigidBodyStack.addAll(rootBodies);

      while (!rigidBodyStack.isEmpty())
      {
         RigidBodyBasics currentBody = rigidBodyStack.remove(0);
         List<? extends JointBasics> childrenJoints = currentBody.getChildrenJoints();
         for (JointBasics joint : childrenJoints)
         {
            RigidBodyBasics successor = joint.getSuccessor();
            rigidBodyStack.add(successor);
            subtree.add(joint);
         }
      }

      assertEquals("These should be equal", subtreeJoints.length, subtree.size());
      for(int i = 0; i < subtreeJoints.length; i++)
      {
         assertTrue(subtreeJoints[i].equals(subtree.get(i)));
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetRootBody()
   {
      RigidBodyBasics randomBody = MultiBodySystemTools.getRootBody(joints.get(joints.size() - 1).getPredecessor());
      assertTrue(randomBody.isRootBody());
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testCreateJointPath()
   {
      int numberOfJoints = joints.size(), numberOfBodies = numberOfJoints + 1;
      RigidBodyBasics[] allBodies = new RigidBodyBasics[numberOfBodies];
      allBodies[0] = elevator;
      for(int i = 0; i < numberOfJoints; i++)
      {
         allBodies[i+1] = joints.get(i).getSuccessor();
      }

      RigidBodyBasics start = allBodies[0] , end = allBodies[allBodies.length - 1];
      JointBasics[] jointPath = ScrewTools.createJointPath(start, end);
      for(int i = 0; i < jointPath.length; i++)
      {
         assertTrue(jointPath[i].getName().equalsIgnoreCase("chainCjoint" + i));
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsAncestor()
   {
      int numberOfJoints = joints.size(), numberOfBodies = numberOfJoints + 1;
      RigidBodyBasics[] allBodies = new RigidBodyBasics[numberOfBodies];
      allBodies[0] = elevator;
      for(int i = 0; i < numberOfJoints; i++)
      {
         allBodies[i+1] = joints.get(i).getSuccessor();
      }

      RigidBodyBasics d0 = allBodies[0]; //elevator
      RigidBodyBasics d1 = allBodies[1]; //chainAbody0
      RigidBodyBasics d2 = allBodies[2]; //chainAbody1
      RigidBodyBasics d3 = allBodies[3]; //chainAbody2

      assertTrue(MultiBodySystemTools.isAncestor(d0, d0)); //self
      assertTrue(MultiBodySystemTools.isAncestor(d3, d0)); //ancestor
      assertFalse(MultiBodySystemTools.isAncestor(d0, d3)); //descendant 
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeDistanceToAncestor()
   {
      int numberOfJoints = joints.size(), numberOfBodies = numberOfJoints + 1;
      RigidBodyBasics[] allBodies = new RigidBodyBasics[numberOfBodies];
      allBodies[0] = elevator;
      for(int i = 0; i < numberOfJoints; i++)
      {
         allBodies[i+1] = joints.get(i).getSuccessor();
      }

      RigidBodyBasics d0 = allBodies[0]; //elevator
      RigidBodyBasics d1 = allBodies[1]; //chainAbody0
      RigidBodyBasics d2 = allBodies[2]; //chainAbody1
      RigidBodyBasics d3 = allBodies[3]; //chainAbody2

      assertEquals(0, MultiBodySystemTools.computeDistanceToAncestor(d0, d0)); //self
      assertEquals(3, MultiBodySystemTools.computeDistanceToAncestor(d3, d0)); //ancestor
      assertEquals(-1, MultiBodySystemTools.computeDistanceToAncestor(d0, d3)); //descendant 
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testPackJointVelocitiesMatrix_Array()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());

      DenseMatrix64F originalVelocities = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);
      for(int i = 0; i < originalVelocities.getNumRows() * originalVelocities.getNumCols(); i++)
      {       //create original matrix
         originalVelocities.set(i, random.nextDouble());
      }
      MultiBodySystemTools.insertJointsState(jointsArray, JointStateType.VELOCITY, originalVelocities); //set velocities from matrix
      DenseMatrix64F newVelocities = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);

      MultiBodySystemTools.extractJointsState(jointsArray, JointStateType.VELOCITY, newVelocities);//pack new matrix
      for(int i = 0; i < jointsArray.length; i++)
      {
         assertEquals("Should be equal velocities", originalVelocities.get(i), newVelocities.get(i), epsilon);
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testPackJointVelocitiesMatrix_Iterable()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());
      ArrayList<JointBasics> jointsList = new ArrayList<JointBasics>();
      for(int i = 0; i < jointsArray.length; i++)
      {
         jointsList.add(jointsArray[i]);
      }

      DenseMatrix64F originalVelocities = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);
      for(int i = 0; i < originalVelocities.getNumRows() * originalVelocities.getNumCols(); i++)
      {       //create original matrix
         originalVelocities.set(i, random.nextDouble());
      }
      MultiBodySystemTools.insertJointsState(jointsArray, JointStateType.VELOCITY, originalVelocities); //set velocities from matrix
      DenseMatrix64F newVelocities = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);

      MultiBodySystemTools.extractJointsState(jointsList, JointStateType.VELOCITY, newVelocities);//pack new matrix
      for(int i = 0; i < jointsArray.length; i++)
      {
         assertEquals("Should be equal velocities", originalVelocities.get(i), newVelocities.get(i), epsilon);
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testPackDesiredJointAccelerationsMatrix()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());

      DenseMatrix64F originalAccel = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);
      for(int i = 0; i < originalAccel.getNumRows() * originalAccel.getNumCols(); i++)
      {       //create original matrix
         originalAccel.set(i, random.nextDouble());
      }
      MultiBodySystemTools.insertJointsState(jointsArray, JointStateType.ACCELERATION, originalAccel); //set velocities from matrix
      DenseMatrix64F newAccelerations = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);

      MultiBodySystemTools.extractJointsState(jointsArray, JointStateType.ACCELERATION, newAccelerations);//pack new matrix
      for(int i = 0; i < jointsArray.length; i++)
      {
         assertEquals("Should be equal velocities", originalAccel.get(i), newAccelerations.get(i), epsilon);
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeDegreesOfFreedom_Array()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      chain.nextState(random, JointStateType.CONFIGURATION, JointStateType.VELOCITY);

      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());
      RigidBodyBasics[] partialBodiesArray = ScrewTools.computeSubtreeSuccessors(chain.getElevator());
      RigidBodyBasics[] bodiesArray = new RigidBodyBasics[partialBodiesArray.length + 1];
      bodiesArray[0] = chain.getElevator();
      for(int i = 0; i < partialBodiesArray.length; i++)
      {
         bodiesArray[i+1] = partialBodiesArray[i];
      }

      int result = MultiBodySystemTools.computeDegreesOfFreedom(jointsArray);
      assertEquals(11, result);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeDegreesOfFreedom_Iterable()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      chain.nextState(random, JointStateType.CONFIGURATION, JointStateType.VELOCITY);

      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());
      ArrayList<JointBasics> jointsList = new ArrayList<JointBasics>(jointsArray.length);

      RigidBodyBasics[] partialBodiesArray = ScrewTools.computeSubtreeSuccessors(chain.getElevator());
      RigidBodyBasics[] bodiesArray = new RigidBodyBasics[partialBodiesArray.length + 1];
      bodiesArray[0] = chain.getElevator();
      for(int i = 0; i < partialBodiesArray.length; i++)
      {
         bodiesArray[i+1] = partialBodiesArray[i];
      }

      MultiBodySystemTools.computeDegreesOfFreedom(jointsList);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testCreateGravitationalSpatialAcceleration()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      chain.nextState(random, JointStateType.CONFIGURATION, JointStateType.VELOCITY);

      double gravity = RandomNumbers.nextDouble(random, 100.0);
      SpatialAcceleration result = ScrewTools.
            createGravitationalSpatialAcceleration(chain.getElevator(), gravity);

      Vector3DReadOnly angularPart = result.getAngularPart();
      Vector3D zeroes = new Vector3D(0.0, 0.0, 0.0);

      assertTrue(angularPart.epsilonEquals(zeroes, epsilon));

      Vector3DReadOnly linearPart = result.getLinearPart();
      assertEquals(zeroes.getX(), linearPart.getX(), epsilon);
      assertEquals(zeroes.getY(), linearPart.getY(), epsilon);
      assertEquals(gravity, linearPart.getZ(), epsilon);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetDesiredAccelerations()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());

      DenseMatrix64F jointAccelerations = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);
      for(int i = 0; i < jointAccelerations.getNumRows() * jointAccelerations.getNumCols(); i++)
      {
         jointAccelerations.set(i, random.nextDouble());
      }

      MultiBodySystemTools.insertJointsState(jointsArray, JointStateType.ACCELERATION, jointAccelerations);

      DenseMatrix64F sixDoFAccel = new DenseMatrix64F(6, 1);
      jointsArray[0].getJointAcceleration(0, sixDoFAccel);
      for(int i = 0; i < 6; i++)
      {
         assertEquals("Should be equal accelerations", jointAccelerations.get(i), sixDoFAccel.get(i), epsilon);
      }

      OneDoFJointBasics joint;

      for(int i = 6; i < jointAccelerations.getNumRows() * jointAccelerations.getNumCols(); i++)
      {
         joint = (OneDoFJointBasics)jointsArray[i - 5]; //1 - 6
         assertEquals("Should be equal accelerations", jointAccelerations.get(i), joint.getQdd(), epsilon);
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetVelocities()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArray = ScrewTools.computeSubtreeJoints(chain.getElevator());

      DenseMatrix64F jointVelocities = new DenseMatrix64F(MultiBodySystemTools.computeDegreesOfFreedom(jointsArray), 1);
      for(int i = 0; i < jointVelocities.getNumRows() * jointVelocities.getNumCols(); i++)
      {
         jointVelocities.set(i, random.nextDouble());
      }

      MultiBodySystemTools.insertJointsState(jointsArray, JointStateType.VELOCITY, jointVelocities);

      DenseMatrix64F sixDoFVeloc = new DenseMatrix64F(6, 1);
      jointsArray[0].getJointVelocity(0, sixDoFVeloc);
      for(int i = 0; i < 6; i++)
      {
         assertEquals("Should be equal velocitiess", jointVelocities.get(i), sixDoFVeloc.get(i), epsilon);
      }

      OneDoFJointBasics joint;

      for(int i = 6; i < jointVelocities.getNumRows() * jointVelocities.getNumCols(); i++)
      {
         joint = (OneDoFJointBasics)jointsArray[i - 5]; //1 - 6
         assertEquals("Should be equal velocities", jointVelocities.get(i), joint.getQd(), epsilon);
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeIndicesForJoint()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArr = ScrewTools.computeSubtreeJoints(chain.getElevator());
      JointBasics rootJoint = jointsArr[0];
      JointBasics testJoint4 = jointsArr[5];

      
      TIntArrayList indices = new TIntArrayList();
      ScrewTools.computeIndicesForJoint(jointsArr, indices, testJoint4, rootJoint);
      assertEquals(7, indices.size());

      for(int i = 0; i < rootJoint.getDegreesOfFreedom(); i++)
      {
         assertEquals(i, indices.get(i));
      }
      assertEquals(10, indices.get(indices.size() - 1));
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testExtractRevoluteJoints()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArr = ScrewTools.computeSubtreeJoints(chain.getElevator());

      RevoluteJoint[] revoluteJoints = MultiBodySystemTools.filterJoints(jointsArr, RevoluteJoint.class);
      assertEquals(jointsArr.length - 1, revoluteJoints.length);
      for(int i = 0; i < revoluteJoints.length; i++)
      {
         assertEquals("testJoint" + i, revoluteJoints[i].getName());
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeNumberOfJointsOfType()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArr = ScrewTools.computeSubtreeJoints(chain.getElevator());

      int number6DoF = MultiBodySystemTools.computeNumberOfJointsOfType(SixDoFJoint.class, jointsArr);
      int numberRev = MultiBodySystemTools.computeNumberOfJointsOfType(RevoluteJoint.class, jointsArr);

      assertEquals(1, number6DoF);
      assertEquals(jointsArr.length - 1, numberRev);      
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFilterJoints()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArr = ScrewTools.computeSubtreeJoints(chain.getElevator());

      RevoluteJoint[] justRevolutes = MultiBodySystemTools.filterJoints(jointsArr, RevoluteJoint.class);
      assertEquals(jointsArr.length - 1, justRevolutes.length);

      SixDoFJoint[] justSix = MultiBodySystemTools.filterJoints(jointsArr, SixDoFJoint.class);
      assertEquals(1, justSix.length);
      assertTrue(justSix[0] instanceof SixDoFJoint);

      Boolean clean = false;
      for(JointBasics joint: justRevolutes)
      {
         if(joint instanceof RevoluteJoint)
         {
            clean = true;
         }
         else
         {
            clean = false;
         }
         assertTrue(clean);
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFilterJoints_dest()
   {
      Vector3D[] jointAxes = {X, Y, Z, Y, X};
      RandomFloatingRevoluteJointChain chain = new RandomFloatingRevoluteJointChain(random, jointAxes);
      JointBasics[] jointsArr = ScrewTools.computeSubtreeJoints(chain.getElevator());

      RevoluteJoint[] justRevolutes = new RevoluteJoint[jointsArr.length - 1];
      MultiBodySystemTools.filterJoints(jointsArr, justRevolutes, RevoluteJoint.class);
      assertEquals(jointsArr.length - 1, justRevolutes.length);

      SixDoFJoint[] justSix = new SixDoFJoint[1];
      MultiBodySystemTools.filterJoints(jointsArr, justSix, SixDoFJoint.class);
      assertEquals(1, justSix.length);
      assertTrue(justSix[0] instanceof SixDoFJoint);

      Boolean clean = false;
      for(JointBasics joint: justRevolutes)
      {
         if(joint instanceof RevoluteJoint)
         {
            clean = true;
         }
         else
         {
            clean = false;
         }
         assertTrue(clean);
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFindJointsWithNames()
   {
      int numberOfJoints = joints.size();
      JointBasics[] allJoints = new JointBasics[joints.size()];
      for(int i = 0; i < numberOfJoints; i++)
      {
         allJoints[i] = joints.get(i);
      }

      JointBasics[] matches;
      try
      {
         matches = ScrewTools.findJointsWithNames(allJoints, "woof");
         fail("Should throw RuntimeException");
      }
      catch(RuntimeException rte)
      {
         //good  
      }
      matches = ScrewTools.findJointsWithNames(allJoints, "chainAJoint0");
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFindRigidBodiesWithNames_RigidBody_String()
   {
      int numberOfJoints = joints.size();
      RigidBodyBasics[] allBodies = new RigidBodyBasics[joints.size() + 1];
      allBodies[0] = elevator;
      for(int i = 0; i < numberOfJoints; i++)
      {
         allBodies[i+1] = joints.get(i).getSuccessor();
      }

      RigidBodyBasics[] matches;
      try
      {
         matches = ScrewTools.findRigidBodiesWithNames(allBodies, "elevatorOOPS");
         fail("Should throw RuntimeException");
      }
      catch(RuntimeException rte)
      {
         //good  
      }
      matches = ScrewTools.findRigidBodiesWithNames(allBodies, "elevator", "chainABody0", 
            "chainABody1", "chainABody2", "chainABody4", "chainBBody0", "chainBBody1", "chainBBody2", 
            "chainBBody3", "chainBBody4", "chainCBody0", "chainCBody1", "chainCBody2", "chainCBody3", "chainCBody4");
   }
}
