package us.ihmc.humanoidBehaviors.ui.mapping.visualizer;

import java.util.ArrayList;
import java.util.List;

import controller_msgs.msg.dds.StereoVisionPointCloudMessage;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Point3D32;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.graphicsDescription.MeshDataGenerator;
import us.ihmc.graphicsDescription.MeshDataHolder;
import us.ihmc.graphicsDescription.SegmentedLine3DMeshDataGenerator;
import us.ihmc.javaFXToolkit.shapes.JavaFXMultiColorMeshBuilder;
import us.ihmc.javaFXToolkit.shapes.TextureColorAdaptivePalette;
import us.ihmc.robotEnvironmentAwareness.communication.converters.PointCloudCompression;

public class PointCloudGraphic extends Group
{
   private static final float ORIGIN_POINT_SIZE = 0.05f;
   private static final float LENGTH_SENSOR_FRAME_AXIS = 0.1f;
   private static final float WIDTH_SENSOR_FRAME_AXIS = 0.01f;
   private static final float SCAN_POINT_SIZE = 0.005f;
   private static final int NUMBER_OF_POINTS_PER_MESSAGE = 5000;
   private static final int NUMBER_OF_POINTS_WAY_POINTS_TRAJECTORY = 10;
   private static final int TRAJECTORY_RADIAL_RESOLUTION = 16;
   private static final double TRAJECTORY_MESH_RADIUS = 0.01;

   private static final int palleteSizeForMeshBuilder = 2048;

   private volatile List<Node> messageNodes;
   private List<Node> lastNodes = null;
   private volatile List<Node> updatePointCloudMeshViews;

   private final JavaFXMultiColorMeshBuilder meshBuilder;

   // linear interpolated trajectory. once StereoVisionPointCloudMessage holds linear/angular velocity, this will be generated with cubic spline.
   private final boolean visualizeTrajectory;
   private RigidBodyTransform latestSensorPose = null;
   private final ArrayList<Point3D> sensorPoseTrajectory = new ArrayList<Point3D>();

   public PointCloudGraphic(boolean visualizeTrajectory)
   {
      meshBuilder = new JavaFXMultiColorMeshBuilder(new TextureColorAdaptivePalette(palleteSizeForMeshBuilder));
      this.visualizeTrajectory = visualizeTrajectory;
   }

   public void initializeMeshes()
   {
      updatePointCloudMeshViews = new ArrayList<>();
      meshBuilder.clear();
   }

   public void generateMeshes()
   {
      int numberOfPoints = sensorPoseTrajectory.size();
      if (numberOfPoints > 1)
      {
         Point3D[] sensorPoseTrajectoryPoints = new Point3D[numberOfPoints];
         for (int i = 0; i < numberOfPoints; i++)
            sensorPoseTrajectoryPoints[i] = new Point3D(sensorPoseTrajectory.get(i));
         SegmentedLine3DMeshDataGenerator segmentedLine3DMeshGenerator = new SegmentedLine3DMeshDataGenerator(numberOfPoints,
                                                                                                              TRAJECTORY_RADIAL_RESOLUTION,
                                                                                                              TRAJECTORY_MESH_RADIUS);
         segmentedLine3DMeshGenerator.compute(sensorPoseTrajectoryPoints);
         for (MeshDataHolder mesh : segmentedLine3DMeshGenerator.getMeshDataHolders())
         {
            meshBuilder.addMesh(mesh, Color.ALICEBLUE);
         }
      }

      MeshView scanMeshView = new MeshView(meshBuilder.generateMesh());
      scanMeshView.setMaterial(meshBuilder.generateMaterial());

      updatePointCloudMeshViews.add(scanMeshView);
      meshBuilder.clear();
      messageNodes = updatePointCloudMeshViews;
   }

   public void addStereoVisionPointCloudMessageMeshes(List<StereoVisionPointCloudMessage> stereoVisionPointCloudMessages, Color pointCloudColor)
   {
      for (int i = 0; i < stereoVisionPointCloudMessages.size(); i++)
      {
         addStereoVisionPointCloudMessageMesh(stereoVisionPointCloudMessages.get(i), pointCloudColor);
      }
   }

   public void addStereoVisionPointCloudMessageMesh(StereoVisionPointCloudMessage stereoVisionPointCloudMessage, Color pointCloudColor)
   {
      int redScaler = (int) (0xFF * (1 - (stereoVisionPointCloudMessage.getSensorPoseConfidence())));
      int greenScaler = (int) (0xFF * (stereoVisionPointCloudMessage.getSensorPoseConfidence()));
      Color confidenceColor = Color.rgb(redScaler, greenScaler, 0);
      addSensorPoseMesh(MessageTools.unpackSensorPose(stereoVisionPointCloudMessage), confidenceColor);

      addPointsMeshes(PointCloudCompression.decompressPointCloudToArray(stereoVisionPointCloudMessage), pointCloudColor);
   }

   public void addPointsMeshes(Point3DReadOnly[] points, Color colorToViz)
   {
      Point3D32 scanPoint = new Point3D32();

      int numberOfScanPoints = points.length;
      int sizeOfPointCloudToVisualize = Math.min(numberOfScanPoints, NUMBER_OF_POINTS_PER_MESSAGE);
      for (int j = 0; j < sizeOfPointCloudToVisualize; j++)
      {
         scanPoint.set(points[j]);
         meshBuilder.addMesh(MeshDataGenerator.Tetrahedron(SCAN_POINT_SIZE), scanPoint, colorToViz);
      }
   }

   public void addSensorPoseMesh(RigidBodyTransformReadOnly sensorPose, Color colorToViz)
   {
      Point3D32 sensorPosition = new Point3D32();
      sensorPosition.set(sensorPose.getTranslation());
      meshBuilder.addMesh(MeshDataGenerator.Tetrahedron(ORIGIN_POINT_SIZE), sensorPosition, colorToViz);

      RotationMatrix rotation = new RotationMatrix(sensorPose.getRotation());
      Point3D xAxis = new Point3D();
      Point3D yAxis = new Point3D();
      Point3D zAxis = new Point3D();
      rotation.getColumn(0, xAxis);
      rotation.getColumn(1, yAxis);
      rotation.getColumn(2, zAxis);
      xAxis.scale(LENGTH_SENSOR_FRAME_AXIS);
      yAxis.scale(LENGTH_SENSOR_FRAME_AXIS);
      zAxis.scale(LENGTH_SENSOR_FRAME_AXIS);
      meshBuilder.addMesh(MeshDataGenerator.Line(new Point3D(), xAxis, WIDTH_SENSOR_FRAME_AXIS), sensorPosition, Color.RED);
      meshBuilder.addMesh(MeshDataGenerator.Line(new Point3D(), yAxis, WIDTH_SENSOR_FRAME_AXIS), sensorPosition, Color.GREEN);
      meshBuilder.addMesh(MeshDataGenerator.Line(new Point3D(), zAxis, WIDTH_SENSOR_FRAME_AXIS), sensorPosition, Color.BLUE);

      if (visualizeTrajectory)
      {
         if (latestSensorPose == null)
         {
            sensorPoseTrajectory.add(new Point3D(sensorPose.getTranslation()));
            latestSensorPose = new RigidBodyTransform(sensorPose);
            return;
         }
         else
         {
            for (int i = 0; i < NUMBER_OF_POINTS_WAY_POINTS_TRAJECTORY; i++)
            {
               double alpha = (double) (i + 1) / NUMBER_OF_POINTS_WAY_POINTS_TRAJECTORY;
               Point3D trajectoryPoint = new Point3D();
               trajectoryPoint.interpolate(latestSensorPose.getTranslation(), sensorPose.getTranslation(), alpha);
               sensorPoseTrajectory.add(trajectoryPoint);
            }

            latestSensorPose.set(sensorPose);
         }
      }
   }

   public void addPointsMeshes(Point3DReadOnly[] points, RigidBodyTransformReadOnly sensorPose, Color pointCloudColor, Color sensorPoseColor)
   {
      addSensorPoseMesh(sensorPose, sensorPoseColor);
      addPointsMeshes(points, pointCloudColor);
   }

   public void update()
   {
      List<Node> meshViews = messageNodes;
      if (lastNodes != meshViews)
      {
         getChildren().clear();
         getChildren().addAll(meshViews);
         lastNodes = meshViews;
      }
   }
}
