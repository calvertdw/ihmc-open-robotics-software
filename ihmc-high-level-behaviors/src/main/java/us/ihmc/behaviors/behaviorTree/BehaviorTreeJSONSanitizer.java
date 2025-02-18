package us.ihmc.behaviors.behaviorTree;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.mutable.MutableObject;
import us.ihmc.behaviors.behaviorTree.topology.BehaviorTreeTopologyOperations;
import us.ihmc.communication.crdt.CRDTInfo;
import us.ihmc.communication.ros2.ROS2ActorDesignation;
import us.ihmc.log.LogTools;
import us.ihmc.tools.io.JSONFileTools;
import us.ihmc.tools.io.JSONTools;
import us.ihmc.tools.io.WorkspaceResourceDirectory;
import us.ihmc.tools.io.WorkspaceResourceFile;

import javax.annotation.Nullable;

/**
 * Tool to load all JSON files and resave them all in order to perform
 * schema changes.
 */
public class BehaviorTreeJSONSanitizer
{
   private final WorkspaceResourceDirectory treeFilesDirectory;
   private final CRDTInfo crdtInfo = new CRDTInfo(ROS2ActorDesignation.OPERATOR, 1);

   public BehaviorTreeJSONSanitizer(Class<?> classForFindingSourceSetDirectory)
   {
      treeFilesDirectory = new WorkspaceResourceDirectory(classForFindingSourceSetDirectory, "/behaviorTrees");

      for (WorkspaceResourceFile fileToLoad : treeFilesDirectory.queryContainedFiles())
      {
         MutableObject<BehaviorTreeNodeDefinition> loadedRootNode = new MutableObject<>();

         LogTools.info("Loading {}", fileToLoad.getFilesystemFile());
         JSONFileTools.load(fileToLoad, jsonNode ->
         {
            loadedRootNode.setValue(loadFromFile(jsonNode, null, fileToLoad.getFileName()));
         });

         loadedRootNode.getValue().saveToFile();
      }
   }

   private BehaviorTreeNodeDefinition loadFromFile(JsonNode jsonNode, BehaviorTreeNodeDefinition parentNode, @Nullable String jsonFileName)
   {
      String typeName = jsonNode.get("type").textValue();

      Class<?> definitionType = BehaviorTreeDefinitionRegistry.getClassFromTypeName(typeName);

      BehaviorTreeNodeDefinition node = BehaviorTreeDefinitionBuilder.createNode(definitionType, crdtInfo, treeFilesDirectory);

      if (jsonFileName != null)
         node.setJSONFileName(jsonFileName);
      node.loadFromFile(jsonNode);

      if (parentNode != null)
         BehaviorTreeTopologyOperations.addChildBasic(node, parentNode);

      JSONTools.forEachArrayElement(jsonNode, "children", childJsonNode ->
      {
         JsonNode fileNode = childJsonNode.get("file");
         if (fileNode == null)
         {
            loadFromFile(childJsonNode, node, null);
         }
         else
         {
            WorkspaceResourceFile childFile = new WorkspaceResourceFile(treeFilesDirectory, fileNode.asText());
            LogTools.info("Loading {}", childFile.getFilesystemFile());
            JSONFileTools.load(childFile, childJSONNode -> loadFromFile(childJSONNode, node, childFile.getFileName()));
         }
      });

      return node;
   }
}
