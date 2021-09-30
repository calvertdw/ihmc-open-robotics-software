package us.ihmc.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import imgui.internal.ImGui;
import imgui.type.ImBoolean;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.gdx.sceneManager.GDXSceneLevel;
import us.ihmc.gdx.simulation.GDXLowLevelDepthSensorSimulator;
import us.ihmc.gdx.tools.GDXModelPrimitives;
import us.ihmc.gdx.tools.GDXTools;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.gdx.ui.gizmo.GDXPose3DGizmo;
import us.ihmc.gdx.vr.GDXVRManager;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

import static us.ihmc.gdx.vr.GDXVRControllerButtons.SteamVR_Trigger;

public class GDXVRDepthSensorDemo
{
   private final GDXImGuiBasedUI baseUI = new GDXImGuiBasedUI(getClass(),
                                                              "ihmc-open-robotics-software",
                                                              "ihmc-high-level-behaviors/src/test/resources",
                                                              "VR Depth Sensor Demo");
   private ModelInstance cylinder;
   private boolean moveWithController = true;
   private final Matrix4 tempTransform = new Matrix4();
   private final ImBoolean useSensorColor = new ImBoolean(false);
   private final ImBoolean useGizmoToPoseSensor = new ImBoolean(false);
   private final float[] color = new float[4];
   private final GDXPose3DGizmo gizmo = new GDXPose3DGizmo();

   public GDXVRDepthSensorDemo()
   {
      GDXLowLevelDepthSensorSimulator depthSensorSimulator = new GDXLowLevelDepthSensorSimulator("Sensor", 90.0, 640, 480, 0.05, 10.0);
      GDXPointCloudRenderer pointCloudRenderer = new GDXPointCloudRenderer();
      SideDependentList<ModelInstance> controllerCoordinateFrames = new SideDependentList<>();

      baseUI.launchGDXApplication(new Lwjgl3ApplicationAdapter()
      {
         @Override
         public void create()
         {
            baseUI.create();

            baseUI.get3DSceneManager().addCoordinateFrame(1.0);
            DepthSensorDemoObjectsModel depthSensorDemoObjectsModel = new DepthSensorDemoObjectsModel();
            cylinder = depthSensorDemoObjectsModel.buildCylinder();
            baseUI.get3DSceneManager().addModelInstance(cylinder);
            baseUI.get3DSceneManager().addModelInstance(depthSensorDemoObjectsModel.newInstance());

            depthSensorSimulator.create();
            depthSensorSimulator.getCamera().position.set(0.0f, -1.0f, 1.0f);
            depthSensorSimulator.getCamera().direction.set(0.0f, 0.0f, -1.0f);

            pointCloudRenderer.create((int) depthSensorSimulator.getCamera().viewportHeight * (int) depthSensorSimulator.getCamera().viewportWidth);
            baseUI.get3DSceneManager().addRenderableProvider(pointCloudRenderer, GDXSceneLevel.VIRTUAL);

            for (RobotSide side : RobotSide.values)
            {
               ModelInstance coordinateFrameInstance = GDXModelPrimitives.createCoordinateFrameInstance(0.1);
               controllerCoordinateFrames.put(side, coordinateFrameInstance);
               baseUI.get3DSceneManager().addModelInstance(coordinateFrameInstance, GDXSceneLevel.VIRTUAL);
            }

            baseUI.getVRManager().addVRInputProcessor(this::handleVREvents);

            baseUI.getImGuiPanelManager().addPanel("Point Cloud Settings", this::renderPointCloudSettings);

            gizmo.create(baseUI.get3DSceneManager().getCamera3D());
            baseUI.addImGui3DViewInputProcessor(gizmo::process3DViewInput);
            baseUI.get3DSceneManager().addRenderableProvider(this::getRenderables);
         }

         private void handleVREvents(GDXVRManager vrManager)
         {
            vrManager.getContext().getController(RobotSide.LEFT, controller ->
            {
               if (controller.isButtonNewlyPressed(SteamVR_Trigger))
               {
                  moveWithController = !moveWithController;
               }
               if (moveWithController)
               {
                  controller.getPose(ReferenceFrame.getWorldFrame(), tempTransform);
                  depthSensorSimulator.setCameraWorldTransform(tempTransform);
                  controllerCoordinateFrames.get(RobotSide.LEFT).transform.set(tempTransform); // TODO: Should be an option on the VR manager probably
               }
            });
            vrManager.getContext().getController(RobotSide.RIGHT, controller ->
            {
               controller.getPose(ReferenceFrame.getWorldFrame(), cylinder.nodes.get(0).globalTransform);
            });
         }

         private void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
         {
            if (useGizmoToPoseSensor.get())
            {
               gizmo.getRenderables(renderables, pool);
            }
         }

         @Override
         public void render()
         {
            if (useGizmoToPoseSensor.get())
            {
               GDXTools.toGDX(gizmo.getTransform(), tempTransform);
               depthSensorSimulator.setCameraWorldTransform(tempTransform);
            }

            depthSensorSimulator.render(baseUI.get3DSceneManager());

            if (useSensorColor.get())
            {
               pointCloudRenderer.updateMesh(depthSensorSimulator.getPoints(), depthSensorSimulator.getColors());
            }
            else
            {
               pointCloudRenderer.setPointsToRender(depthSensorSimulator.getPoints(), new Color(color[0], color[1], color[2], color[3]));
               pointCloudRenderer.updateMesh();
            }

            baseUI.renderBeforeOnScreenUI();
            baseUI.renderEnd();
         }

         private void renderPointCloudSettings()
         {
            ImGui.checkbox("Use Gizmo to pose sensor", useGizmoToPoseSensor);
            ImGui.checkbox("Use Sensor Color", useSensorColor);
            ImGui.colorPicker4("Color", color);
         }

         @Override
         public void dispose()
         {
            depthSensorSimulator.dispose();
            pointCloudRenderer.dispose();
            baseUI.dispose();
         }
      });
   }

   public static void main(String[] args)
   {
      new GDXVRDepthSensorDemo();
   }
}
