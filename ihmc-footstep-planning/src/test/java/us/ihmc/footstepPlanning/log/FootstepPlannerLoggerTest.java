package us.ihmc.footstepPlanning.log;

import controller_msgs.msg.dds.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlannerOutput;
import us.ihmc.footstepPlanning.FootstepPlannerRequest;
import us.ihmc.footstepPlanning.FootstepPlanningModule;
import us.ihmc.footstepPlanning.tools.FootstepPlannerMessageTools;
import us.ihmc.pathPlanning.DataSet;
import us.ihmc.pathPlanning.DataSetIOTools;
import us.ihmc.pathPlanning.DataSetName;
import us.ihmc.robotics.robotSide.RobotSide;

import java.io.File;

public class FootstepPlannerLoggerTest
{
   @Test
   public void testLogger()
   {
      FootstepPlanningModule planningModule = new FootstepPlanningModule("testModule");
      DataSet dataSet = DataSetIOTools.loadDataSet(DataSetName._20190220_172417_EOD_Cinders);

      FootstepPlannerRequest request = new FootstepPlannerRequest();
      request.setStartFootPose(new Pose3D(dataSet.getPlannerInput().getStartPosition(), new Quaternion(dataSet.getPlannerInput().getStartYaw(), 0.0, 0.0)));
      request.setRequestedInitialStanceSide(RobotSide.LEFT);
      request.setGoalPose(new Pose3D(dataSet.getPlannerInput().getGoalPosition(), new Quaternion(dataSet.getPlannerInput().getGoalYaw(), 0.0, 0.0)));
      request.setPlanarRegionsList(dataSet.getPlanarRegionsList());
      request.setAssumeFlatGround(false);
      request.setPlanBodyPath(true);

      planningModule.getFootstepPlannerParameters().setMaximumStepZ(0.294);
      planningModule.getFootstepPlannerParameters().setYawWeight(0.17);
      planningModule.getFootstepPlannerParameters().setMaximumStepZWhenSteppingUp(0.4);
      planningModule.getFootstepPlannerParameters().setMaximumZPenetrationOnValleyRegions(1.0);
      planningModule.getVisibilityGraphParameters().setNavigableExtrusionDistance(0.01);
      planningModule.getVisibilityGraphParameters().setExplorationDistanceFromStartGoal(50.0);

      FootstepPlannerOutput plannerOutput = planningModule.handleRequest(request);
      String logDirectory = System.getProperty("user.home") + File.separator + "testLog" + File.separator;

      FootstepPlannerLogger logger = new FootstepPlannerLogger(planningModule);
      boolean success = logger.logSession(logDirectory);
      Assertions.assertTrue(success, "Error generating footstep planner log");

      FootstepPlannerLogLoader logLoader = new FootstepPlannerLogLoader();
      success = logLoader.load(new File(logDirectory));
      Assertions.assertTrue(success, "Error loading footstep planner log");

      FootstepPlannerLog log = logLoader.getLog();

      FootstepPlanningRequestPacket expectedRequestPacket = new FootstepPlanningRequestPacket();
      FootstepPlannerParametersPacket expectedFootstepParameters = new FootstepPlannerParametersPacket();
      VisibilityGraphsParametersPacket expectedBodyPathParameters = new VisibilityGraphsParametersPacket();
      FootstepPlanningToolboxOutputStatus expectedOutputStatusPacket = new FootstepPlanningToolboxOutputStatus();

      request.setPacket(expectedRequestPacket);
      FootstepPlannerMessageTools.copyParametersToPacket(expectedFootstepParameters, planningModule.getFootstepPlannerParameters());
      FootstepPlannerMessageTools.copyParametersToPacket(expectedBodyPathParameters, planningModule.getVisibilityGraphParameters());
      plannerOutput.setPacket(expectedOutputStatusPacket);

      Assertions.assertTrue(expectedRequestPacket.epsilonEquals(log.getRequestPacket(), 1e-5));
      Assertions.assertTrue(expectedFootstepParameters.epsilonEquals(log.getFootstepParametersPacket(), 1e-5));
      Assertions.assertTrue(expectedBodyPathParameters.epsilonEquals(log.getBodyPathParametersPacket(), 1e-5));
      Assertions.assertTrue(expectedOutputStatusPacket.epsilonEquals(log.getStatusPacket(), 1e-5));
   }
}
