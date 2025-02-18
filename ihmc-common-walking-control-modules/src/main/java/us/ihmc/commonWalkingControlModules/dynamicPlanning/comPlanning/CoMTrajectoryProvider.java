package us.ihmc.commonWalkingControlModules.dynamicPlanning.comPlanning;

import us.ihmc.robotics.math.trajectories.generators.MultipleSegmentPositionTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.interfaces.FixedFramePositionTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.interfaces.Polynomial3DBasics;
import us.ihmc.robotics.math.trajectories.interfaces.Polynomial3DReadOnly;
import us.ihmc.robotics.math.trajectories.interfaces.PositionTrajectoryGenerator;

import java.util.List;

public interface CoMTrajectoryProvider extends CoMTrajectoryPlannerInterface
{
   List<? extends Polynomial3DReadOnly> getVRPTrajectories();

   MultipleSegmentPositionTrajectoryGenerator<?> getCoMTrajectory();

   @Override
   default int getSegmentNumber(double time)
   {
      double startTime = 0.0;
      for (int i = 0; i < getVRPTrajectories().size(); i++)
      {
         if (getVRPTrajectories().get(i).timeIntervalContains(time - startTime))
            return i;

         startTime += getVRPTrajectories().get(i).getDuration();
      }

      return -1;
   }

   @Override
   default double getTimeInSegment(int segmentNumber, double time)
   {
      for (int i = 0; i < segmentNumber; i++)
         time -= getVRPTrajectories().get(i).getDuration();

      return time;
   }

}
