package us.ihmc.humanoidBehaviors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.humanoidBehaviors.tools.BehaviorHelper;
import us.ihmc.humanoidBehaviors.tools.BehaviorMessagerUpdateThread;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.SharedMemoryMessager;
import us.ihmc.messager.kryo.KryoMessager;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.tools.lists.PairList;

import static us.ihmc.humanoidBehaviors.BehaviorModule.API.BehaviorSelection;

public class BehaviorModule
{
   private enum CommunicationMode { INTERPROCESS, INTRAPROCESS }

   private final BehaviorRegistry behaviorRegistry;
   private final MessagerAPI messagerAPI;
   private final Messager messager;
   private final PairList<BehaviorStatics, BehaviorInterface> constructedBehaviors = new PairList<>();

   public static BehaviorModule createInterprocess(BehaviorRegistry behaviorRegistry, DRCRobotModel robotModel)
   {
      return new BehaviorModule(behaviorRegistry, robotModel, CommunicationMode.INTERPROCESS);
   }

   public static BehaviorModule createIntraprocess(BehaviorRegistry behaviorRegistry, DRCRobotModel robotModel)
   {
      return new BehaviorModule(behaviorRegistry, robotModel, CommunicationMode.INTRAPROCESS);
   }

   private BehaviorModule(BehaviorRegistry behaviorRegistry, DRCRobotModel robotModel, CommunicationMode communicationMode)
   {
      this.behaviorRegistry = behaviorRegistry;
      LogTools.info("Starting behavior module in {} mode", communicationMode.name());

      messagerAPI = behaviorRegistry.constructMessagerAPI();

      PubSubImplementation pubSubImplementation;
      if (communicationMode == CommunicationMode.INTERPROCESS)
      {
         pubSubImplementation = PubSubImplementation.FAST_RTPS;
         messager = KryoMessager.createServer(messagerAPI,
                                              NetworkPorts.BEHAVIOUR_MODULE_PORT.getPort(),
                                              new BehaviorMessagerUpdateThread(BehaviorModule.class.getSimpleName(), 5));
         ExceptionTools.handle(() -> messager.startMessager(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
      }
      else // intraprocess
      {
         pubSubImplementation = PubSubImplementation.INTRAPROCESS;
         messager = new SharedMemoryMessager(messagerAPI);
         ExceptionTools.handle(() -> messager.startMessager(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
      }

      Ros2Node ros2Node = ROS2Tools.createRos2Node(pubSubImplementation, "behavior_backpack");

      for (BehaviorStatics behaviorStatics : behaviorRegistry.getEntries())
      {
         constructedBehaviors.add(behaviorStatics, behaviorStatics.getBehaviorSupplier().build(new BehaviorHelper(robotModel, messager, ros2Node)));
      }

      messager.registerTopicListener(BehaviorSelection, selection -> // simple string based selection
      {
         for (ImmutablePair<BehaviorStatics, BehaviorInterface> behavior : constructedBehaviors)
         {
            behavior.getRight().setEnabled(behavior.getLeft().getName().equals(selection));
         }
      });
   }

   public Messager getMessager()
   {
      return messager;
   }

   public MessagerAPI getMessagerAPI()
   {
      return messagerAPI;
   }

   // API created here from build
   public static class API
   {
      private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
      private static final MessagerAPIFactory.Category RootCategory = apiFactory.createRootCategory("Root");
      private static final MessagerAPIFactory.CategoryTheme BehaviorModuleTheme = apiFactory.createCategoryTheme("BehaviorModule");

      public static final MessagerAPIFactory.Topic<String> BehaviorSelection = topic("BehaviorSelection");

      private static final <T> MessagerAPIFactory.Topic<T> topic(String name)
      {
         return RootCategory.child(BehaviorModuleTheme).topic(apiFactory.createTypedTopicTheme(name));
      }

      public static synchronized final MessagerAPI create(MessagerAPI... behaviorAPIs) // TODO check threading
      {
         apiFactory.includeMessagerAPIs(behaviorAPIs);

         return apiFactory.getAPIAndCloseFactory();
      }
   }
}
