package us.ihmc.footstepPlanning.ui.components;

import javafx.scene.Group;
import javafx.scene.Node;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.ui.components.FootstepPathMeshViewer;
import us.ihmc.javaFXToolkit.messager.Messager;

import java.util.concurrent.atomic.AtomicReference;

import static us.ihmc.footstepPlanning.ui.FootstepPlannerUserInterfaceAPI.FootstepPlanTopic;

public class FootstepPathRenderer
{

   private final Group root = new Group();

   private final AtomicReference<FootstepPlan> footstepPlanReference;



   private final FootstepPathMeshViewer footstepPathMeshViewer;

   public FootstepPathRenderer(Messager messager)
   {
      footstepPlanReference = messager.createInput(FootstepPlanTopic);

      messager.registerTopicListener(FootstepPlanTopic, request -> processFootstepPath());

      footstepPathMeshViewer = new FootstepPathMeshViewer();
      root.getChildren().add(footstepPathMeshViewer.getRoot());
   }

   public void clear()
   {
      footstepPlanReference.set(null);
   }

   public void start()
   {
      footstepPathMeshViewer.start();
   }

   public void stop()
   {
      footstepPathMeshViewer.stop();
   }


   private void processFootstepPath()
   {
      footstepPathMeshViewer.processFootstepPath(footstepPlanReference.getAndSet(null));
   }


   public Node getRoot()
   {
      return root;
   }
}
