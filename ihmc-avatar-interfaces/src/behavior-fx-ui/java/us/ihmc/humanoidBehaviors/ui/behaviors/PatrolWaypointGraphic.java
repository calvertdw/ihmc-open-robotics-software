package us.ihmc.humanoidBehaviors.ui.behaviors;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.humanoidBehaviors.ui.graphics.OrientationGraphic;
import us.ihmc.humanoidBehaviors.ui.graphics.SnappedPositionGraphic;
import us.ihmc.humanoidBehaviors.ui.model.interfaces.OrientationEditable;
import us.ihmc.humanoidBehaviors.ui.model.interfaces.PoseEditable;
import us.ihmc.humanoidBehaviors.ui.model.interfaces.PositionEditable;

public class PatrolWaypointGraphic extends Group implements PoseEditable
{
   private final SnappedPositionGraphic snappedPositionGraphic;
   private final OrientationGraphic orientationGraphic;

   public PatrolWaypointGraphic()
   {
      snappedPositionGraphic = new SnappedPositionGraphic(Color.YELLOW);
      orientationGraphic = new OrientationGraphic(snappedPositionGraphic);

      getChildren().add(snappedPositionGraphic.getSphere());
      getChildren().add(orientationGraphic.getArrow());
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

   @Override
   public Point3D getPosition()
   {
      return snappedPositionGraphic.getPosition();
   }
}
