package us.ihmc.atlas.behaviors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.humanoidBehaviors.BehaviorTeleop;
import us.ihmc.humanoidBehaviors.ui.BehaviorUI;

public class AtlasBehaviorUI extends Application
{
   private static final boolean launchBehaviorModule = false;

   private BehaviorUI ui;

   @Override
   public void start(Stage primaryStage) throws Exception
   {
      DRCRobotModel drcRobotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.REAL_ROBOT, false);

      BehaviorTeleop teleop = BehaviorTeleop.createForUI(drcRobotModel, "localhost");

      ui = new BehaviorUI(primaryStage,
                          teleop,
                          drcRobotModel);
      ui.show();

      if (launchBehaviorModule)
      {
         // launch behavior module
      }
   }

   @Override
   public void stop() throws Exception
   {
      super.stop();

      ui.stop();

      Platform.exit();
   }

   public static void main(String[] args)
   {
      launch(args);
   }
}
