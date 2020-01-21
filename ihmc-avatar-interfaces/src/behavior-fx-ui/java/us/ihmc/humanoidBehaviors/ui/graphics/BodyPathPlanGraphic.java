package us.ihmc.humanoidBehaviors.ui.graphics;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.humanoidBehaviors.ui.tools.PrivateAnimationTimer;
import us.ihmc.javaFXToolkit.shapes.JavaFXMultiColorMeshBuilder;
import us.ihmc.javaFXToolkit.shapes.TextureColorAdaptivePalette;
import us.ihmc.log.LogTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PathTools;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BodyPathPlanGraphic extends Group
{
   private static final double LINE_THICKNESS = 0.025;
   private final double startColorHue = Color.GREEN.getHue();
   private final double goalColorHue = Color.RED.getHue();

   private final MeshView meshView = new MeshView();
   private final PrivateAnimationTimer animationTimer = new PrivateAnimationTimer(this::handle);
   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));
   private final TextureColorAdaptivePalette palette = new TextureColorAdaptivePalette(1024, false);
   private final JavaFXMultiColorMeshBuilder meshBuilder = new JavaFXMultiColorMeshBuilder(palette);
   private Mesh mesh;
   private Material material;

   public BodyPathPlanGraphic()
   {
      getChildren().addAll(meshView);

      animationTimer.start();
   }

   /**
    * To process in parallel.
    */
   public void generateMeshesAsynchronously(List<Point3DReadOnly> bodyPath)
   {
      executorService.submit(() -> {
         LogTools.debug("Received body path plan containing {} points", bodyPath.size());
         generateMeshes(bodyPath);
      });
   }

   public void generateMeshes(List<Point3DReadOnly> bodyPath)
   {
      meshBuilder.clear();

      double totalPathLength = PathTools.computePathLength(bodyPath);
      double currentLength = 0.0;

      palette.clearPalette();
      JavaFXMultiColorMeshBuilder meshBuilder = new JavaFXMultiColorMeshBuilder(palette);

      for (int segmentIndex = 0; segmentIndex < bodyPath.size() - 1; segmentIndex++)
      {
         Point3DReadOnly lineStart = bodyPath.get(segmentIndex);
         Point3DReadOnly lineEnd = bodyPath.get(segmentIndex + 1);

         double lineStartHue = EuclidCoreTools.interpolate(startColorHue, goalColorHue, currentLength / totalPathLength);
         currentLength += lineStart.distance(lineEnd);
         double lineEndHue = EuclidCoreTools.interpolate(startColorHue, goalColorHue, currentLength / totalPathLength);
         meshBuilder.addLine(lineStart, lineEnd, LINE_THICKNESS, Color.hsb(lineStartHue, 1.0, 0.5), Color.hsb(lineEndHue, 1.0, 1.0));
      }


      Mesh mesh = meshBuilder.generateMesh();
      Material material = meshBuilder.generateMaterial();

      synchronized (this)
      {
         this.mesh = mesh;
         this.material = material;
      }
   }

   private void handle(long now)
   {
      synchronized (this)
      {
         meshView.setMesh(mesh);
         meshView.setMaterial(material);
      }
   }
}
