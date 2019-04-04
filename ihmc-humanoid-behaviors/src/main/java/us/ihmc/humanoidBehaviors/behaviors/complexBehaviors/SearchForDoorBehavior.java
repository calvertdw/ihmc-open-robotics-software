package us.ihmc.humanoidBehaviors.behaviors.complexBehaviors;

import controller_msgs.msg.dds.DoorLocationPacket;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.behaviors.behaviorServices.FiducialDetectorBehaviorService;
import us.ihmc.humanoidBehaviors.behaviors.goalLocation.GoalDetectorBehaviorService;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.ros2.Ros2Node;

public class SearchForDoorBehavior extends AbstractBehavior
{
   private Pose3D doorTransformToWorld;
   private boolean recievedNewDoorLocation = false;

   protected final ConcurrentListeningQueue<DoorLocationPacket> doorLocationQueue = new ConcurrentListeningQueue<DoorLocationPacket>(10);
   private final FiducialDetectorBehaviorService fiducialDetectorBehaviorService;
   private final IHMCROS2Publisher<DoorLocationPacket> publisher;


   public SearchForDoorBehavior(String robotName, Ros2Node ros2Node, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      super(robotName, "SearchForDoor", ros2Node);
      createBehaviorInputSubscriber(DoorLocationPacket.class, doorLocationQueue::put);
      fiducialDetectorBehaviorService = new FiducialDetectorBehaviorService(robotName, "SearchForDoorFiducial1", ros2Node, yoGraphicsListRegistry);
      fiducialDetectorBehaviorService.setTargetIDToLocate(50);
      fiducialDetectorBehaviorService.setExpectedFiducialSize(0.2032);

      registry.addChild(fiducialDetectorBehaviorService.getYoVariableRegistry());
      publisher = createBehaviorOutputPublisher(DoorLocationPacket.class);

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

         
         //1.1684 down .65405 .2032" fiducial offset on door
                  
         FramePose3D tmpFP = new FramePose3D();
         fiducialDetectorBehaviorService.getReportedGoalPoseWorldFrame(tmpFP);

         tmpFP.appendPitchRotation(Math.toRadians(90));
         tmpFP.appendYawRotation(0);
         tmpFP.appendRollRotation(Math.toRadians(-90));
         
         tmpFP.appendPitchRotation(-tmpFP.getPitch());
         
         FramePose3D doorFrame = new FramePose3D(tmpFP);
//         doorFrame.appendPitchRotation(-doorFrame.getPitch());
         doorFrame.appendTranslation(-0.015875,.65405, -1.1684);
        // doorFrame.changeFrame(ReferenceFrame.getWorldFrame());
         
         
         Pose3D pose = new Pose3D(doorFrame.getPosition(), doorFrame.getOrientation());
         
         
         
         
         publishTextToSpeech("Recieved Door Location From fiducial");
         pose.appendYawRotation(Math.toRadians(-90));
         
         Point3D location = new Point3D();
         Quaternion orientation = new Quaternion();
         pose.get(location, orientation);
        // publishUIPositionCheckerPacket(location,orientation);

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

   private void recievedDoorLocation(DoorLocationPacket doorLocationPacket)
   {
      recievedNewDoorLocation = true;
      publishTextToSpeech("Recieved Door Location From UI");
      //setDoorLocation(doorLocationPacket.getDoorTransformToWorld());



   }
   
   public void setDoorLocation(Pose3D pose)
   {
      doorTransformToWorld = pose;

      
      publisher.publish(HumanoidMessageTools.createDoorLocationPacket(pose));
      
      
      publishUIPositionCheckerPacket(pose.getPosition(), pose.getOrientation());
      
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
