package us.ihmc.commonWalkingControlModules.controlModules.rigidBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.commons.PrintTools;
import us.ihmc.commons.lists.RecyclingArrayDeque;
import us.ihmc.communication.packets.ExecutionMode;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.EuclideanTrajectoryControllerCommand;
import us.ihmc.robotics.controllers.pidGains.PID3DGainsReadOnly;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.math.trajectories.waypoints.MultipleWaypointsPositionTrajectoryGenerator;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SelectionMatrix3D;
import us.ihmc.robotics.weightMatrices.WeightMatrix3D;
import us.ihmc.yoVariables.providers.BooleanProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFramePoint3D;
import us.ihmc.yoVariables.variable.YoFrameVector3D;

public class RigidBodyPositionControlHelper
{
   private final PointFeedbackControlCommand feedbackControlCommand = new PointFeedbackControlCommand();

   private final MultipleWaypointsPositionTrajectoryGenerator trajectoryGenerator;
   private final FrameEuclideanTrajectoryPoint lastPointAdded = new FrameEuclideanTrajectoryPoint();
   private final RecyclingArrayDeque<FrameEuclideanTrajectoryPoint> pointQueue = new RecyclingArrayDeque<>(RigidBodyTaskspaceControlState.maxPoints,
                                                                                                           FrameEuclideanTrajectoryPoint.class,
                                                                                                           FrameEuclideanTrajectoryPoint::set);

   private boolean messageWeightValid = false;
   private final BooleanProvider useWeightFromMessage;

   private final WeightMatrix3D defaultWeightMatrix = new WeightMatrix3D();
   private final WeightMatrix3D messageWeightMatrix = new WeightMatrix3D();
   private final YoFrameVector3D currentWeight;

   private final SelectionMatrix3D selectionMatrix = new SelectionMatrix3D();

   private Vector3DReadOnly defaultWeight;
   private PID3DGainsReadOnly gains;

   private final FramePoint3D desiredPosition = new FramePoint3D();
   private final FrameVector3D desiredVelocity = new FrameVector3D();
   private final FrameVector3D feedForwardAcceleration = new FrameVector3D();

   private final BooleanProvider useBaseFrameForControl;

   private final FixedFramePoint3DBasics controlFramePosition;
   private final ReferenceFrame defaultControlFrame;

   private final ReferenceFrame bodyFrame;
   private final ReferenceFrame baseFrame;

   private final YoFramePoint3D yoCurrentPosition;
   private final YoFramePoint3D yoDesiredPosition;
   private final List<YoGraphicPosition> graphics = new ArrayList<>();

   private final String warningPrefix;

   public RigidBodyPositionControlHelper(String postfix, String warningPrefix, RigidBody bodyToControl, RigidBody baseBody, RigidBody elevator,
                                         Collection<ReferenceFrame> trajectoryFrames, ReferenceFrame controlFrame, ReferenceFrame baseFrame,
                                         BooleanProvider useBaseFrameForControl, BooleanProvider useWeightFromMessage, YoVariableRegistry registry,
                                         YoGraphicsListRegistry graphicsListRegistry)
   {
      this.warningPrefix = warningPrefix;
      this.baseFrame = baseFrame;
      this.useBaseFrameForControl = useBaseFrameForControl;
      this.useWeightFromMessage = useWeightFromMessage;

      String bodyName = bodyToControl.getName() + postfix;
      String prefix = bodyName + "TaskspacePosition";

      trajectoryGenerator = new MultipleWaypointsPositionTrajectoryGenerator(bodyName, RigidBodyTaskspaceControlState.maxPointsInGenerator, true,
                                                                             ReferenceFrame.getWorldFrame(), registry);
      if (trajectoryFrames != null)
      {
         trajectoryFrames.forEach(frame -> trajectoryGenerator.registerNewTrajectoryFrame(frame));
      }
      trajectoryGenerator.registerNewTrajectoryFrame(baseFrame);

      currentWeight = new YoFrameVector3D(prefix + "CurrentWeight", null, registry);

      feedbackControlCommand.set(elevator, bodyToControl);
      feedbackControlCommand.setPrimaryBase(baseBody);

      defaultControlFrame = controlFrame;
      bodyFrame = bodyToControl.getBodyFixedFrame();
      controlFramePosition = new FramePoint3D(bodyFrame);
      setDefaultControlFrame();

      if (graphicsListRegistry != null)
      {
         yoCurrentPosition = new YoFramePoint3D(prefix + "Current", ReferenceFrame.getWorldFrame(), registry);
         yoDesiredPosition = new YoFramePoint3D(prefix + "Desired", ReferenceFrame.getWorldFrame(), registry);
         setupViz(graphicsListRegistry, bodyName);
      }
      else
      {
         yoCurrentPosition = null;
         yoDesiredPosition = null;
      }
   }

   public void setGains(PID3DGainsReadOnly gains)
   {
      this.gains = gains;
   }

   public void setWeights(Vector3DReadOnly weights)
   {
      this.defaultWeight = weights;
   }

   private void setupViz(YoGraphicsListRegistry graphicsListRegistry, String bodyName)
   {
      String listName = getClass().getSimpleName();

      YoGraphicPosition controlPoint = new YoGraphicPosition(bodyName + "Current", yoCurrentPosition, 0.005, YoAppearance.Red());
      graphicsListRegistry.registerYoGraphic(listName, controlPoint);
      graphics.add(controlPoint);

      YoGraphicPosition desiredPoint = new YoGraphicPosition(bodyName + "Desired", yoDesiredPosition, 0.005, YoAppearance.Blue());
      graphicsListRegistry.registerYoGraphic(listName, desiredPoint);
      graphics.add(desiredPoint);
   }

   public List<YoGraphicPosition> getGraphics()
   {
      return graphics;
   }

   private void setDefaultControlFrame()
   {
      controlFramePosition.setFromReferenceFrame(defaultControlFrame);
      feedbackControlCommand.setBodyFixedPointToControl(controlFramePosition);
   }

   private void setControlFramePosition(Tuple3DReadOnly controlFramePositionInBodyFrame)
   {
      controlFramePosition.set(controlFramePositionInBodyFrame);
      feedbackControlCommand.setBodyFixedPointToControl(controlFramePosition);
   }

   public void holdCurrent()
   {
      clear();
      desiredPosition.setIncludingFrame(controlFramePosition);
      queueInitialPoint(desiredPosition);
   }

   public void holdCurrentDesired()
   {
      // Compute the desired position in the body frame.
      getDesiredPosition(desiredPosition);
      desiredPosition.changeFrame(controlFramePosition.getReferenceFrame());
      desiredPosition.sub(controlFramePosition);

      clear();

      // Move the desired position to the control frame
      desiredPosition.add(controlFramePosition);
      queueInitialPoint(desiredPosition);
   }

   public void goToPositionFromCurrent(FramePoint3DReadOnly position, double trajectoryTime)
   {
      holdCurrent();

      FrameEuclideanTrajectoryPoint trajectoryPoint = pointQueue.addLast();
      trajectoryPoint.setToZero(baseFrame);
      trajectoryPoint.setTime(trajectoryTime);

      desiredPosition.setIncludingFrame(position);
      desiredPosition.changeFrame(baseFrame);
      trajectoryPoint.setPosition(desiredPosition);
   }

   public void goToPosition(FramePoint3DReadOnly position, double trajectoryTime)
   {
      holdCurrentDesired();

      FrameEuclideanTrajectoryPoint trajectoryPoint = pointQueue.addLast();
      trajectoryPoint.setToZero(baseFrame);
      trajectoryPoint.setTime(trajectoryTime);

      desiredPosition.setIncludingFrame(position);
      desiredPosition.changeFrame(baseFrame);
      trajectoryPoint.setPosition(desiredPosition);
   }

   public void getDesiredPosition(FixedFramePoint3DBasics positionToPack)
   {
      if (trajectoryGenerator.isEmpty())
      {
         positionToPack.setMatchingFrame(controlFramePosition);
      }
      else
      {
         trajectoryGenerator.getPosition(desiredPosition);
         positionToPack.setMatchingFrame(desiredPosition);
      }
   }

   public boolean doAction(double timeInTrajectory)
   {
      boolean done = false;
      if (trajectoryGenerator.isDone() || trajectoryGenerator.getLastWaypointTime() <= timeInTrajectory)
      {
         done = fillAndReinitializeTrajectories();
      }

      trajectoryGenerator.compute(timeInTrajectory);
      trajectoryGenerator.getLinearData(desiredPosition, desiredVelocity, feedForwardAcceleration);

      desiredPosition.changeFrame(ReferenceFrame.getWorldFrame());
      desiredVelocity.changeFrame(ReferenceFrame.getWorldFrame());
      feedForwardAcceleration.changeFrame(ReferenceFrame.getWorldFrame());

      feedbackControlCommand.set(desiredPosition, desiredVelocity);
      feedbackControlCommand.setFeedForwardAction(feedForwardAcceleration);
      feedbackControlCommand.setGains(gains);

      // This will improve the tracking with respect to moving trajectory frames.
      if (useBaseFrameForControl.getValue())
      {
         feedbackControlCommand.setControlBaseFrame(trajectoryGenerator.getCurrentTrajectoryFrame());
      }
      else
      {
         feedbackControlCommand.resetControlBaseFrame();
      }

      // Update the QP weight and selection YoVariables:
      defaultWeightMatrix.set(defaultWeight);
      defaultWeightMatrix.setWeightFrame(null);
      WeightMatrix3D weightMatrix = useWeightFromMessage.getValue() ? messageWeightMatrix : defaultWeightMatrix;
      currentWeight.set(weightMatrix);

      feedbackControlCommand.setWeightMatrix(weightMatrix);
      feedbackControlCommand.setSelectionMatrix(selectionMatrix);

      if (yoCurrentPosition != null && yoDesiredPosition != null)
      {
         yoCurrentPosition.setMatchingFrame(controlFramePosition);
         yoDesiredPosition.setMatchingFrame(desiredPosition);
      }

      return done;
   }

   private boolean fillAndReinitializeTrajectories()
   {
      if (pointQueue.isEmpty())
      {
         return true;
      }

      if (!trajectoryGenerator.isEmpty())
      {
         trajectoryGenerator.clear();
         lastPointAdded.changeFrame(trajectoryGenerator.getCurrentTrajectoryFrame());
         trajectoryGenerator.appendWaypoint(lastPointAdded);
      }

      int currentNumberOfWaypoints = trajectoryGenerator.getCurrentNumberOfWaypoints();
      int pointsToAdd = RigidBodyTaskspaceControlState.maxPointsInGenerator - currentNumberOfWaypoints;
      for (int pointIdx = 0; pointIdx < pointsToAdd; pointIdx++)
      {
         if (pointQueue.isEmpty())
            break;

         FrameEuclideanTrajectoryPoint pointToAdd = pointQueue.pollFirst();
         lastPointAdded.setIncludingFrame(pointToAdd);
         trajectoryGenerator.appendWaypoint(pointToAdd);
      }

      trajectoryGenerator.initialize();
      return false;
   }

   public boolean handleTrajectoryCommand(EuclideanTrajectoryControllerCommand command)
   {
      if (command.getExecutionMode() == ExecutionMode.OVERRIDE || isEmpty())
      {
         // Compute the initial desired position for the body frame.
         getDesiredPosition(desiredPosition);
         desiredPosition.changeFrame(controlFramePosition.getReferenceFrame());
         desiredPosition.sub(controlFramePosition);

         clear();

         // Set the new control frame and move the initial desired position to be for that frame.
         if (command.useCustomControlFrame())
         {
            setControlFramePosition(command.getControlFramePose().getTranslationVector());
         }
         desiredPosition.add(controlFramePosition);

         trajectoryGenerator.changeFrame(command.getTrajectoryFrame());
         selectionMatrix.set(command.getSelectionMatrix());

         if (command.getTrajectoryPoint(0).getTime() > RigidBodyTaskspaceControlState.timeEpsilonForInitialPoint)
         {
            queueInitialPoint(desiredPosition);
         }

         messageWeightMatrix.set(command.getWeightMatrix());
         boolean messageHasValidWeights = true;
         for (int i = 0; i < 3; i++)
         {
            double weight = messageWeightMatrix.getElement(i);
            messageHasValidWeights = messageHasValidWeights && !(Double.isNaN(weight) || weight < 0.0);
         }
         messageWeightValid = messageHasValidWeights;
      }
      else if (command.getTrajectoryFrame() != trajectoryGenerator.getCurrentTrajectoryFrame())
      {
         PrintTools.warn(warningPrefix + "Was executing in " + trajectoryGenerator.getCurrentTrajectoryFrame() + " can not switch to "
               + command.getTrajectoryFrame() + " without override.");
         return false;
      }
      else if (!selectionMatrix.equals(command.getSelectionMatrix()))
      {
         PrintTools.warn(warningPrefix + "Received a change of selection matrix without an override. Was\n" + selectionMatrix + "\nRequested\n"
               + command.getSelectionMatrix());
         return false;
      }

      command.getTrajectoryPointList().changeFrame(trajectoryGenerator.getCurrentTrajectoryFrame());
      for (int i = 0; i < command.getNumberOfTrajectoryPoints(); i++)
      {
         if (!checkTime(command.getTrajectoryPoint(i).getTime()))
            return false;
         if (!queuePoint(command.getTrajectoryPoint(i)))
            return false;
      }

      return true;
   }

   public boolean isMessageWeightValid()
   {
      return messageWeightValid;
   }

   public int getNumberOfPointsInQueue()
   {
      return pointQueue.size();
   }

   public int getNumberOfPointsInGenerator()
   {
      return trajectoryGenerator.getCurrentNumberOfWaypoints();
   }

   private void queueInitialPoint(FramePoint3D initialPosition)
   {
      initialPosition.changeFrame(trajectoryGenerator.getCurrentTrajectoryFrame());
      FrameEuclideanTrajectoryPoint initialPoint = pointQueue.addLast();
      initialPoint.setToZero(trajectoryGenerator.getCurrentTrajectoryFrame());
      initialPoint.setTime(0.0);
      initialPoint.setPosition(initialPosition);
   }

   private boolean queuePoint(FrameEuclideanTrajectoryPoint trajectoryPoint)
   {
      if (pointQueue.size() >= RigidBodyTaskspaceControlState.maxPoints)
      {
         PrintTools.info(warningPrefix + "Reached maximum capacity of " + RigidBodyTaskspaceControlState.maxPoints + " can not execute trajectory.");
         return false;
      }

      pointQueue.addLast().setIncludingFrame(trajectoryPoint);
      return true;
   }

   private boolean checkTime(double time)
   {
      if (time <= getLastTrajectoryPointTime())
      {
         PrintTools.warn(warningPrefix + "Time in trajectory must be strictly increasing.");
         return false;
      }
      return true;
   }

   public PointFeedbackControlCommand getFeedbackControlCommand()
   {
      return feedbackControlCommand;
   }

   public void onExit()
   {
      clear();
   }

   public boolean isEmpty()
   {
      if (!pointQueue.isEmpty())
      {
         return false;
      }
      return trajectoryGenerator.isDone();
   }

   public double getLastTrajectoryPointTime()
   {
      if (isEmpty())
      {
         return Double.NEGATIVE_INFINITY;
      }
      else if (pointQueue.isEmpty())
      {
         return trajectoryGenerator.getLastWaypointTime();
      }
      else
      {
         return pointQueue.peekLast().getTime();
      }
   }

   public void clear()
   {
      selectionMatrix.resetSelection();
      trajectoryGenerator.clear(baseFrame);
      setDefaultControlFrame();
      pointQueue.clear();
   }

   public void disable()
   {
      clear();
      holdCurrentDesired();
      selectionMatrix.clearSelection();
   }
}
