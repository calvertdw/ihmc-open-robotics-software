package us.ihmc.humanoidBehaviors.behaviors.primitives;

import java.util.ArrayList;
import java.util.List;

import controller_msgs.msg.dds.KinematicsPlanningToolboxCenterOfMassMessage;
import controller_msgs.msg.dds.KinematicsPlanningToolboxOutputStatus;
import controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage;
import controller_msgs.msg.dds.ToolboxStateMessage;
import controller_msgs.msg.dds.WholeBodyTrajectoryMessage;
import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.ToolboxState;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.humanoidRobotics.communication.packets.KinematicsPlanningToolboxOutputConverter;
import us.ihmc.idl.IDLSequence.Double;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.SelectionMatrix3D;
import us.ihmc.ros2.Ros2Node;

public class KinematicsPlanningBehavior extends AbstractBehavior
{
   // TODO : hold hand!
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static final double defaultRigidBodyWeight = 20.0;
   private static final double defaultCOMWeight = 1.0;

   private final FullHumanoidRobotModel fullRobotModel;

   private final TDoubleArrayList keyFrameTimes;
   private final List<KinematicsPlanningToolboxRigidBodyMessage> rigidBodyMessages;

   private final KinematicsPlanningToolboxOutputConverter outputConverter;
   private final ConcurrentListeningQueue<KinematicsPlanningToolboxOutputStatus> toolboxOutputQueue = new ConcurrentListeningQueue<>(40);

   private final IHMCROS2Publisher<ToolboxStateMessage> toolboxStatePublisher;
   private final IHMCROS2Publisher<KinematicsPlanningToolboxRigidBodyMessage> rigidBodyMessagePublisher;
   private final IHMCROS2Publisher<KinematicsPlanningToolboxCenterOfMassMessage> comMessagePublisher;
   private final IHMCROS2Publisher<WholeBodyTrajectoryMessage> wholeBodyTrajectoryPublisher;

   public KinematicsPlanningBehavior(String robotName, Ros2Node ros2Node, FullHumanoidRobotModelFactory fullRobotModelFactory,
                                     FullHumanoidRobotModel fullRobotModel)
   {
      super(robotName, ros2Node);

      this.fullRobotModel = fullRobotModel;

      keyFrameTimes = new TDoubleArrayList();
      rigidBodyMessages = new ArrayList<KinematicsPlanningToolboxRigidBodyMessage>();

      outputConverter = new KinematicsPlanningToolboxOutputConverter(fullRobotModelFactory);

      createSubscriber(KinematicsPlanningToolboxOutputStatus.class, kinematicsPlanningToolboxPubGenerator, toolboxOutputQueue::put);
      toolboxStatePublisher = createPublisher(ToolboxStateMessage.class, kinematicsPlanningToolboxSubGenerator);
      rigidBodyMessagePublisher = createPublisher(KinematicsPlanningToolboxRigidBodyMessage.class, kinematicsPlanningToolboxSubGenerator);
      comMessagePublisher = createPublisher(KinematicsPlanningToolboxCenterOfMassMessage.class, kinematicsPlanningToolboxSubGenerator);
      wholeBodyTrajectoryPublisher = createPublisherForController(WholeBodyTrajectoryMessage.class);
   }

   public void clear()
   {
      keyFrameTimes.clear();
      rigidBodyMessages.clear();
   }

   public void setKeyFrameTimes(double trajectoryTime, int numberOfKeyFrames)
   {
      if (keyFrameTimes.size() != 0)
         keyFrameTimes.clear();
      else
         ;

      for (int i = 0; i < numberOfKeyFrames; i++)
      {
         double alpha = (i + 1) / (double) (numberOfKeyFrames);
         keyFrameTimes.add(alpha * trajectoryTime);
      }
   }

   public void setKeyFrameTimes(TDoubleArrayList times)
   {
      if (keyFrameTimes.size() != 0)
         keyFrameTimes.clear();
      else
         ;

      keyFrameTimes.addAll(times);
   }

   public void setKeyFrameTimes(double[] times)
   {
      if (keyFrameTimes.size() != 0)
         keyFrameTimes.clear();
      else
         ;

      keyFrameTimes.addAll(times);
   }

   public void setEndEffectorKeyFrames(RigidBodyBasics endEffector, Pose3DReadOnly desiredPose)
   {
      if (keyFrameTimes.size() == 0)
         throw new RuntimeException("key frame times should be set ahead.");

      List<Pose3DReadOnly> keyFramePoses = new ArrayList<Pose3DReadOnly>();
      FramePose3D initialPose = new FramePose3D(endEffector.getBodyFixedFrame());
      initialPose.changeFrame(worldFrame);

      for (int i = 0; i < keyFrameTimes.size(); i++)
      {
         double alpha = (i + 1) / (double) (keyFrameTimes.size());
         Pose3D pose = new Pose3D(initialPose);
         pose.interpolate(desiredPose, alpha);
         keyFramePoses.add(pose);
      }

      KinematicsPlanningToolboxRigidBodyMessage endEffectorMessage = HumanoidMessageTools.createKinematicsPlanningToolboxRigidBodyMessage(endEffector,
                                                                                                                                          keyFrameTimes,
                                                                                                                                          keyFramePoses);
      rigidBodyMessages.add(endEffectorMessage);
   }

   public void setEndEffectorKeyFrames(RigidBodyBasics endEffector, List<Pose3DReadOnly> desiredPoses)
   {
      if (keyFrameTimes.size() != desiredPoses.size())
         throw new RuntimeException("Inconsistent list lengths: = " + keyFrameTimes.size() + ", desiredPoses.size() = ");

      KinematicsPlanningToolboxRigidBodyMessage endEffectorMessage = HumanoidMessageTools.createKinematicsPlanningToolboxRigidBodyMessage(endEffector,
                                                                                                                                          keyFrameTimes,
                                                                                                                                          desiredPoses);
      rigidBodyMessages.add(endEffectorMessage);
   }

   public void setEndEffectorKeyFrames(RobotSide robotSide, Pose3DReadOnly desiredPose)
   {
      RigidBodyBasics endEffector = fullRobotModel.getHand(robotSide);
      setEndEffectorKeyFrames(endEffector, desiredPose);
   }

   public void setEndEffectorKeyFrames(RobotSide robotSide, List<Pose3DReadOnly> desiredPoses)
   {
      RigidBodyBasics endEffector = fullRobotModel.getHand(robotSide);
      setEndEffectorKeyFrames(endEffector, desiredPoses);
   }

   @Override
   public void doControl()
   {
      if (toolboxOutputQueue.isNewPacketAvailable())
      {
         KinematicsPlanningToolboxOutputStatus solution = toolboxOutputQueue.poll();

         Double keyFrameTimes = solution.getKeyFrameTimes();

         if (keyFrameTimes.size() != solution.getKeyFrameTimes().size())
            throw new RuntimeException("size of key frames are not matched : (given) " + keyFrameTimes.size() + ", (output) "
                  + solution.getKeyFrameTimes().size());
         if (solution.getSolutionQuality() < 0)
            throw new RuntimeException("a key frame can not be accepted.");

         WholeBodyTrajectoryMessage message = new WholeBodyTrajectoryMessage();
         message.setDestination(PacketDestination.CONTROLLER.ordinal());
         outputConverter.setMessageToCreate(message);
         outputConverter.computeWholeBodyTrajectoryMessage(solution);
         wholeBodyTrajectoryPublisher.publish(message);
         deactivateKinematicsToolboxModule();
      }
   }

   @Override
   public void onBehaviorEntered()
   {
      System.out.println("kinematics planning behavior");

      if (keyFrameTimes.size() == 0)
         throw new RuntimeException("key frame times should be set ahead.");

      if (rigidBodyMessages.size() == 0)
         throw new RuntimeException("rigid body key frames should be set ahead.");

      toolboxStatePublisher.publish(MessageTools.createToolboxStateMessage(ToolboxState.WAKE_UP));

      for (int i = 0; i < rigidBodyMessages.size(); i++)
      {
         KinematicsPlanningToolboxRigidBodyMessage message = rigidBodyMessages.get(i);
         message.getAngularWeightMatrix().set(MessageTools.createWeightMatrix3DMessage(defaultRigidBodyWeight));
         message.getLinearWeightMatrix().set(MessageTools.createWeightMatrix3DMessage(defaultRigidBodyWeight));
         rigidBodyMessagePublisher.publish(message);
      }

      // TODO : COM should consider current root body location.
      List<Point3DReadOnly> desiredCOMPoints = new ArrayList<Point3DReadOnly>();
      for (int i = 0; i < keyFrameTimes.size(); i++)
         desiredCOMPoints.add(new Point3D());

      KinematicsPlanningToolboxCenterOfMassMessage comMessage = HumanoidMessageTools.createKinematicsPlanningToolboxCenterOfMassMessage(keyFrameTimes,
                                                                                                                                        desiredCOMPoints);
      SelectionMatrix3D selectionMatrix = new SelectionMatrix3D();
      selectionMatrix.selectZAxis(false);
      comMessage.getSelectionMatrix().set(MessageTools.createSelectionMatrix3DMessage(selectionMatrix));
      comMessage.getWeights().set(MessageTools.createWeightMatrix3DMessage(defaultCOMWeight));
      //comMessagePublisher.publish(comMessage);

      System.out.println("published");
   }

   @Override
   public void onBehaviorAborted()
   {

   }

   @Override
   public void onBehaviorPaused()
   {

   }

   @Override
   public void onBehaviorResumed()
   {

   }

   @Override
   public void onBehaviorExited()
   {

   }

   @Override
   public boolean isDone()
   {
      return false;
   }

   private void deactivateKinematicsToolboxModule()
   {
      toolboxStatePublisher.publish(MessageTools.createToolboxStateMessage(ToolboxState.SLEEP));
   }
}
