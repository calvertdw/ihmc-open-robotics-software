package us.ihmc.avatar.sharedControl;

import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.log.LogTools;
import us.ihmc.promp.*;
import us.ihmc.tools.io.WorkspaceDirectory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static us.ihmc.promp.Trajectory.infer_closest_trajectory;
import static us.ihmc.promp.presets.ProMPInfoMapper.EigenMatrixXd;
import static us.ihmc.promp.presets.ProMPInfoMapper.EigenVectorXd;

/**
 * Class to learn and use ProMPs for assistance in a given task. Based on promp java-cpp library.
 * A ProMP represents a probabilistic prediction of a multi-dimensional trajectory.
 * The dimension is set in the constructor according to the information you want to retain
 * (e.g, position, orientation, pose of a body part of the robot).
 * The prediction can be updated to match the speed and trend of the current motion.
 * The mean trajectory of the prediction (which can be updated) is always used as the actual predicted trajectory.
 */
public class ProMPManager
{
   private final String taskName;
   private final HashMap<String, String> bodyPartsGeometry;
   // learnedProMPs stores a set of multi-D (e.g., for pose D=6, for position D=3) proMPs (a multi-D proMP for each body part)
   private final HashMap<String, ProMP> learnedProMPs = new HashMap<>();
   private final HashMap<String, TrajectoryGroup> trainingTrajectories = new HashMap<>();

   /* Class constructor
    * @param taskName: name of the task
    * @param bodyPartsGeometry: body part of the robot and geometry (e.g. position, orientation, pose) for which you want to learn the ProMPs
    */
   public ProMPManager(String taskName, HashMap<String, String> bodyPartsGeometry)
   {
      this.taskName = taskName;
      this.bodyPartsGeometry = bodyPartsGeometry;
      ProMPNativeLibrary.load();
   }

   /* learn the ProMPs for the task based demo training trajectories stored in .../promp/etc/demos
    * learn a ProMP for each bodyPart specified in the constructor of this class */
   public void learnTaskFromDemos()
   {
      WorkspaceDirectory demoDir = new WorkspaceDirectory("ihmc-open-robotics-software", "promp/etc/demos");
      String demoDirAbs = demoDir.getDirectoryPath().toAbsolutePath().toString();
      String demoTrainingDirAbs = demoDirAbs + taskName;
      File demoFolder = new File(demoTrainingDirAbs);
      File[] listOfFiles = demoFolder.listFiles();
      List<String> fileListTraining = new ArrayList<>();
      // The trajectories contained in the demos folders represent different demonstration of a given task
      // Several trajectories of different body parts have been recorded
      for (int i = 0; i < listOfFiles.length; i++) //get training files
         fileListTraining.add(demoTrainingDirAbs + "/" + (i + 1) + ".csv");

      List<Long> dofs = new ArrayList<>();
      // This is how the dofs are stored in the csv training files (generated using KinematicsRecordReplay in GDXVRKinematicsStreaming)
      // 0,1,2,3: left hand quaternion; 4,5,6: left hand X,Y,Z;
      // 7,8,9,10: right hand quaternion; 11,12,13: left hand X,Y,Z;
      for (String bodyPart : bodyPartsGeometry.keySet())
      {
         if (bodyPartsGeometry.get(bodyPart).equals("Orientation"))
         {
            dofs.add(0L);
            dofs.add(1L);
            dofs.add(2L);
            dofs.add(3L);
         }
         else if (bodyPartsGeometry.get(bodyPart).equals("Position"))
         {
            dofs.add(4L);
            dofs.add(5L);
            dofs.add(6L);
         }
         else if (bodyPartsGeometry.get(bodyPart).equals("Pose"))
         {
            dofs.add(0L);
            dofs.add(1L);
            dofs.add(2L);
            dofs.add(3L);
            dofs.add(4L);
            dofs.add(5L);
            dofs.add(6L);
         }
         if (bodyPart.equals("rightHand"))
         {
            dofs.replaceAll(dof -> dof + 7L);
         }
         TrajectoryGroup trainingTrajectory = new TrajectoryGroup();
         //training filelist
         StringVector fileListStringVectorTraining = new StringVector();
         fileListTraining.forEach(fileListStringVectorTraining::push_back);
         SizeTVector doFsSizeTVector = new SizeTVector();
         dofs.forEach(doFsSizeTVector::push_back);
         // load the training trajectories from the csv files
         trainingTrajectory.load_csv_trajectories(fileListStringVectorTraining, doFsSizeTVector);
         // make all training trajectories have the same length (= mean length)
         int meanLengthTraining = (int) trainingTrajectory.normalize_length();
         trainingTrajectories.put(bodyPart, trainingTrajectory);
         learnedProMPs.put(bodyPart, new ProMP(trainingTrajectory, 20)); // default 20 rbf functions seems to generalize well
      }
   }

   public void resetTask()
   {
      for (String bodyPart : bodyPartsGeometry.keySet())
         learnedProMPs.replace(bodyPart, new ProMP(trainingTrajectories.get(bodyPart), 20));
   }

   /* update the speed of the ProMPs of the task based on observation of a body-part trajectory (e.g., RightHand or LeftHand) */
   public void updateTaskSpeed(List<Pose3DReadOnly> observedFrameTrajectory, String bodyPart)
   {
      EigenMatrixXd observedTrajectory = toEigenMatrix(observedFrameTrajectory, bodyPart);
      TrajectoryVector demoTrajectories = trainingTrajectories.get(bodyPart).trajectories();
      // infer what training demo is the closest to the observed trajectory
      int demo = infer_closest_trajectory(observedTrajectory, demoTrajectories);
      // infer the new speed for the demo trajectory based on observed (portion of) trajectory
      double inferredSpeed = demoTrajectories.get(demo).infer_speed(observedTrajectory, 0.25, 4.0, 30);
      // find equivalent timesteps
      int inferredTimesteps = (int) (demoTrajectories.get(demo).timesteps() / inferredSpeed);
      LogTools.info("Inferred timesteps: {}", inferredTimesteps);
      // update the time modulation of the learned ProMPs with estimated value
      for (ProMP proMPBodyPart : learnedProMPs.values())
         proMPBodyPart.update_time_modulation((double) proMPBodyPart.get_traj_length() / inferredTimesteps);
   }

   /* update the speed of the ProMPs of the task based on observation of a body-part trajectory and goal (e.g., RightHand or LeftHand) */
   public void updateTaskSpeed(List<Pose3DReadOnly> observedFrameTrajectory, Pose3DReadOnly observedGoal, String bodyPart)
   {
      EigenMatrixXd observedTrajectory = toEigenMatrix(observedFrameTrajectory, bodyPart);
      // create a copy of proMP for current task
      // NOTE. we do not want to condition a proMP that will be modulated afterwards, this will likely corrupt the model
      ProMP copyProMPCurrentTask = new ProMP(trainingTrajectories.get(bodyPart), 20);
      // condition proMP to reach observed goal
      updateTrajectoryGoal(copyProMPCurrentTask, bodyPart, observedGoal);
      Trajectory meanTrajectoryProMPCurrentTask = new Trajectory(copyProMPCurrentTask.generate_trajectory(), 1.0);
      // infer the new speed for the mean trajectory based on observed (portion of) trajectory
      double inferredSpeed = meanTrajectoryProMPCurrentTask.infer_speed(observedTrajectory, 0.25, 4.0, 30);
      // find equivalent timesteps
      int inferredTimesteps = (int) (meanTrajectoryProMPCurrentTask.timesteps() / inferredSpeed);
      LogTools.info("Inferred timesteps: {}", inferredTimesteps);
      // update the time modulation of the learned ProMPs with estimated value
      for (ProMP proMPBodyPart : learnedProMPs.values())
         proMPBodyPart.update_time_modulation((double) proMPBodyPart.get_traj_length() / inferredTimesteps);
   }

   /* transform trajectory from list of setposes to EigenMatrixXd */
   private EigenMatrixXd toEigenMatrix(List<Pose3DReadOnly> frameList, String bodyPart)
   {
      EigenMatrixXd matrix = null;
      if (bodyPartsGeometry.get(bodyPart).equals("Orientation"))
      {
         matrix = new EigenMatrixXd(frameList.size(), 4);
         for (int i = 0; i < matrix.rows(); i++)
         {
            matrix.apply(i, 0).put(frameList.get(i).getOrientation().getX());
            matrix.apply(i, 1).put(frameList.get(i).getOrientation().getY());
            matrix.apply(i, 2).put(frameList.get(i).getOrientation().getZ());
            matrix.apply(i, 3).put(frameList.get(i).getOrientation().getS());
         }
      }
      else if (bodyPartsGeometry.get(bodyPart).equals("Position"))
      {
         matrix = new EigenMatrixXd(frameList.size(), 3);
         for (int i = 0; i < matrix.rows(); i++)
         {
            matrix.apply(i, 0).put(frameList.get(i).getPosition().getX());
            matrix.apply(i, 1).put(frameList.get(i).getPosition().getY());
            matrix.apply(i, 2).put(frameList.get(i).getPosition().getZ());
         }
      }
      else if (bodyPartsGeometry.get(bodyPart).equals("Pose"))
      {
         matrix = new EigenMatrixXd(frameList.size(), 7);
         for (int i = 0; i < matrix.rows(); i++)
         {
            matrix.apply(i, 0).put(frameList.get(i).getOrientation().getX());
            matrix.apply(i, 1).put(frameList.get(i).getOrientation().getY());
            matrix.apply(i, 2).put(frameList.get(i).getOrientation().getZ());
            matrix.apply(i, 3).put(frameList.get(i).getOrientation().getS());
            matrix.apply(i, 4).put(frameList.get(i).getPosition().getX());
            matrix.apply(i, 5).put(frameList.get(i).getPosition().getY());
            matrix.apply(i, 6).put(frameList.get(i).getPosition().getZ());
         }
      }
      return matrix;
   }

   /* update the predicted trajectories based on observed setposes */
   public void updateTaskTrajectories(HashMap<String, Pose3DReadOnly> bodyPartObservedPose, int conditioningTimestep)
   {
      // condition ProMP to reach point at given timestep
      for (String bodyPart : learnedProMPs.keySet())
      {
         updateTaskTrajectory(bodyPart, bodyPartObservedPose.get(bodyPart), conditioningTimestep);
      }
   }

   /* update the predicted trajectory based on observed setpose */
   public void updateTaskTrajectory(String bodyPart, Pose3DReadOnly observedPose, int conditioningTimestep)
   {
      updateTrajectory(learnedProMPs.get(bodyPart), bodyPart, observedPose, conditioningTimestep);
   }

   private void updateTrajectory(ProMP myProMP, String bodyPart, Pose3DReadOnly observedPose, int conditioningTimestep)
   {
      myProMP.set_conditioning_ridge_factor(0.0001);
      int proMPDimensions = (int) myProMP.get_dims();
      EigenVectorXd viaPoint = new EigenVectorXd(proMPDimensions);
      //build std deviation matrix
      EigenMatrixXd viaPointStdDeviation = new EigenMatrixXd(proMPDimensions, proMPDimensions);
      for (int i = 0; i < viaPointStdDeviation.rows(); i++)
      {
         for (int j = 0; j < viaPointStdDeviation.cols(); j++)
         {
            if (i == j)
               viaPointStdDeviation.apply(i, j).put(0.00001); // generally std deviation is low, unless you have high observation uncertainty
            else
               viaPointStdDeviation.apply(i, j).put(0);
         }
      }
      setViaPoint(viaPoint, bodyPart, observedPose);
      myProMP.condition_via_point(conditioningTimestep, viaPoint, viaPointStdDeviation);
   }

   /* update the predicted trajectory based on observed goal */
   public void updateTaskTrajectoriesGoal(HashMap<String, Pose3DReadOnly> bodyPartObservedPose)
   {
      // condition ProMP to reach end point
      for (String bodyPart : learnedProMPs.keySet())
      {
         updateTaskTrajectoryGoal(bodyPart, bodyPartObservedPose.get(bodyPart));
      }
   }

   /* update the predicted trajectory based on observed goal */
   public void updateTaskTrajectoryGoal(String bodyPart, Pose3DReadOnly observedPose)
   {
      updateTrajectoryGoal(learnedProMPs.get(bodyPart), bodyPart, observedPose);
   }

   private void updateTrajectoryGoal(ProMP myProMP, String bodyPart, Pose3DReadOnly observedPose)
   {
      // condition ProMP to reach end point
      myProMP.set_conditioning_ridge_factor(0.0001);
      int proMPDimensions = (int) myProMP.get_dims();
      EigenVectorXd viaPoint = new EigenVectorXd(proMPDimensions);
      //build std deviation matrix
      EigenMatrixXd viaPointStdDeviation = new EigenMatrixXd(proMPDimensions, proMPDimensions);
      for (int i = 0; i < viaPointStdDeviation.rows(); i++)
      {
         for (int j = 0; j < viaPointStdDeviation.cols(); j++)
         {
            if (i == j)
               viaPointStdDeviation.apply(i, j).put(0.00001); // generally std deviation is low, unless you have high observation uncertainty
            else
               viaPointStdDeviation.apply(i, j).put(0);
         }
      }
      setViaPoint(viaPoint, bodyPart, observedPose);
      myProMP.condition_goal(viaPoint, viaPointStdDeviation);
   }

   private void setViaPoint(EigenVectorXd viaPoint, String bodyPart, Pose3DReadOnly observedPose)
   {
      if (bodyPartsGeometry.get(bodyPart).equals("Position"))
      {
         viaPoint.apply(0).put(observedPose.getPosition().getX());
         viaPoint.apply(1).put(observedPose.getPosition().getY());
         viaPoint.apply(2).put(observedPose.getPosition().getZ());
      }
      else if (bodyPartsGeometry.get(bodyPart).equals("Orientation"))
      {
         viaPoint.apply(0).put(observedPose.getOrientation().getX());
         viaPoint.apply(1).put(observedPose.getOrientation().getY());
         viaPoint.apply(2).put(observedPose.getOrientation().getZ());
         viaPoint.apply(3).put(observedPose.getOrientation().getS());
      }
      else if (bodyPartsGeometry.get(bodyPart).equals("Pose"))
      {
         viaPoint.apply(0).put(observedPose.getOrientation().getX());
         viaPoint.apply(1).put(observedPose.getOrientation().getY());
         viaPoint.apply(2).put(observedPose.getOrientation().getZ());
         viaPoint.apply(3).put(observedPose.getOrientation().getS());
         viaPoint.apply(4).put(observedPose.getPosition().getX());
         viaPoint.apply(5).put(observedPose.getPosition().getY());
         viaPoint.apply(6).put(observedPose.getPosition().getZ());
      }
   }

   /* generate mean of predicted trajectory as a list of frame poses */
   public List<FramePose3D> generateTaskTrajectory(String bodyPart)
   {
      EigenMatrixXd meanTrajectoryConditioned = learnedProMPs.get(bodyPart).generate_trajectory();
      return toFrameList(meanTrajectoryConditioned, bodyPart);
   }

   /* transform trajectory from list of frame poses to EigenMatrixXd */
   private List<FramePose3D> toFrameList(EigenMatrixXd matrix, String bodyPart)
   {
      List<FramePose3D> frameList = new ArrayList<>();
      for (int i = 0; i < matrix.rows(); i++)
      {
         FramePose3D setPose = new FramePose3D();
         if (bodyPartsGeometry.get(bodyPart).equals("Position"))
         {
            setPose.getPosition().set(matrix.coeff(i, 0), matrix.coeff(i, 1), matrix.coeff(i, 2));
         }
         else if (bodyPartsGeometry.get(bodyPart).equals("Orientation"))
         {
            setPose.getOrientation().set(matrix.coeff(i, 0), matrix.coeff(i, 1), matrix.coeff(i, 2), matrix.coeff(i, 3));
         }
         else if (bodyPartsGeometry.get(bodyPart).equals("Pose"))
         {
            setPose.getOrientation().set(matrix.coeff(i, 0), matrix.coeff(i, 1), matrix.coeff(i, 2), matrix.coeff(i, 3));
            setPose.getPosition().set(matrix.coeff(i, 4), matrix.coeff(i, 5), matrix.coeff(i, 6));
         }
         frameList.add(setPose);
      }
      return frameList;
   }

   public TrajectoryGroup getTrainingTrajectories(String bodyPart)
   {
      return trainingTrajectories.get(bodyPart);
   }

   public HashMap<String, ProMP> getLearnedProMPs()
   {
      return learnedProMPs;
   }

   public ProMP getLearnedProMP(String bodyPart)
   {
      return learnedProMPs.get(bodyPart);
   }

   public String getTaskName()
   {
      return taskName;
   }

   public HashMap<String, String> getBodyPartsGeometry()
   {
      return bodyPartsGeometry;
   }
}
