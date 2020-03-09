package us.ihmc.commonWalkingControlModules.controlModules.foot.partialFoothold;

import us.ihmc.euclid.referenceFrame.*;
import us.ihmc.euclid.referenceFrame.interfaces.FrameLine2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint2DReadOnly;
import us.ihmc.graphicsDescription.plotting.artifact.Artifact;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactLineSegment2d;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.statistics.Line2DStatisticsCalculator;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFrameLine2D;
import us.ihmc.yoVariables.variable.YoFramePoint2D;
import us.ihmc.yoVariables.variable.YoFrameVector2D;
import us.ihmc.yoVariables.variable.YoFrameVector3D;

import java.awt.Color;

/**
 * This calculates the edge by looking at the direction of rotation as the cross product between the foot normal and the ground plane normal. The location of
 * the line is then centered on the measured Center of Pressure. The problem with this approach is that it assumes that the ground plane normal is vertical.
 */
public class GeometricRotationEdgeCalculator implements RotationEdgeCalculator
{
   private final static ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final MovingReferenceFrame soleFrame;

   private final FrameVector3D lineOfContact = new FrameVector3D();
   private final FrameVector3D footNormal = new FrameVector3D();
   private final YoFrameVector3D groundPlaneNormal;

   private final FrameLine2D lineOfRotationInWorldFrame = new FrameLine2D();

   private final YoFrameLine2D lineOfRotationInSole;

   private final Line2DStatisticsCalculator lineOfRotationStandardDeviation;

   private final EdgeVisualizer edgeVisualizer;

   public GeometricRotationEdgeCalculator(RobotSide side, MovingReferenceFrame soleFrame, YoVariableRegistry parentRegistry,
                                          YoGraphicsListRegistry graphicsListRegistry)
   {
      this.soleFrame = soleFrame;

      String namePrefix = side.getLowerCaseName() + "Geometric";
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());

      YoFramePoint2D point = new YoFramePoint2D(namePrefix + "PointOfRotation", soleFrame, registry);
      YoFrameVector2D direction = new YoFrameVector2D(namePrefix + "AxisOfRotation", soleFrame, registry);
      lineOfRotationInSole = new YoFrameLine2D(point, direction);

      groundPlaneNormal = new YoFrameVector3D(namePrefix + "PlaneNormal", worldFrame, registry);
      groundPlaneNormal.setZ(1.0);

      lineOfRotationStandardDeviation = new Line2DStatisticsCalculator(namePrefix + "LineOfRotation", lineOfRotationInSole, registry);

      if (graphicsListRegistry != null)
         edgeVisualizer = new EdgeVisualizer(namePrefix, Color.GREEN, registry, graphicsListRegistry);
      else
         edgeVisualizer = null;

      reset();

      parentRegistry.addChild(registry);
   }

   private final FramePoint3D tempPointOfRotation = new FramePoint3D();

   public void compute(FramePoint2DReadOnly measuredCoP)
   {
      // intersect the foot plane and the ground plane
      footNormal.setIncludingFrame(soleFrame, 0.0, 0.0, 1.0);
      footNormal.changeFrame(worldFrame);
      lineOfContact.cross(groundPlaneNormal, footNormal);

      tempPointOfRotation.setIncludingFrame(measuredCoP, 0.0);
      lineOfRotationInWorldFrame.setToZero(worldFrame);
      lineOfRotationInWorldFrame.set(tempPointOfRotation, lineOfContact);
      lineOfRotationInSole.setMatchingFrame(lineOfRotationInWorldFrame);

      if (edgeVisualizer != null)
         edgeVisualizer.updateGraphics(lineOfRotationInSole);
   }

   public void reset()
   {
      if (edgeVisualizer != null)
         edgeVisualizer.reset();

      lineOfRotationInSole.setToZero();
      lineOfRotationStandardDeviation.reset();
   }

   public FrameLine2DReadOnly getLineOfRotation()
   {
      return lineOfRotationInSole;
   }
}
