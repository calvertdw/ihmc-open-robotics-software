package us.ihmc.wholeBodyController.diagnostics.utils;

import java.util.ArrayDeque;
import java.util.List;

import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.wholeBodyController.diagnostics.DiagnosticDataReporter;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class DiagnosticParallelTask extends DiagnosticTask
{
   private final DiagnosticTask[] diagnosticTasks;

   public DiagnosticParallelTask(DiagnosticTask... diagnosticTasks)
   {
      this.diagnosticTasks = diagnosticTasks;
   }

   public DiagnosticParallelTask(List<? extends DiagnosticTask> diagnosticTasks)
   {
      this.diagnosticTasks = new DiagnosticTask[diagnosticTasks.size()];
      diagnosticTasks.toArray(this.diagnosticTasks);
   }

   @Override
   void setYoTimeInCurrentTask(YoDouble timeInCurrentTask)
   {
      super.setYoTimeInCurrentTask(timeInCurrentTask);

      for (int i = 0; i < diagnosticTasks.length; i++)
         diagnosticTasks[i].setYoTimeInCurrentTask(timeInCurrentTask);
   }

   @Override
   public void doTransitionIntoAction()
   {
      for (int i = 0; i < diagnosticTasks.length; i++)
         diagnosticTasks[i].doTransitionIntoAction();
   }

   @Override
   public void doAction()
   {
      for (int i = 0; i < diagnosticTasks.length; i++)
      {
         if (!diagnosticTasks[i].isDone())
            diagnosticTasks[i].doAction();
      }
   }

   @Override
   public void doTransitionOutOfAction()
   {
      for (int i = 0; i < diagnosticTasks.length; i++)
         diagnosticTasks[i].doTransitionOutOfAction();
   }

   @Override
   public boolean isDone()
   {
      for (int i = 0; i < diagnosticTasks.length; i++)
         if (!diagnosticTasks[i].isDone())
            return false;
      return true;
   }

   @Override
   public boolean abortRequested()
   {
      for (int i = 0; i < diagnosticTasks.length; i++)
         if (diagnosticTasks[i].abortRequested())
            return true;
      return false;
   }

   public double getDesiredJointPositionOffset(OneDoFJointBasics joint)
   {
      double desiredJointPositionOffset = 0.0;

      for (int i = 0; i < diagnosticTasks.length; i++)
         desiredJointPositionOffset += diagnosticTasks[i].getDesiredJointPositionOffset(joint);

      return desiredJointPositionOffset;
   }

   public double getDesiredJointVelocityOffset(OneDoFJointBasics joint)
   {
      double desiredJointVelocityOffset = 0.0;

      for (int i = 0; i < diagnosticTasks.length; i++)
         desiredJointVelocityOffset += diagnosticTasks[i].getDesiredJointVelocityOffset(joint);

      return desiredJointVelocityOffset;
   }

   public double getDesiredJointTauOffset(OneDoFJointBasics joint)
   {
      double desiredJointTauOffset = 0.0;

      for (int i = 0; i < diagnosticTasks.length; i++)
         desiredJointTauOffset += diagnosticTasks[i].getDesiredJointTauOffset(joint);

      return desiredJointTauOffset;
   }

   @Override
   public void getDataReporterToRun(ArrayDeque<DiagnosticDataReporter> dataReportersToPack)
   {
      for (int i = 0; i < diagnosticTasks.length; i++)
         diagnosticTasks[i].getDataReporterToRun(dataReportersToPack);
   }

   @Override
   public void attachParentYoVariableRegistry(YoRegistry parentRegistry)
   {
      for (int i = 0; i < diagnosticTasks.length; i++)
         diagnosticTasks[i].attachParentYoVariableRegistry(parentRegistry);
   }

   @Override
   public String getName()
   {
      String ret = "{ ";
      for (int i = 0; i < diagnosticTasks.length - 1; i++)
         ret += diagnosticTasks[i].getName() + ", ";
      ret += diagnosticTasks[diagnosticTasks.length - 1].getName() + " }";
      return ret;
   }
}
