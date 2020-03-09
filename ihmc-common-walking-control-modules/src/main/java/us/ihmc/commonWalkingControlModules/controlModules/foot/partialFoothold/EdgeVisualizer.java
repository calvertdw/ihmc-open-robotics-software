package us.ihmc.commonWalkingControlModules.controlModules.foot.partialFoothold;

import us.ihmc.euclid.referenceFrame.FrameLine3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FrameLine2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameLine3DBasics;
import us.ihmc.graphicsDescription.plotting.artifact.Artifact;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactLineSegment2d;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFramePoint2D;

import java.awt.*;

public class EdgeVisualizer
{
   private static final double LineVizWidth = 0.1;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final FrameLine3DBasics tempLineOfRotationInWorld = new FrameLine3D();

   private final YoFramePoint2D linePointA;
   private final YoFramePoint2D linePointB;

   public EdgeVisualizer(String prefix, YoVariableRegistry registry, YoGraphicsListRegistry graphicsListRegistry)
   {
      linePointA = new YoFramePoint2D(prefix + "_FootRotationPointA", worldFrame, registry);
      linePointB = new YoFramePoint2D(prefix + "_FootRotationPointB", worldFrame, registry);

      Artifact lineArtifact = new YoArtifactLineSegment2d(prefix + "_LineOfRotation", linePointA, linePointB, Color.ORANGE, 0.005, 0.01);
      graphicsListRegistry.registerArtifact(getClass().getSimpleName(), lineArtifact);
   }

   public void reset()
   {
      linePointA.setToNaN();
      linePointB.setToNaN();
   }

   public void updateGraphics(FrameLine2DReadOnly lineOfRotation)
   {
      tempLineOfRotationInWorld.setToZero(lineOfRotation.getReferenceFrame());
      tempLineOfRotationInWorld.set(lineOfRotation);
      tempLineOfRotationInWorld.changeFrame(ReferenceFrame.getWorldFrame());

      linePointA.set(tempLineOfRotationInWorld.getDirection());
      linePointA.scale(-0.5 * LineVizWidth);
      linePointA.add(tempLineOfRotationInWorld.getPointX(), tempLineOfRotationInWorld.getPointY());

      linePointB.set(tempLineOfRotationInWorld.getDirection());
      linePointB.scale(0.5 * LineVizWidth);
      linePointB.add(tempLineOfRotationInWorld.getPointX(), tempLineOfRotationInWorld.getPointY());
   }
}
