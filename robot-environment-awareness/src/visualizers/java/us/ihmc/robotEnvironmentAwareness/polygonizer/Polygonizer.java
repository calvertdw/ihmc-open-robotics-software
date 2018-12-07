package us.ihmc.robotEnvironmentAwareness.polygonizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.geometry.LineSegment2D;
import us.ihmc.euclid.geometry.interfaces.LineSegment2DReadOnly;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.MessagerAPIFactory.Category;
import us.ihmc.messager.MessagerAPIFactory.CategoryTheme;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.MessagerAPIFactory.TopicTheme;
import us.ihmc.messager.MessagerAPIFactory.TypedTopicTheme;
import us.ihmc.robotEnvironmentAwareness.geometry.ConcaveHullFactoryParameters;
import us.ihmc.robotEnvironmentAwareness.geometry.SimpleConcaveHullFactory;
import us.ihmc.robotEnvironmentAwareness.geometry.SimpleConcaveHullFactory.ConcaveHullFactoryResult;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationRawData;

public class Polygonizer
{
   private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
   private static final Category Root = apiFactory.createRootCategory(apiFactory.createCategoryTheme(Polygonizer.class.getSimpleName()));

   private static final CategoryTheme ConcaveHull = apiFactory.createCategoryTheme("ConcaveHull");
   private static final CategoryTheme Factory = apiFactory.createCategoryTheme("Factory");

   private static final TopicTheme Parameters = apiFactory.createTopicTheme("Parameters");
   private static final TypedTopicTheme<List<Input>> InputTheme = apiFactory.createTypedTopicTheme("input");
   private static final TypedTopicTheme<List<Output>> OutputTheme = apiFactory.createTypedTopicTheme("output");

   public static final Topic<ConcaveHullFactoryParameters> PolygonizerParameters = Root.child(ConcaveHull).child(Factory).topic(Parameters);
   public static final Topic<List<Input>> PolygonizerInput = Root.topic(InputTheme);
   public static final Topic<List<Output>> PolygonizerOutput = Root.topic(OutputTheme);

   public static final MessagerAPI API = apiFactory.getAPIAndCloseFactory();

   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));
   private final AtomicReference<ConcaveHullFactoryParameters> parameters;
   private final Messager messager;

   public Polygonizer(Messager messager)
   {
      this.messager = messager;
      parameters = messager.createInput(PolygonizerParameters, new ConcaveHullFactoryParameters());
      messager.registerTopicListener(PolygonizerInput, this::processAndPublishLater);
   }

   private void processAndPublishLater(Collection<Input> inputs)
   {
      executorService.execute(() -> messager.submitMessage(PolygonizerOutput, process(inputs)));
   }

   private List<Output> process(Collection<Input> inputs)
   {
      return inputs.stream().map(this::process).collect(Collectors.toList());
   }

   public void shutdown()
   {
      executorService.shutdownNow();
   }

   private Output process(Input input)
   {
      ConcaveHullFactoryResult result = SimpleConcaveHullFactory.createConcaveHull(input.points, input.lineConstraints, parameters.get());
      return new Output(input, result);
   }

   public static Input toInput(PlanarRegionSegmentationRawData data)
   {
      return new Input(data.getRegionId(), data.getTransformFromLocalToWorld(), data.getPointCloudInPlane(), data.getIntersectionsInPlane());
   }

   public static List<Input> toInputList(Collection<PlanarRegionSegmentationRawData> data)
   {
      return data.stream().map(Polygonizer::toInput).collect(Collectors.toList());
   }

   public static class Input
   {
      private final int id;
      private final RigidBodyTransform transformToWorld = new RigidBodyTransform();
      private final List<Point2DReadOnly> points = new ArrayList<>();
      private final List<LineSegment2DReadOnly> lineConstraints = new ArrayList<>();

      public Input(int id, RigidBodyTransform transformToWorld, List<? extends Point2DReadOnly> points, List<? extends LineSegment2DReadOnly> lineConstraints)
      {
         this.id = id;
         this.transformToWorld.set(transformToWorld);
         points.stream().map(Point2D::new).forEach(this.points::add);
         lineConstraints.stream().map(LineSegment2D::new).forEach(this.lineConstraints::add);
      }

      public int getId()
      {
         return id;
      }

      public List<Point2DReadOnly> getPoints()
      {
         return points;
      }

      public List<LineSegment2DReadOnly> getLineConstraints()
      {
         return lineConstraints;
      }

      public RigidBodyTransform getTransformToWorld()
      {
         return transformToWorld;
      }
   }

   public static class Output
   {
      private final Input input;
      private final ConcaveHullFactoryResult concaveHullFactoryResult;

      public Output(Input input, ConcaveHullFactoryResult concaveHullFactoryResult)
      {
         this.input = input;
         this.concaveHullFactoryResult = concaveHullFactoryResult;
      }

      public Input getInput()
      {
         return input;
      }

      public ConcaveHullFactoryResult getConcaveHullFactoryResult()
      {
         return concaveHullFactoryResult;
      }
   }
}
