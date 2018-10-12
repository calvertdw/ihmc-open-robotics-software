package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DBasics;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoFramePoint3D;

public class YoContactPoint implements ContactPointInterface
{
   private final YoVariableRegistry registry;
   private final YoFramePoint3D yoPosition;
   private final YoBoolean isInContact;
   private final String namePrefix;
   private final PlaneContactState parentContactState;

   public YoContactPoint(String namePrefix, int index, FramePoint2D contactPointPosition2d, PlaneContactState parentContactState,
         YoVariableRegistry parentRegistry)
   {
      this(namePrefix, index, contactPointPosition2d.getReferenceFrame(), parentContactState, parentRegistry);
      setPosition(contactPointPosition2d);
   }

   public YoContactPoint(String namePrefix, int index, FramePoint3D contactPointPosition, PlaneContactState parentContactState, YoVariableRegistry parentRegistry)
   {
      this(namePrefix, index, contactPointPosition.getReferenceFrame(), parentContactState, parentRegistry);
      setPosition(contactPointPosition);
   }

   public YoContactPoint(String namePrefix, int index, ReferenceFrame pointFrame, PlaneContactState parentContactState, YoVariableRegistry parentRegistry)
   {
      this.parentContactState = parentContactState;
      this.namePrefix = namePrefix;

      //TODO: Check if it is better to create an actual child registry
      registry = parentRegistry;

      yoPosition = new YoFramePoint3D(namePrefix + "Contact" + index, pointFrame, registry);
      isInContact = new YoBoolean(namePrefix + "InContact" + index, registry);
   }

   @Override
   public boolean isInContact()
   {
      return isInContact.getBooleanValue();
   }

   @Override
   public void setInContact(boolean inContact)
   {
      isInContact.set(inContact);
   }

   @Override
   public void getPosition2d(FrameTuple2DBasics framePoint2dToPack)
   {
      framePoint2dToPack.setIncludingFrame(yoPosition);
   }

   @Override
   public FramePoint3DReadOnly getPosition()
   {
      return yoPosition;
   }

   public YoFramePoint3D getYoPosition()
   {
      return yoPosition;
   }

   @Override
   public void getPosition(FramePoint3D framePointToPack)
   {
      framePointToPack.setIncludingFrame(yoPosition);
   }

   @Override
   public void getPosition2d(Tuple2DBasics position2d)
   {
      position2d.set(yoPosition.getX(), yoPosition.getY());
   }

   @Override
   public void setPosition(FrameTuple3DReadOnly position)
   {
      this.yoPosition.setMatchingFrame(position);
   }

   @Override
   public void setPosition2d(FrameTuple2DReadOnly position2d)
   {
      yoPosition.set(position2d);
   }

   public void setPosition2d(Point2D contactPointLocation)
   {
      yoPosition.set(contactPointLocation);
   }

   public void setPosition(FramePoint2DReadOnly contactPointLocation)
   {
      yoPosition.set(contactPointLocation);
   }

   @Override
   public PlaneContactState getParentContactState()
   {
      return parentContactState;
   }

   public boolean epsilonEquals(FramePoint2D contactPointPosition2d, double threshold)
   {
      yoPosition.checkReferenceFrameMatch(contactPointPosition2d);
      if (!MathTools.epsilonEquals(yoPosition.getX(), contactPointPosition2d.getX(), threshold))
         return false;
      if (!MathTools.epsilonEquals(yoPosition.getY(), contactPointPosition2d.getY(), threshold))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      return namePrefix + ", in contact: " + isInContact() + ", position: " + yoPosition.toString();
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return yoPosition.getReferenceFrame();
   }
}
