package us.ihmc.exampleSimulations.simpleArm;

import java.util.EnumMap;
import java.util.Random;

import us.ihmc.euclid.Axis3D;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.mecano.multiBodySystem.RevoluteJoint;
import us.ihmc.mecano.multiBodySystem.RigidBody;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.robotics.geometry.RotationalInertiaCalculator;
import us.ihmc.simulationconstructionset.GroundContactModel;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.LinearGroundContactModel;

public class SimpleArmRobot extends Robot
{
   private final AppearanceDefinition red = YoAppearance.Red();
   private final AppearanceDefinition black = YoAppearance.Black();
   private final AppearanceDefinition blue = YoAppearance.Blue();

   private static final Random random = new Random(12873202943L);

   public enum ArmJoint
   {
      YAW,
      PITCH_1,
      PITCH_2;

      public static ArmJoint[] values = {YAW, PITCH_1, PITCH_2};
   }

   private enum ArmBody
   {
      ELEVATOR,
      ARM_1,
      ARM_2,
      ARM_3,
   }

   private final EnumMap<ArmJoint, OneDoFJointBasics> jointMap = new EnumMap<>(ArmJoint.class);
   private final EnumMap<ArmJoint, OneDegreeOfFreedomJoint> scsJointMap = new EnumMap<>(ArmJoint.class);
   private final EnumMap<ArmBody, RigidBodyBasics> bodyMap = new EnumMap<>(ArmBody.class);

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   public static final double gravity = 9.81;
   private static final double jointDamping = 0.1;

   private static final double baseHeight = 0.1;

   private static final double arm1_mass = 1.0;
   private static final double arm1_length = 0.5;
   private static final double arm1_radius = 0.02;

   private static final double arm2_mass = 0.5;
   private static final double arm2_length = 0.4;
   private static final double arm2_radius = 0.015;

   private static final double actuator_length = 0.04;
   private static final double actuator_radius = 0.02;

   public SimpleArmRobot(double transparancy)
   {
      super("SimpleArm");
      this.setGravity(0.0, 0.0, -gravity);

      red.setTransparency(transparancy);
      black.setTransparency(transparancy);
      blue.setTransparency(transparancy);

      // --- id robot ---
      RigidBodyBasics elevator = new RigidBody("elevator", worldFrame);
      bodyMap.put(ArmBody.ELEVATOR, elevator);

      RevoluteJoint idYawJoint = new RevoluteJoint("idYawJoint", elevator, new Vector3D(0.0, 0.0, baseHeight), new Vector3D(0.0, 0.0, 1.0));
      Matrix3D inertiaArm1 = RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidCylinder(arm1_mass, arm1_radius, arm1_length, Axis3D.Z);
      RigidBodyBasics arm1 = new RigidBody("arm1", idYawJoint, inertiaArm1, arm1_mass, new Vector3D(0.0, 0.0, arm1_length/2.0));
      jointMap.put(ArmJoint.YAW, idYawJoint);
      bodyMap.put(ArmBody.ARM_1, arm1);

      RevoluteJoint idPitch1Joint = new RevoluteJoint("idPitch1Joint", arm1, new Vector3D(0.0, 0.0, arm1_length), new Vector3D(1.0, 0.0, 0.0));
      Matrix3D inertiaArm2 = RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidCylinder(arm2_mass, arm2_radius, arm2_length, Axis3D.X);
      RigidBodyBasics arm2 = new RigidBody("arm2", idPitch1Joint, inertiaArm2, arm2_mass, new Vector3D(0.0, 0.0, arm2_length/2.0));
      jointMap.put(ArmJoint.PITCH_1, idPitch1Joint);
      bodyMap.put(ArmBody.ARM_2, arm2);

      RevoluteJoint idPitch2Joint = new RevoluteJoint("idPitch2Joint", arm2, new Vector3D(0.0, 0.0, arm2_length), new Vector3D(1.0, 0.0, 0.0));
      RigidBodyBasics arm3 = new RigidBody("arm3", idPitch2Joint, inertiaArm2, arm2_mass, new Vector3D(0.0, 0.0, arm2_length/2.0));
      jointMap.put(ArmJoint.PITCH_2, idPitch2Joint);
      bodyMap.put(ArmBody.ARM_3, arm3);

      // --- scs robot ---
      makeBase();

      PinJoint yawJoint = new PinJoint("yaw", new Vector3D(0.0, 0.0, baseHeight), this, Axis3D.Z);
      yawJoint.setDamping(jointDamping);
      yawJoint.setLink(makeArm1());
      this.addRootJoint(yawJoint);
      scsJointMap.put(ArmJoint.YAW, yawJoint);

      PinJoint pitch1Joint = new PinJoint("pitch1", new Vector3D(0.0, 0.0, arm1_length), this, Axis3D.X);
      pitch1Joint.setDamping(jointDamping);
      pitch1Joint.setLink(makeArm2());
      yawJoint.addJoint(pitch1Joint);
      scsJointMap.put(ArmJoint.PITCH_1, pitch1Joint);

      PinJoint pitch2Joint = new PinJoint("pitch2", new Vector3D(0.0, 0.0, arm2_length), this, Axis3D.X);
      pitch2Joint.setDamping(jointDamping);
      pitch2Joint.setLink(makeArm2());
      pitch1Joint.addJoint(pitch2Joint);
      scsJointMap.put(ArmJoint.PITCH_2, pitch2Joint);

      GroundContactModel groundContactModel = new LinearGroundContactModel(this, this.getRobotsYoRegistry());
      this.setGroundContactModel(groundContactModel);

      showCoordinatesRecursively(yawJoint, false);

      yawJoint.setQ((random.nextDouble() - 0.5) * Math.PI);
      pitch1Joint.setQ((random.nextDouble() - 0.5) * Math.PI);
      pitch2Joint.setQ((random.nextDouble() - 0.5) * Math.PI);
   }

   private Link makeArm1()
   {
      Link arm = new Link("arm1");

      arm.setMass(arm1_mass);
      arm.setComOffset(new Vector3D(0.0, 0.0, arm1_length/2.0));

      double ixx = arm1_mass/12.0 * (3.0 * arm1_radius*arm1_radius + arm1_length*arm1_length);
      double iyy = ixx;
      double izz = arm1_mass/2.0 * arm1_radius*arm1_radius;

      arm.setMomentOfInertia(ixx, iyy, izz);

      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCylinder(arm1_length, arm1_radius, red);
      arm.setLinkGraphics(linkGraphics);

      return arm;
   }

   private Link makeArm2()
   {
      Link arm = new Link("arm2");

      arm.setMass(arm2_mass);
      arm.setComOffset(new Vector3D(0.0, 0.0, arm2_length/2.0));

      double ixx = arm2_mass/12.0 * (3.0 * arm2_radius*arm2_radius + arm2_length*arm2_length);
      double iyy = ixx;
      double izz = arm2_mass/2.0 * arm2_radius*arm2_radius;
      arm.setMomentOfInertia(ixx, iyy, izz);

      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCylinder(arm2_length, arm2_radius, red);

      linkGraphics.rotate(Math.PI/2.0, Axis3D.Z);
      linkGraphics.rotate(Math.PI/2.0, Axis3D.X);
      linkGraphics.translate(0.0, 0.0, -actuator_length/2.0);
      linkGraphics.addCylinder(actuator_length, actuator_radius, black);

      arm.setLinkGraphics(linkGraphics);

      return arm;
   }

   private void makeBase()
   {
      Link base = new Link("base");
      base.setMass(100.0);
      base.setMomentOfInertia(1.0, 1.0, 1.0);
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCylinder(baseHeight, baseHeight, blue);
      linkGraphics.addCoordinateSystem(0.3);
      base.setLinkGraphics(linkGraphics);
      this.addStaticLink(base);
   }

   private void showCoordinatesRecursively(Joint joint, boolean drawEllipoids)
   {
      Graphics3DObject linkGraphics = joint.getLink().getLinkGraphics();
      linkGraphics.identity();
      linkGraphics.addCoordinateSystem(0.2);
      if (drawEllipoids)
      {
         joint.getLink().addEllipsoidFromMassProperties();
      }
      for (Joint child : joint.getChildrenJoints())
      {
         showCoordinatesRecursively(child, drawEllipoids);
      }
   }

   public OneDoFJointBasics getJoint(ArmJoint jointName)
   {
      return jointMap.get(jointName);
   }

   public RigidBodyBasics getEndEffectorBody()
   {
      return bodyMap.get(ArmBody.ARM_3);
   }

   public void updateInverseDynamicsStructureFromSimulation()
   {
      for (ArmJoint joint : ArmJoint.values)
      {
         OneDoFJointBasics idJoint = jointMap.get(joint);
         OneDegreeOfFreedomJoint scsJoint = scsJointMap.get(joint);
         idJoint.setQ(scsJoint.getQYoVariable().getDoubleValue());
         idJoint.setQd(scsJoint.getQDYoVariable().getDoubleValue());
      }
      bodyMap.get(ArmBody.ELEVATOR).updateFramesRecursively();
   }

   public void updateSimulationFromInverseDynamicsTorques()
   {
      for (ArmJoint joint : ArmJoint.values)
      {
         OneDoFJointBasics idJoint = jointMap.get(joint);
         OneDegreeOfFreedomJoint scsJoint = scsJointMap.get(joint);
         scsJoint.setTau(idJoint.getTau());
      }
   }

}