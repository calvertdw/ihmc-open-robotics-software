package us.ihmc.humanoidBehaviors.behaviors.complexBehaviors;

import controller_msgs.msg.dds.DoorLocationPacket;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.behaviors.goalLocation.GoalDetectorBehaviorService;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.ros2.Ros2Node;

public class SearchForDoorBehavior extends AbstractBehavior
{
   private Pose3D doorTransformToWorld;
   private boolean recievedNewDoorLocation = false;

   protected final ConcurrentListeningQueue<DoorLocationPacket> doorLocationQueue = new ConcurrentListeningQueue<DoorLocationPacket>(10);
   private final GoalDetectorBehaviorService fiducialDetectorBehaviorService;

   public SearchForDoorBehavior(String robotName, Ros2Node ros2Node, GoalDetectorBehaviorService goalDetectorBehaviorService)
   {
      super(robotName, "SearchForDoor", ros2Node);
      createBehaviorInputSubscriber(DoorLocationPacket.class, doorLocationQueue::put);

      this.fiducialDetectorBehaviorService = goalDetectorBehaviorService;
      addBehaviorService(fiducialDetectorBehaviorService);
   }

   @Override
   public void onBehaviorEntered()
   {
   }

   @Override
   public void doControl()
   {
      if (doorLocationQueue.isNewPacketAvailable())
      {
         recievedDoorLocation(doorLocationQueue.getLatestPacket());
      }
      if (fiducialDetectorBehaviorService.getGoalHasBeenLocated())
      {
         FramePose3D tmpFP = new FramePose3D();
         fiducialDetectorBehaviorService.getReportedGoalPoseWorldFrame(tmpFP);
         Pose3D pose = new Pose3D(tmpFP.getPosition(), tmpFP.getOrientation());
         publishTextToSpeech("Recieved Door Location From fiducial");

         setDoorLocation(pose);
      }
      

   }

   @Override
   public boolean isDone()
   {
      return recievedNewDoorLocation;
   }

   @Override
   public void onBehaviorExited()
   {
      recievedNewDoorLocation = false;
   }

   public Pose3D getLocation()
   {
      return doorTransformToWorld;
   }

   private void recievedDoorLocation(DoorLocationPacket valveLocationPacket)
   {

      publishTextToSpeech("Recieved Door Location From UI");
      setDoorLocation(valveLocationPacket.getDoorTransformToWorld());



   }
   
   public void setDoorLocation(Pose3D pose)
   {
      doorTransformToWorld = pose;

      recievedNewDoorLocation = true;
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

}
