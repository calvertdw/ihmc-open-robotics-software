package us.ihmc.humanoidBehaviors.ui.behaviors;

import javafx.scene.paint.Color;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.humanoidBehaviors.ui.graphics.OrientationGraphic;
import us.ihmc.humanoidBehaviors.ui.graphics.SnappedPositionGraphic;
import us.ihmc.humanoidBehaviors.ui.model.interfaces.OrientationEditable;
import us.ihmc.humanoidBehaviors.ui.model.interfaces.PositionEditable;

public class PatrolWaypoint implements PositionEditable, OrientationEditable
{
   private final SnappedPositionGraphic snappedPositionGraphic;
   private final OrientationGraphic orientationGraphic;

   public PatrolWaypoint()
   {
      snappedPositionGraphic = new SnappedPositionGraphic(Color.YELLOW);
      orientationGraphic = new OrientationGraphic(snappedPositionGraphic);
   }

   @Override
   public void setMouseTransparent(boolean transparent)
   {
      snappedPositionGraphic.getSphere().setMouseTransparent(transparent);
      orientationGraphic.getArrow().setMouseTransparent(transparent);
   }

   @Override
   public void setPosition(Point3D position)
   {
      snappedPositionGraphic.setPosition(position);
      orientationGraphic.setPosition(position);
   }

   @Override
   public void setOrientation(Point3D orientationPoint)
   {
      orientationGraphic.setOrientation(orientationPoint);
   }

   public SnappedPositionGraphic getSnappedPositionGraphic()
   {
      return snappedPositionGraphic;
   }

   public OrientationGraphic getOrientationGraphic()
   {
      return orientationGraphic;
   }
}
