package us.ihmc.gdx.imgui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import imgui.internal.ImGui;
import us.ihmc.log.LogTools;
import us.ihmc.tools.io.JSONFileTools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class ImGuiPanelManager
{
   private final ArrayList<ImGuiPanel> panels = new ArrayList<>();

   private final Class<?> classForLoading;
   private final String directoryNameToAssumePresent;
   private final String subsequentPathToResourceFolder;

   public ImGuiPanelManager(Class<?> classForLoading, String directoryNameToAssumePresent, String subsequentPathToResourceFolder)
   {
      this.classForLoading = classForLoading;
      this.directoryNameToAssumePresent = directoryNameToAssumePresent;
      this.subsequentPathToResourceFolder = subsequentPathToResourceFolder;
   }

   public void addPanel(String windowName, Runnable render)
   {
      panels.add(new ImGuiPanel(windowName, render));
   }

   public void addPrimaryPanel(String windowName)
   {
      panels.add(new ImGuiPanel(windowName));
   }

   public void renderPanelMenu()
   {
      for (ImGuiPanel panel : panels)
      {
         if (panel.isTogglable())
         {
            ImGui.menuItem(panel.getPanelName(), "", panel.getEnabled());
         }
      }
   }

   public void renderPanels()
   {
      for (ImGuiPanel panel : panels)
      {
         if (panel.isTogglable() && panel.getEnabled().get())
         {
            panel.render();
         }
      }
   }

   public void loadConfiguration(Path settingsPath)
   {
      Path windowsSettingsPath = settingsPath.getParent().resolve(settingsPath.getFileName().toString().replace("Settings.ini", "Panels.json"));
      JSONFileTools.loadWithClasspathDefault(windowsSettingsPath,
                                             classForLoading,
                                             directoryNameToAssumePresent,
                                             subsequentPathToResourceFolder,
                                             "/imgui",
                                             jsonNode ->
      {
         JsonNode windowsNode = jsonNode.get("windows");
         for (Iterator<Map.Entry<String, JsonNode>> it = windowsNode.fields(); it.hasNext(); )
         {
            Map.Entry<String, JsonNode> panel = it.next();
            for (ImGuiPanel imGuiPanel : panels)
            {
               if (imGuiPanel.getPanelName().equals(panel.getKey()))
               {
                  imGuiPanel.getEnabled().set(panel.getValue().asBoolean());
               }
            }
         }
      });
   }

   public void saveConfiguration(Path settingsPath, boolean saveDefault)
   {
      Consumer<ObjectNode> rootConsumer = root ->
      {
         ObjectNode anchorJSON = root.putObject("windows");

         for (ImGuiPanel window : this.panels)
         {
            if (window.isTogglable())
            {
               anchorJSON.put(window.getPanelName(), window.getEnabled().get());
            }
         }
      };
      String saveFileNameString = settingsPath.getFileName().toString().replace("Settings.ini", "Panels.json");
      if (saveDefault)
      {
         JSONFileTools.saveToClasspath(directoryNameToAssumePresent, subsequentPathToResourceFolder, "imgui/" + saveFileNameString, rootConsumer);
      }
      else
      {
         Path windowsSettingsPath = settingsPath.getParent().resolve(saveFileNameString);
         LogTools.info("Saving ImGui windows settings to {}", windowsSettingsPath.toString());
         JSONFileTools.save(windowsSettingsPath, rootConsumer);
      }
   }

   public ArrayList<ImGuiPanel> getPanels()
   {
      return panels;
   }
}
