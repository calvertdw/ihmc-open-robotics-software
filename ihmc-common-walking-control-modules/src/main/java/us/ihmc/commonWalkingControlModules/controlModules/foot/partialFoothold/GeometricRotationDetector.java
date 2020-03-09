package us.ihmc.commonWalkingControlModules.controlModules.foot.partialFoothold;

import us.ihmc.commonWalkingControlModules.controlModules.foot.ExplorationParameters;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoFrameVector3D;

/**
 * This class is designed to detect whether or not the foot is rotating. It does this by looking at the orientation of the foot with respect
 * to the world, finding the angle between the ground plane normal and the foot sole normal. The problem with this approach is that it assumes
 * that the ground plane is flat.
 */
public class GeometricRotationDetector implements FootRotationDetector
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoFrameVector3D groundPlaneNormal;
   private final FrameVector3D footNormal = new FrameVector3D();

   private final YoDouble angleFootGround;
   private final YoDouble angleThreshold;
   private final YoBoolean isRotating;

   private final ReferenceFrame soleFrame;

   public GeometricRotationDetector(RobotSide side, ReferenceFrame soleFrame, FootholdRotationParameters explorationParameters, YoVariableRegistry parentRegistry)
   {
      this.soleFrame = soleFrame;

      String namePrefix = side.getLowerCaseName() + "Kinematic";
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());

      groundPlaneNormal = new YoFrameVector3D(namePrefix + "PlaneNormal", worldFrame, registry);
      groundPlaneNormal.setZ(1.0);

      angleFootGround = new YoDouble(namePrefix + "AngleToGround", registry);
      angleThreshold = explorationParameters.getGeometricDetectionAngleThreshold();
      isRotating = new YoBoolean(namePrefix + "IsRotating", registry);

      parentRegistry.addChild(registry);
   }

   public void reset()
   {
      isRotating.set(false);
      angleFootGround.setToNaN();
   }

   public boolean compute()
   {
      footNormal.setIncludingFrame(soleFrame, 0.0, 0.0, 1.0);
      footNormal.changeFrame(worldFrame);

      double cosAlpha = Math.abs(groundPlaneNormal.dot(footNormal));
      double alpha = Math.acos(cosAlpha);
      angleFootGround.set(alpha);
      if (!isRotating.getBooleanValue())
         isRotating.set(alpha > angleThreshold.getDoubleValue());

      return isRotating.getBooleanValue();
   }

   public boolean isRotating()
   {
      return isRotating.getBooleanValue();
   }
}
