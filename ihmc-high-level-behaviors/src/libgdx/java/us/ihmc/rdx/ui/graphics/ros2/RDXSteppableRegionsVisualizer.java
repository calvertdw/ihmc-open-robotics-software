package us.ihmc.rdx.ui.graphics.ros2;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import perception_msgs.msg.dds.SteppableRegionsListCollectionMessage;
import us.ihmc.communication.IHMCROS2Callback;
import us.ihmc.perception.steppableRegions.SteppableRegionsAPI;
import us.ihmc.rdx.sceneManager.RDXSceneLevel;
import us.ihmc.rdx.imgui.ImGuiFrequencyPlot;
import us.ihmc.rdx.ui.graphics.RDXVisualizer;
import us.ihmc.rdx.ui.graphics.RDXSteppableRegionGraphic;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.tools.thread.MissingThreadTools;
import us.ihmc.tools.thread.ResettableExceptionHandlingExecutorService;

import java.util.Set;

public class RDXSteppableRegionsVisualizer extends RDXVisualizer
{
   private final RDXSteppableRegionGraphic steppableRegionGraphic = new RDXSteppableRegionGraphic();

   private final ResettableExceptionHandlingExecutorService executorService;
   private final ImGuiFrequencyPlot frequencyPlot = new ImGuiFrequencyPlot();
   private final ImInt yawToShow = new ImInt(0);
   private final ImBoolean renderHeightMap = new ImBoolean(true);
   private final ImBoolean renderPlanes = new ImBoolean(false);
   private final ImBoolean inPaintHeight = new ImBoolean(false);
   private final ImBoolean renderGroundPlane = new ImBoolean(false);
   private final ImBoolean renderGroundCells = new ImBoolean(false);
   private int receivedRegions = -1;

   public RDXSteppableRegionsVisualizer(String title)
   {
      super(title);

      boolean daemon = true;
      int queueSize = 1;
      executorService = MissingThreadTools.newSingleThreadExecutor(getClass().getSimpleName(), daemon, queueSize);
   }

   @Override
   public void create()
   {
      super.create();

      setActive(true);
   }

   public void setUpForNetworking(ROS2Node ros2Node)
   {
      new IHMCROS2Callback<>(ros2Node, SteppableRegionsAPI.STEPPABLE_REGIONS_OUTPUT, this::acceptSteppableRegionsCollection);
   }

   public void acceptSteppableRegionsCollection(SteppableRegionsListCollectionMessage steppableRegionsListCollection)
   {
      frequencyPlot.recordEvent();
      if (isActive())
      {
         if (steppableRegionsListCollection == null)
            return;

         receivedRegions = steppableRegionsListCollection.getRegionsPerYaw().get(yawToShow.get());
         executorService.clearQueueAndExecute(() ->
                                              {
                                                 steppableRegionGraphic.setRenderHeightMap(renderHeightMap.get());
                                                 steppableRegionGraphic.setRenderPlanes(renderPlanes.get());
                                                 steppableRegionGraphic.setInPaintHeight(inPaintHeight.get());
                                                 steppableRegionGraphic.setRenderGroundPlane(renderGroundPlane.get());
                                                 steppableRegionGraphic.setRenderGroundCells(renderGroundCells.get());
                                                 steppableRegionGraphic.generateMeshesAsync(steppableRegionsListCollection, yawToShow.get());
                                              });
      }
   }

   @Override
   public void setActive(boolean active)
   {
      super.setActive(active);
      if (!isActive())
      {
         executorService.interruptAndReset();
      }
   }

   @Override
   public void renderImGuiWidgets()
   {
      super.renderImGuiWidgets();

      ImGui.checkbox("Render Height Map", renderHeightMap);
      ImGui.checkbox("Render Planes", renderPlanes);
      ImGui.checkbox("In Paint Height", inPaintHeight);
      ImGui.checkbox("Render Ground Plane", renderGroundPlane);
      ImGui.checkbox("Render Ground Cells", renderGroundCells);

      if (!isActive())
      {
         executorService.interruptAndReset();
      }
      imgui.internal.ImGui.text("Regions rendered: " + receivedRegions);
      imgui.internal.ImGui.sliderInt("Yaw to show", yawToShow.getData(), 0, 4);

      frequencyPlot.renderImGuiWidgets();
   }

   @Override
   public void update()
   {
      super.update();

      if (isActive())
      {
         steppableRegionGraphic.update();
      }
   }

   @Override
   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool, Set<RDXSceneLevel> sceneLevels)
   {
      if (isActive() && sceneLevelCheck(sceneLevels))
      {
         steppableRegionGraphic.getRenderables(renderables, pool);
      }
   }

   public void destroy()
   {
      steppableRegionGraphic.destroy();
   }
}
