# This message is part of the IHMC footstep planning module.

# Unique ID used to identify this message, should preferably be consecutively increasing.
uint32 sequence_id

byte FOOTSTEP_PLANNING_RESULT_OPTIMAL_SOLUTION = 0
byte FOOTSTEP_PLANNING_RESULT_SUB_OPTIMAL_SOLUTION = 1
byte FOOTSTEP_PLANNING_RESULT_TIMED_OUT_BEFORE_SOLUTION = 2
byte FOOTSTEP_PLANNING_RESULT_NO_PATH_EXISTS = 3
byte FOOTSTEP_PLANNING_RESULT_SNAPPING_FAILED = 4
byte FOOTSTEP_PLANNING_RESULT_PLANNER_FAILED = 5

int32 NO_PLAN_ID = -1

byte footstep_planning_result 255
int32 plan_id -1

perception_msgs/PlanarRegionsListMessage planar_regions_list

geometry_msgs/Pose[] body_path
geometry_msgs/Pose2D path_planner_start_pose
geometry_msgs/Pose2D path_planner_goal_pose