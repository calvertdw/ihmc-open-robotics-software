package us.ihmc.robotics.math.trajectories.trajectorypoints.interfaces;

public interface TrajectoryPointListInterface<T extends TrajectoryPointInterface>
{
   abstract void clear();

   abstract void addTrajectoryPoint(T trajectoryPoint);

   abstract T getTrajectoryPoint(int trajectoryPointIndex);

   abstract int getNumberOfTrajectoryPoints();

   default void set(TrajectoryPointListInterface<T> other)
   {
      clear();
      for (int i = 0; i < other.getNumberOfTrajectoryPoints(); i++)
      {
         addTrajectoryPoint(other.getTrajectoryPoint(i));
      }
   }

   default void addTimeOffset(double timeOffsetToAdd)
   {
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         getTrajectoryPoint(i).addTimeOffset(timeOffsetToAdd);
      }
   }

   default void subtractTimeOffset(double timeOffsetToSubtract)
   {
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         getTrajectoryPoint(i).subtractTimeOffset(timeOffsetToSubtract);
      }
   }

   default T getLastTrajectoryPoint()
   {
      return getTrajectoryPoint(getNumberOfTrajectoryPoints());
   }

   default double getTrajectoryTime()
   {
      return getLastTrajectoryPoint().getTime();
   }
}
