package us.ihmc.gdx;

import boofcv.struct.calib.CameraPinholeBrown;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.gdx.sceneManager.GDXSceneLevel;
import us.ihmc.gdx.simulation.sensors.GDXHighLevelDepthSensorSimulator;
import us.ihmc.gdx.tools.BoxesDemoModel;
import us.ihmc.gdx.tools.GDXModelPrimitives;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.gdx.ui.graphics.live.GDXROS1VideoVisualizer;
import us.ihmc.gdx.ui.visualizers.ImGuiGDXGlobalVisualizersPanel;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.RosTools;

public class GDXROS1DepthSensorDemo
{
   private final GDXImGuiBasedUI baseUI = new GDXImGuiBasedUI(getClass(),
                                                              "ihmc-open-robotics-software",
                                                              "ihmc-high-level-behaviors/src/test/resources",
                                                              getClass().getSimpleName());
   private ImGuiGDXGlobalVisualizersPanel globalVisualizersUI;
   private GDXHighLevelDepthSensorSimulator l515;

   public GDXROS1DepthSensorDemo()
   {
      RosMainNode ros1Node = RosTools.createRosNode(NetworkParameters.getROSURI(), "depth_sensor_demo");

      baseUI.launchGDXApplication(new Lwjgl3ApplicationAdapter()
      {
         @Override
         public void create()
         {
            baseUI.create();

            baseUI.get3DSceneManager().addModelInstance(new ModelInstance(GDXModelPrimitives.createCoordinateFrame(0.3)));
            baseUI.get3DSceneManager().addModelInstance(new BoxesDemoModel().newInstance());
            baseUI.get3DSceneManager().addModelInstance(new DepthSensorDemoObjectsModel().newInstance());

            RigidBodyTransform transform = new RigidBodyTransform();
            transform.appendTranslation(0.0, 0.0, 0.5);
            transform.appendPitchRotation(Math.PI / 6.0);
            ReferenceFrame sensorFrame = ReferenceFrameTools.constructFrameWithUnchangingTransformToParent("sensorFrame",
                                                                                                           ReferenceFrameTools.getWorldFrame(),
                                                                                                           transform);

            double publishRateHz = 5.0;
            double verticalFOV = 55.0;
            int imageWidth = 640;
            int imageHeight = 480;
            double fx = 500.0;
            double fy = 500.0;
            double minRange = 0.105;
            double maxRange = 5.0;
            CameraPinholeBrown depthCameraIntrinsics = new CameraPinholeBrown(fx, fy, 0, imageWidth / 2.0, imageHeight / 2.0, imageWidth, imageHeight);
            l515 = new GDXHighLevelDepthSensorSimulator("Stepping L515",
                                                        ros1Node,
                                                        RosTools.MAPSENSE_DEPTH_IMAGE,
                                                        RosTools.MAPSENSE_DEPTH_CAMERA_INFO,
                                                        depthCameraIntrinsics,
                                                        RosTools.L515_VIDEO,
                                                        RosTools.L515_COLOR_CAMERA_INFO,
                                                        null,
                                                        null,
                                                        null,
                                                        sensorFrame,
                                                        () -> 0L,
                                                        verticalFOV,
                                                        imageWidth,
                                                        imageHeight,
                                                        minRange,
                                                        maxRange,
                                                        publishRateHz,
                                                        false);
            globalVisualizersUI = new ImGuiGDXGlobalVisualizersPanel();
            globalVisualizersUI.addVisualizer(new GDXROS1VideoVisualizer("L515 Depth Video", RosTools.L515_DEPTH));


            baseUI.getImGuiPanelManager().addPanel(l515);
            baseUI.getImGuiPanelManager().addPanel(globalVisualizersUI);

            l515.setSensorEnabled(true);
            l515.setPublishPointCloudROS2(true);
            l515.setRenderPointCloudDirectly(true);
            l515.setPublishDepthImageROS1(true);
            l515.setDebugCoordinateFrame(true);
            l515.setRenderColorVideoDirectly(true);
            l515.setRenderDepthVideoDirectly(true);
            l515.setPublishColorImageROS1(true);
            l515.setPublishColorImageROS2(true);
            l515.create();

            baseUI.get3DSceneManager().addRenderableProvider(l515, GDXSceneLevel.VIRTUAL);
            globalVisualizersUI.create();

            ros1Node.execute();
         }

         @Override
         public void render()
         {
            globalVisualizersUI.update();
            l515.render(baseUI.get3DSceneManager());
            baseUI.renderBeforeOnScreenUI();
            baseUI.renderEnd();
         }

         @Override
         public void dispose()
         {
            l515.dispose();
            globalVisualizersUI.destroy();
            baseUI.dispose();
         }
      });
   }

   public static void main(String[] args)
   {
      new GDXROS1DepthSensorDemo();
   }
}
