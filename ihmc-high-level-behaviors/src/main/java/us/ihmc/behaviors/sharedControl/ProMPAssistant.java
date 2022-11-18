package us.ihmc.behaviors.sharedControl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFrameQuaternionBasics;
import us.ihmc.log.LogTools;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Math.exp;

/**
 * Class to pack a teleoperated referenceFrame and modify it by using some assistance from the robot.
 * The assistance comes from pre-trained probabilistic models: the ProMPs,
 * which represent a probabilistic prediction of multidimensional trajectories.
 * Initially the input from the user is not modified and is simply observed to produce a fitting prediction.
 * Once the task is detected (by recognizing the protagonist object of that task and/or by observing the current motion of the user),
 * the teleoperated referenceFrame gradually shifts from the reference specified by the user to the predicted assistance of the ProMPs.
 */
public class ProMPAssistant implements TeleoperationAssistant
{
   private final HashMap<String, ProMPManager> proMPManagers = new HashMap<>(); //proMPManagers stores a proMPManager for each task
   private String currentTask = ""; //detected task
   private int numberObservations = 0; //number of observations used to update the prediction
   private String relevantBodyPart = ""; // e.g., right hand is the robot part being used to reach the handle and open the door in the task "open door"
   private HashMap<String, String> taskRelevantBodyPart = new HashMap<>();
   private final FramePose3D taskGoalPose = new FramePose3D(); //detected goal
   private final HashMap<String, List<Pose3DReadOnly>> bodyPartObservedFrameTrajectory = new HashMap<>();
   private final HashMap<String, List<FramePose3D>> bodyPartGeneratedFrameTrajectory = new HashMap<>();
   private final HashMap<String, Integer> bodyPartTrajectorySampleCounter = new HashMap<>(); //to track the last used sample of a generated trajectory
   private boolean doneInitialProcessingTask = false;
   private int testNumber;

   public ProMPAssistant()
   {
      List<String> taskNames = new ArrayList<>();
      List<String> relevantBodyParts = new ArrayList<>();
      List<HashMap<String, String>> bodyPartsGeometries = new ArrayList<>();
      boolean logEnabled = false;
      // read parameters regarding the properties of available learned tasks from json file
      try
      {
         LogTools.info("Looking for configuration file ProMPAssistant.json ...");
         String configurationFile = "repository-group/ihmc-open-robotics-software/ihmc-high-level-behaviors/src/main/resources/us/ihmc/behaviors/sharedControl/ProMPAssistant.json";
         JSONObject jsonObject = (JSONObject) new JSONParser().parse(new FileReader(Paths.get(System.getProperty("user.home"), configurationFile).toString()));
         LogTools.info("File found: /{}", configurationFile);
         testNumber = (int) ((long) jsonObject.get("testNumberUseOnlyForTesting"));
         logEnabled = (boolean) jsonObject.get("logging");
         numberObservations = (int) ((long) jsonObject.get("numberObservations"));
         // getting tasks
         JSONArray tasksArray = (JSONArray) jsonObject.get("tasks");
         //iterating tasks
         Iterator taskIterator = tasksArray.iterator();
         while (taskIterator.hasNext())
         {
            Iterator<Map.Entry> taskPropertiesIterator = ((Map) taskIterator.next()).entrySet().iterator();
            while (taskPropertiesIterator.hasNext())
            {
               Map.Entry taskPropertyMap = taskPropertiesIterator.next();
               switch (taskPropertyMap.getKey().toString())
               {
                  case "name":
                     taskNames.add((String) taskPropertyMap.getValue());
                     break;
                  case "relevantBodyPart":
                     relevantBodyParts.add((String) taskPropertyMap.getValue());
                     break;
                  case "bodyParts":
                     JSONArray bodyPartsArray = (JSONArray) taskPropertyMap.getValue();
                     HashMap<String, String> bodyPartsGeometry = new HashMap<>();
                     //parse body parts
                     for (Object bodyPartObject : bodyPartsArray)
                     {
                        JSONObject jsonBodyPartObject = (JSONObject) bodyPartObject;
                        List<String> name = new ArrayList<>();
                        List<String> geometry = new ArrayList<>();
                        jsonBodyPartObject.keySet().forEach(bodyPartProperty ->
                        {
                           switch (bodyPartProperty.toString())
                           {
                              case "name":
                                 name.add(String.valueOf((jsonBodyPartObject.get(bodyPartProperty))));
                                 break;
                              case "geometry":
                                 geometry.add(String.valueOf(jsonBodyPartObject.get(bodyPartProperty)));
                                 break;
                              default:
                                 break;
                           }
                        });
                        for (int i = 0; i < name.size(); i++)
                           bodyPartsGeometry.put(name.get(i), geometry.get(i));
                     }
                     bodyPartsGeometries.add(bodyPartsGeometry);
                     break;
                  default:
                     break;
               }
            }
         }
      }
      catch (FileNotFoundException ex)
      {
         ex.printStackTrace();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      catch (ParseException e)
      {
         throw new RuntimeException(e);
      }

      for (int i = 0; i < taskNames.size(); i++)
      {
         LogTools.info("Learning ProMPs for task: {}", taskNames.get(i));
         for (int j = 0; j < bodyPartsGeometries.size(); j++)
         {
            for (String key : bodyPartsGeometries.get(j).keySet())
            {
               LogTools.info("     {} {}", key, bodyPartsGeometries.get(j).get(key));
            }
         }
         proMPManagers.put(taskNames.get(i), new ProMPManager(taskNames.get(i), bodyPartsGeometries.get(i), logEnabled));
         taskRelevantBodyPart.put(taskNames.get(i), relevantBodyParts.get(i));
      }
      for (ProMPManager proMPManager : proMPManagers.values())
         proMPManager.learnTaskFromDemos();
      LogTools.info("ProMPs are ready to be used!");
   }

   @Override
   public void processFrameInformation(Pose3DReadOnly observedPose, String bodyPart)
   {
      if (objectDetected())
      {
         if (!relevantBodyPart.isEmpty()) //the task does have a relevant body part, this means there is a goal a body part can reach
         {
            if (objectPoseEstimated())
            {
               //store observed pose
               Pose3D lastObservedPose = new Pose3D();
               lastObservedPose.getPosition().set(observedPose.getPosition().getX(), observedPose.getPosition().getY(), observedPose.getPosition().getZ());
               lastObservedPose.getOrientation()
                               .set(observedPose.getOrientation().getX(),
                                    observedPose.getOrientation().getY(),
                                    observedPose.getOrientation().getZ(),
                                    observedPose.getOrientation().getS());
               bodyPartObservedFrameTrajectory.get(bodyPart).add(lastObservedPose);
               //update the proMP prediction according to observations and observed goal and generate mean trajectory
               if (bodyPartObservedFrameTrajectory.get(bodyPart).size() > numberObservations) //if observed a sufficient number of poses
               {
                  updateTaskWithObjectInfo();
                  generateTaskTrajectories();
                  doneInitialProcessingTask = true;
               }
            }
         }
         else //there is no specific goal location to reach with a body part in this task
         {
            //store observed pose
            Pose3D lastObservedPose = new Pose3D();
            lastObservedPose.getPosition().set(observedPose.getPosition().getX(), observedPose.getPosition().getY(), observedPose.getPosition().getZ());
            lastObservedPose.getOrientation()
                            .set(observedPose.getOrientation().getX(),
                                 observedPose.getOrientation().getY(),
                                 observedPose.getOrientation().getZ(),
                                 observedPose.getOrientation().getS());
            bodyPartObservedFrameTrajectory.get(bodyPart).add(lastObservedPose);
            //update the proMP prediction according to observations and generate mean trajectory
            if (bodyPartObservedFrameTrajectory.get(bodyPart).size() > numberObservations) //if observed a sufficient number of poses
            {
               LogTools.info("Updating ProMP based on observations ...");
               updateTask();
               generateTaskTrajectories();
               doneInitialProcessingTask = true;
               LogTools.info("ProMPs initial processing completed!");
            }
         }
      }
   }

   private boolean objectDetected()
   {
      if (currentTask.isEmpty())
      {
         //TODO A.1. recognize task with object detection algorithm (or Aruco Markers to begin with)
         currentTask = "PushDoor";
         relevantBodyPart = taskRelevantBodyPart.get(currentTask);
         //initialize bodyPartObservedFrameTrajectory that will contain for each body part a list of observed FramePoses
         for (String bodyPart : (proMPManagers.get(currentTask).getBodyPartsGeometry()).keySet())
            bodyPartObservedFrameTrajectory.put(bodyPart, new ArrayList<>());
         return !(currentTask.isEmpty());
      }
      else
         return true;
   }

   private boolean objectPoseEstimated()
   {
      //TODO A.2. identify object pose (with Aruco Markers to begin with)
      //taskGoalPose = ;
      return !(taskGoalPose.equals(new FramePose3D()));
   }

   private void updateTaskWithObjectInfo()
   {
      //update speed proMP based on relevant body part observed trajectory and goal
      proMPManagers.get(currentTask).updateTaskSpeed(bodyPartObservedFrameTrajectory.get(relevantBodyPart), taskGoalPose, relevantBodyPart);
      //update all proMP trajectories based on initial observations (stored observed poses)
      for (String robotPart : bodyPartObservedFrameTrajectory.keySet())
      {
         List<Pose3DReadOnly> robotPartObservedTrajectory = bodyPartObservedFrameTrajectory.get(robotPart);
         for (int i = 0; i < robotPartObservedTrajectory.size(); i++)
         {
            proMPManagers.get(currentTask).updateTaskTrajectory(robotPart, robotPartObservedTrajectory.get(i), i);
         }
      }
      //update only proMP trajectory of the body part relevant for goal of the task, based on observed goal
      proMPManagers.get(currentTask).updateTaskTrajectoryGoal(relevantBodyPart, taskGoalPose);
   }

   private void updateTask()
   {
      //build vector of observed trajectories for the hands
      Set<String> bodyParts = bodyPartObservedFrameTrajectory.keySet();
      List<List<Pose3DReadOnly>> observedFrameTrajectories = new ArrayList<>();
      for (String bodyPart : bodyParts)
         observedFrameTrajectories.add(bodyPartObservedFrameTrajectory.get(bodyPart));
      LogTools.info("   - Updating ProMP speed ...");
      //update speed proMP based on hands observed trajectories
//      proMPManagers.get(currentTask).updateTaskSpeed(observedFrameTrajectories, bodyParts);
      // TODO B.1. use line above where you compare both hands if relevantBodyPart is both hands and check only main hand if it is one hand
      // TODO B.2. what if someone is lefthanded, or simply wants to use the left hand for that task, should we learn the task for both hands?
      // TODO B.3. change relevantBodyPart concept  which now means that bodyPart will reach a goal that can be observed
      //       Add instead goalBodyPart and change use of relevantBodyPart as the part that is used the most for that task
      proMPManagers.get(currentTask).updateTaskSpeed(bodyPartObservedFrameTrajectory.get("rightHand"), "rightHand");
      LogTools.info("   - Updating ProMP trajectories ...");
      //update all proMP trajectories based on initial observations (stored observed poses)
      for (String robotPart : bodyPartObservedFrameTrajectory.keySet())
      {
         List<Pose3DReadOnly> robotPartObservedTrajectory = bodyPartObservedFrameTrajectory.get(robotPart);
         if (robotPartObservedTrajectory.size() > 0)
         {
            proMPManagers.get(currentTask).updateTaskTrajectory(robotPart,
                                                                robotPartObservedTrajectory.get(robotPartObservedTrajectory.size() - 1),
                                                                robotPartObservedTrajectory.size() - 1);
         }
//                  for (int i = 0; i < robotPartObservedTrajectory.size(); i++)
//                  {
//                     proMPManagers.get(currentTask).updateTaskTrajectory(robotPart, robotPartObservedTrajectory.get(i), i);
//                  }
      }
   }

   private void generateTaskTrajectories()
   {
      //for each body part generate the mean trajectory of the learned promp
      for (String bodyPart : bodyPartObservedFrameTrajectory.keySet())
      {
         bodyPartGeneratedFrameTrajectory.put(bodyPart, proMPManagers.get(currentTask).generateTaskTrajectory(bodyPart));
         //start using it after the last sample we observed, not from the beginning. We do not want to restart the motion
         bodyPartTrajectorySampleCounter.put(bodyPart, numberObservations);
      }
   }

   @Override
   public boolean readyToPack()
   {
      return doneInitialProcessingTask;
   }

   @Override
   public void framePoseToPack(FramePose3D framePose, String bodyPart)
   {
      List<FramePose3D> generatedFramePoseTrajectory = bodyPartGeneratedFrameTrajectory.get(bodyPart);
      int sampleCounter = bodyPartTrajectorySampleCounter.get(bodyPart);
      if (sampleCounter < generatedFramePoseTrajectory.size()){
         //take a sample (frame) from the trajectory
         FramePose3D generatedFramePose = generatedFramePoseTrajectory.get(bodyPartTrajectorySampleCounter.get(bodyPart));
         FixedFrameQuaternionBasics generatedFrameOrientation = generatedFramePose.getOrientation();
         FixedFramePoint3DBasics generatedFramePosition = generatedFramePose.getPosition();
         framePose.getPosition().set(generatedFramePosition);
         framePose.getOrientation().set(generatedFrameOrientation);

         //take the next sample from the trajectory next time
         bodyPartTrajectorySampleCounter.replace(bodyPart, bodyPartTrajectorySampleCounter.get(bodyPart) + 1);
      }
      else
      {
         reset();
      }
   }

   //TODO Edit this function to take into account affordance. Ignore for the moment
   public void framePoseToPackToAffordance(FramePose3D framePose, String bodyPart)
   {
      List<FramePose3D> generatedFramePoseTrajectory = bodyPartGeneratedFrameTrajectory.get(bodyPart);
      //take a sample (frame) from the trajectory
      FramePose3D generatedFramePose = generatedFramePoseTrajectory.get(bodyPartTrajectorySampleCounter.get(bodyPart));
      //TODO C.1.1 IF goal is observable -> compute distance from region close to the goal and use this to select the next sample.
      // If distance is increasing, go back to previous sample

      if (bodyPart.equals(relevantBodyPart))
      {
         //TODO C.1.2 IF goal is observable -> compute distance from region close to the goal and use this to modulate alpha
         // compute initial distance when goal is detected
         // set alpha according to distance
         //TODO C.2. Can we re-estimate speed real-time as well to adapt it to user motion and change alpha accordingly?
         // Not sure it'd be robust but worth a try
      }
      // shared-control arbitration law. Shift gradually from user input to robot autonomy
      int sampleCounter = bodyPartTrajectorySampleCounter.get(bodyPart);
      if (sampleCounter <= generatedFramePoseTrajectory.size())
      {
         double x = (double)(sampleCounter - numberObservations) / (generatedFramePoseTrajectory.size() - numberObservations);
         //define a function that goes from 0 to 1 smoothly, while getting to 1 not too close to the end of the motions
         double alpha = 1.0 / (1 + 4 * exp(-18 * (x - 0.2))); //sigmoid with [X:0,Y:~0],[X:0.6,Y:~1],[X>1,Y:1]
         if (alpha>=0.9999)
            alpha=1;
         //set orientation
         FixedFrameQuaternionBasics frameOrientation = framePose.getOrientation();
         FixedFrameQuaternionBasics generatedFrameOrientation = generatedFramePose.getOrientation();
         FixedFrameQuaternionBasics arbitratedFrameOrientation = framePose.getOrientation();
         arbitratedFrameOrientation.set((1 - alpha) * frameOrientation.getX() + alpha * generatedFrameOrientation.getX(),
                                        (1 - alpha) * frameOrientation.getY() + alpha * generatedFrameOrientation.getY(),
                                        (1 - alpha) * frameOrientation.getZ() + alpha * generatedFrameOrientation.getZ(),
                                        (1 - alpha) * frameOrientation.getS() + alpha * generatedFrameOrientation.getS());
         //set position
         FixedFramePoint3DBasics framePosition = framePose.getPosition();
         FixedFramePoint3DBasics generatedFramePosition = generatedFramePose.getPosition();
         FixedFramePoint3DBasics arbitratedFramePosition = framePose.getPosition();
         arbitratedFramePosition.setX((1 - alpha) * framePosition.getX() + alpha * generatedFramePosition.getX());
         arbitratedFramePosition.setY((1 - alpha) * framePosition.getY() + alpha * generatedFramePosition.getY());
         arbitratedFramePosition.setZ((1 - alpha) * framePosition.getZ() + alpha * generatedFramePosition.getZ());

         //         framePose.getPosition().set(arbitratedFramePosition);
         //         framePose.getOrientation().set(arbitratedFrameOrientation);
         framePose.getPosition().set(generatedFramePosition);
         framePose.getOrientation().set(generatedFrameOrientation);

         //take the next sample from the trajectory next time
         bodyPartTrajectorySampleCounter.replace(bodyPart, bodyPartTrajectorySampleCounter.get(bodyPart) + 1);
      }
      else
      {
         reset();
      }
   }

   private void reset()
   {
      //reset manager of current task (reset reference of proMP object of current task to initial proMP before any conditioning)
      proMPManagers.get(currentTask).resetTask();
      currentTask = "";
      taskGoalPose.setToZero(taskGoalPose.getReferenceFrame().getWorldFrame());
      bodyPartObservedFrameTrajectory.clear();
      bodyPartGeneratedFrameTrajectory.clear();
      bodyPartTrajectorySampleCounter.clear();
      doneInitialProcessingTask = false;
   }

   public int getTestNumber()
   {
      return testNumber;
   }
}
