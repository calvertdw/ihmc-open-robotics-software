package us.ihmc.humanoidBehaviors.lookAndStep;

import std_msgs.msg.dds.Empty;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.humanoidBehaviors.tools.footstepPlanner.FootstepForUI;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.ROS2Topic;

import java.util.ArrayList;
import java.util.List;

public class LookAndStepBehaviorAPI
{
   public static final ROS2Topic<Pose3D> GOAL_INPUT = ROS2Tools.BEHAVIOR_MODULE.withInput().withType(Pose3D.class);
   public static final ROS2Topic<Empty> RESET = ROS2Tools.BEHAVIOR_MODULE.withInput().withType(Empty.class);
   public static final ROS2Topic<Empty> REACHED_GOAL = ROS2Tools.BEHAVIOR_MODULE.withOutput().withType(Empty.class);

   private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
   private static final MessagerAPIFactory.Category RootCategory = apiFactory.createRootCategory("LookAndStepBehavior");
   private static final MessagerAPIFactory.CategoryTheme LookAndStepTheme = apiFactory.createCategoryTheme("LookAndStep");

   public static final MessagerAPIFactory.Topic<Boolean> OperatorReviewEnabled = topic("OperatorReviewEnabled");
   public static final MessagerAPIFactory.Topic<Boolean> ReviewApproval = topic("ReviewApproval");

   // Parameter tuning topics
   public static final MessagerAPIFactory.Topic<List<String>> LookAndStepParameters = topic("LookAndStepParameters");
   public static final MessagerAPIFactory.Topic<List<String>> FootstepPlannerParameters = topic("FootstepPlannerParameters");

   // Visualization only topics
   public static final MessagerAPIFactory.Topic<String> CurrentState = topic("CurrentState");
   public static final MessagerAPIFactory.Topic<ArrayList<FootstepForUI>> StartAndGoalFootPosesForUI = topic("StartAndGoalFootPosesForUI");
   public static final MessagerAPIFactory.Topic<ArrayList<FootstepForUI>> FootstepPlanForUI = topic("FootstepPlanForUI");
   public static final MessagerAPIFactory.Topic<Pose3D> ClosestPointForUI = topic("ClosestPointForUI");
   public static final MessagerAPIFactory.Topic<Pose3D> SubGoalForUI = topic("SubGoalForUI");
   public static final MessagerAPIFactory.Topic<PlanarRegionsList> MapRegionsForUI = topic("MapRegionsForUI");
   public static final MessagerAPIFactory.Topic<List<Pose3D>> BodyPathPlanForUI = topic("BodyPathPlanForUI");

   private static <T> MessagerAPIFactory.Topic<T> topic(String name)
   {
      return RootCategory.child(LookAndStepTheme).topic(apiFactory.createTypedTopicTheme(name));
   }

   public static MessagerAPIFactory.MessagerAPI create()
   {
      return apiFactory.getAPIAndCloseFactory();
   }
}
