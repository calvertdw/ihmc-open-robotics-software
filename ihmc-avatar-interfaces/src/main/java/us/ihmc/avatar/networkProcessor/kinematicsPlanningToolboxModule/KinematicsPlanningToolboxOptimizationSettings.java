package us.ihmc.avatar.networkProcessor.kinematicsPlanningToolboxModule;

public class KinematicsPlanningToolboxOptimizationSettings implements SolutionQualityConvergenceSettings
{
   @Override
   public double getSolutionStabilityThreshold()
   {
      return 0.00005;
   }
}
