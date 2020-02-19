package us.ihmc.humanoidBehaviors.ui.behaviors;

import javafx.fxml.FXML;
import javafx.scene.SubScene;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.humanoidBehaviors.StepInPlaceBehavior;
import us.ihmc.humanoidBehaviors.ui.BehaviorUIDefinition;
import us.ihmc.humanoidBehaviors.ui.BehaviorUIInterface;
import us.ihmc.messager.Messager;

public class StepInPlaceBehaviorUI extends BehaviorUIInterface
{
   public static final BehaviorUIDefinition DEFINITION = new BehaviorUIDefinition(StepInPlaceBehavior.DEFINITION, StepInPlaceBehaviorUI::new);

   private Messager behaviorMessager;

   @Override
   public void init(SubScene sceneNode, Messager behaviorMessager, DRCRobotModel robotModel)
   {
      this.behaviorMessager = behaviorMessager;
   }

   @Override
   public void setEnabled(boolean enabled)
   {

   }

   @FXML public void startStepping()
   {
      behaviorMessager.submitMessage(StepInPlaceBehavior.API.Stepping, true);
   }

   @FXML public void pauseStepping()
   {
      behaviorMessager.submitMessage(StepInPlaceBehavior.API.Stepping, false);
   }
}
