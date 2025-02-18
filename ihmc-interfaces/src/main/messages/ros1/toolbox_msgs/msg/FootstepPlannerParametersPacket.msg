# This message is part of the IHMC footstep planning module.
# Maximum step reach when stepping up.
# 
# Long steps forward are rejected by the planner if one of two criteria are met:
# The total length of the footstep exceeds {@link #getMaximumStepReachWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# - OR -
# The y-position of the value of the footstep exceeds {@link #getMaximumStepWidthWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# - AND -
# The z-position of the value of the footstep is greater than {@link #getMaximumStepZWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# Maximum step width when stepping up.
# 
# Long steps forward are rejected by the planner if one of two criteria are met:
# The total length of the footstep exceeds {@link #getMaximumStepReachWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# - OR -
# The y-position of the value of the footstep exceeds {@link #getMaximumStepWidthWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# - AND -
# The z-position of the value of the footstep is greater than {@link #getMaximumStepZWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# Step height for considering stepping up.
# 
# Long steps forward are rejected by the planner if one of two criteria are met:
# The total length of the footstep exceeds {@link #getMaximumStepReachWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# - OR -
# The y-position of the value of the footstep exceeds {@link #getMaximumStepWidthWhenSteppingUp()}, when expressed in its parent's z-up sole frame.
# - AND -
# The z-position of the value of the footstep is greater than {@link #getMaximumStepZWhenSteppingUp()}, when expressed in its parent's z-up sole frame.

float64 DEFAULT_NO_VALUE=-11.1

int8 ROBOT_SIDE_LEFT=0

int8 ROBOT_SIDE_RIGHT=1

# Unique ID used to identify this message, should preferably be consecutively increasing.
uint32 sequence_id

# Sets whether or not the search should check if the body is colliding with the world. This may cause the planner
# to run slower.
bool check_for_body_box_collisions

# Enables a collision check that is lighter-weight than a bounding box. Draws a planar region by vertically extruding the line
# between consecutive steps and invalidates steps with collisions, see: ObstacleBetweenNodesChecker
bool check_for_path_collisions

# Returns the ideal step width for walking on flat ground.
# Field default value -11.1
float64 ideal_footstep_width

# Returns the ideal step length for walking on flat ground.
# Field default value -11.1
float64 ideal_footstep_length

# Returns the ideal step side step width when "shuffling" sideways.
# Field default value -11.1
float64 ideal_side_step_width

# Returns the ideal length when walking backwards. This value is positive.
# Field default value -11.1
float64 ideal_back_step_length

# Returns ideal step length when the vertical height between the start-of-swing and stance feet are at maximum allowed height.
# Field default value -11.1
float64 ideal_step_length_at_max_step_z

# The planner will try to shift footsteps inside of a region so that this value is the minimum distance from the step
# to the edge. A negative value means the footstep can overhang a region.
# Field default value -11.1
float64 wiggle_inside_delta_target

# This parameter only is used if wiggle_while_planning is true. If a step cannot be wiggled inside by this amount or more,
# it will be rejected. Note that if {wiggle_while_planning if false, it's always best effort on the final plan.
# Field default value -11.1
float64 wiggle_inside_delta_minimum

# Maximum xy-distance the planner will consider for candidate steps.
# Step reach refers to the magnitude of the xy-position of a footstep expressed in its parent's z-up sole frame,
# where the parent is the last footstep taken on the other foot.
# 
# This parameter is intended to prevent accepting candidate footsteps that are near both the maximum step length and step width.
# Field default value -11.1
float64 maximum_step_reach

# Maximum yaw between consecutive footsteps
# 
# A candidate footstep will be rejected if the yaw between it and its parent is greater than this value.
# 
# This restricts the planner from planning kinematically infeasible footsteps. It is constant through the
# space of potential steps, so the robot should be able to achieve this yaw, for example, when stepping at
# its maximum reach.
# Field default value -11.1
float64 maximum_step_yaw

# If true, uses IK-based precomputed reachability map instead of the heuristic step metrics
bool use_reachability_map

# Solution quality threshold when using IK-based feasibility check, only used when use_reachability_map is true.
# Field default value -11.1
float64 solution_quality_threshold

# Minimum step width the planner will consider for candidate steps.
# 
# Step width refers to the magnitude of the y-position of a footstep expressed in its parent's sole frame,
# where the parent is the last footstep taken on the other foot.
# 
# If this value is too low, for example below the foot's width, the planner could place consecutive footsteps
# on top of each other. If too high, footsteps might not be kinematically feasible.
# Field default value -11.1
float64 minimum_step_width

# Minimum step length the planner will consider for candidate steps.
# 
# Step length refers to the x-position of a footstep expressed in its parent's sole frame,
# where the parent is the last footstep taken on the other foot.
# 
# If this value is too low, for example below the foot's length, the planner could place consecutive footsteps
# on top of each other. If too high, footsteps might not be kinematically feasible.
# Field default value -11.1
float64 minimum_step_length

# Minimum step yaw.
# Field default value -11.1
float64 minimum_step_yaw

# Large steps forward and up can cause the robot to surpass its torque limits.
# These parameters should be tuned so that when the robot takes a step of length {@link #getMaximumStepReachWhenSteppingUp()} and {@link #getMaximumStepZWhenSteppingUp()},
# it's very close to saturating its torque limits.
# Field default value -11.1
float64 maximum_step_reach_when_stepping_up

# Large steps forward and up can cause the robot to surpass its torque limits.
# These parameters should be tuned so that when the robot takes a step of length {@link #getMaximumStepReachWhenSteppingUp()} and {@link #getMaximumStepZWhenSteppingUp()},
# it's very close to saturating its torque limits.
# Field default value -11.1
float64 maximum_step_width_when_stepping_up

# Large steps forward and up can cause the robot to surpass its torque limits.
# These parameters should be tuned so that when the robot takes a step of length {@link #getMaximumStepReachWhenSteppingUp()} and {@link #getMaximumStepZWhenSteppingUp()},
# it's very close to saturating its torque limits.
# Field default value -11.1
float64 maximum_step_z_when_stepping_up

# Maximum step length when stepping forward and down.
# 
# Large steps forward and down are rejected by the planner if one of two criteria are met:
# The x-position of the value of the footstep exceeds maximumStepXWhenForwardAndDown, when expressed in its parent's z-up sole frame
# - OR -
# The y-position of the value of the footstep exceeds maximumStepYWhenForwardAndDown, when expressed in its parent's z-up sole frame
# - AND -
# The z-position of the value of the footstep is less than maximumStepZWhenForwardAndDown, when expressed in its parent's z-up sole frame
# 
# Large steps forward and down usually cause the robot to hit it's ankle pitch joint limit.
# These parameters should be tuned so that when the robot takes a step of length maximumStepXWhenForwardAndDown and height maximumStepZWhenForwardAndDown,
# it's very close to hitting it's ankle pitch joint limit.
# Field default value -11.1
float64 maximum_step_x_when_forward_and_down

# Maximum step width when stepping forward and down.
# 
# Large steps forward and down are rejected by the planner if one of two criteria are met:
# The x-position of the value of the footstep exceeds maximumStepXWhenForwardAndDown, when expressed in its parent's z-up sole frame
# - OR -
# The y-position of the value of the footstep exceeds maximumStepYWhenForwardAndDown, when expressed in its parent's z-up sole frame
# - AND -
# The z-position of the value of the footstep is less than maximumStepZWhenForwardAndDown, when expressed in its parent's z-up sole frame
# 
# Large steps forward and down usually cause the robot to hit it's ankle pitch joint limit.
# These parameters should be tuned so that when the robot takes a step of length maximumStepXWhenForwardAndDown and height maximumStepZWhenForwardAndDown,
# it's very close to hitting it's ankle pitch joint limit.
# Field default value -11.1
float64 maximum_step_y_when_forward_and_down

# Maximum step height when stepping forward and down.
# 
# Large steps forward and down are rejected by the planner if one of two criteria are met:
# The x-position of the value of the footstep exceeds maximumStepXWhenForwardAndDown, when expressed in its parent's z-up sole frame
# - OR -
# The y-position of the value of the footstep exceeds maximumStepYWhenForwardAndDown, when expressed in its parent's z-up sole frame
# - AND -
# The z-position of the value of the footstep is less than maximumStepZWhenForwardAndDown, when expressed in its parent's z-up sole frame
# 
# Large steps forward and down usually cause the robot to hit it's ankle pitch joint limit.
# These parameters should be tuned so that when the robot takes a step of length maximumStepXWhenForwardAndDown and height maximumStepZWhenForwardAndDown,
# it's very close to hitting it's ankle pitch joint limit.
# Field default value -11.1
float64 maximum_step_z_when_forward_and_down

# Maximum vertical distance between consecutive footsteps
# 
# A candidate footstep will be rejected if its z-value is greater than this value, when expressed its parent's
# z-up sole frame.
# Field default value -11.1
float64 maximum_step_z

# Maximum vertical distance between start-of-swing and touchdown
# Field default value -11.1
float64 maximum_swing_z

# Maximum xy distance between start-of-swing and touchdown
# Field default value -11.1
float64 maximum_swing_reach

# Maximum vertical distance between consecutive footsteps when the trailing foot is pitched at {@link #getMinimumSurfaceInclineRadians()} .
# 
# The maximum depth is determined by linearly interpolating between {@link #getMaximumStepZ()} and this value, based on the fraction the foot is pitched by.
# A candidate footstep will be rejected if its z-value is less than this value, when expressed its parent's z-up sole frame.
# Field default value -11.1
float64 minimum_step_z_when_fully_pitched

# Maximum forward distance between consecutive footsteps when the trailing foot is pitched at {@link #getMinimumSurfaceInclineRadians()} .
# 
# The maximum distance is determined by linearly interpolating between {@link #getMaximumStepZ()} and this value, based on the fraction the foot is pitched by.
# A candidate footstep will be rejected if its z-value is less than this value, when expressed its parent's z-up sole frame.
# Field default value -11.1
float64 maximum_step_x_when_fully_pitched

# This is the reduction factor for the max yaw when the step is at max reach.
# This means that, when the footstep is at its maximum distance, this is the fraction reduction of the max yaw.
# If this returns 0.0, the max yaw is not modified, even at full reach.
# If this returns 1.0, the max yaw is 0 at full reach.
# 
# That is,
# modifiedMaxYaw = (1.0 - reach / maxReach) * maxYaw + reach / maxReach * (1.0 - alpha) * maxYaw
# Field default value -11.1
float64 step_yaw_reduction_factor_at_max_reach

# Minimum percentage that a candidate footstep needs to overlap with its associated planar region in order to be accepted.
# 
# If this parameter is set to 1.0 only full footsteps are allowed. A value less then 1.0 will allow partial footholds.
# Field default value 0.9
float64 minimum_foothold_percent

# The planner will ignore candidate footsteps if they are on a planar region with an incline that is higher
# then the value specified here.
# 
# More specifically, if a footstep has an associated planar region and that regions surface normal has a
# z-value less than cos(minimumSurfaceInclineRadians), it will be rejected.
# Field default value -11.1
float64 minimum_surface_incline_radians

# The wiggler can either run as a post-processor on a resulting plan or on each candidate step while planning.
# If true, this wiggles each candidate step, which will slow down plan times but resulting plans will be guarunteed to match step constraints.
bool wiggle_while_planning

# If wiggle_while_planning is true, this will reject a step if the wiggle meet the specified parameters. If it's false the wiggle does a best effort
bool reject_if_wiggle_not_satisfied

# There are two solvers for wiggling the step, one constrains to the region's convex hull and the other to the region's concave hull,
# this toggles between them.
bool enable_concave_hull_wiggler

# When wiggling a candidate footstep into a planar region, this is the maximum distance xy-distance
# distance the planner will use
# Field default value -11.1
float64 maximum_xy_wiggle_distance

# When wiggling a candidate footstep into a planar region, this is the maximum yaw
# distance the planner will use
# Field default value -11.1
float64 maximum_yaw_wiggle

# When snapping a candidate footstep to a planar region, its possible that another planar region
# intersects the footstep at a steep angle, i.e. a valley. If this intersecting planar region
# is never more than maximumZPenetrationOnValleyRegions above the footstep, it won't be rejected,
# otherwise it will.
# Field default value -11.1
float64 maximum_z_penetration_on_valley_regions

# Maximum step width the planner will consider for candidate steps.
# 
# Step width refers to the magnitude of the y-position of a footstep expressed in its parent's sole frame,
# where the parent is the last footstep taken on the other foot.
# 
# If this value is too low, the planner will unnecessarily reject footsteps. If too high, footsteps might not be kinematically feasible.
# Field default value -11.1
float64 maximum_step_width

# The planner can be setup to avoid footsteps near the bottom of "cliffs". When the footstep has a planar region
# nearby that is cliffHeightToShiftAwayFrom higher than the candidate footstep, it will move away from it
# until it is minimumDistanceFromCliffBottoms away from it.
# 
# If these values are set to zero, cliff avoidance will be turned off. This creates a risk that the robot will
# hit the cliff with its swing foot. Therefore, these parameters should be set according to what the swing trajectory
# generator is capable of swinging over.
# Field default value -11.1
float64 cliff_base_height_to_avoid

# The planner can be setup to avoid footsteps near the bottom of "cliffs". When the footstep has a planar region
# nearby that is {@link #getCliffBaseHeightToAvoid} higher than the candidate footstep, it will move away from it
# until it is minimumDistanceFromCliffBottoms away from it.
# 
# If these values are set to zero, cliff avoidance will be turned off. This creates a risk that the robot will
# hit the cliff with its swing foot. Therefore, these parameters should be set according to what the swing trajectory
# generator is capable of swinging over.
# Field default value -11.1
float64 minimum_distance_from_cliff_bottoms

# The planner can be setup to avoid footsteps near the top of "cliffs". When the footstep has a planar region
# nearby that is cliffHeightToShiftAwayFrom higher than the candidate footstep, it will move away from it
# until it is minimumDistanceFromCliffTops away from it.
# 
# If these values are set to zero, cliff avoidance will be turned off. This creates a risk that the robot will
# hit the cliff with its swing foot. Therefore, these parameters should be set according to what the swing trajectory
# generator is capable of swinging over.
# Field default value -11.1
float64 cliff_top_height_to_avoid

# The planner can be setup to avoid footsteps near the top of "cliffs". When the footstep has a planar region
# nearby that is {@link #getCliffTopHeightToAvoid} higher than the candidate footstep, it will move away from it
# until it is minimumDistanceFromCliffBottoms away from it.
# 
# If these values are set to zero, cliff avoidance will be turned off. This creates a risk that the robot will
# hit the cliff with its swing foot. Therefore, these parameters should be set according to what the swing trajectory
# generator is capable of swinging over.
# Field default value -11.1
float64 minimum_distance_from_cliff_tops

# Some node checkers will check if a bounding box that describes the body of the robot will move
# through a planar region (e.g. a wall) when going from one footstep to the next one. To avoid these
# collisions, this defines the box height.
# Field default value -11.1
float64 body_box_height

# Some node checkers will check if a bounding box that describes the body of the robot will move
# through a planar region (e.g. a wall) when going from one footstep to the next one. To avoid these
# collisions, this defines the box depth.
# Field default value -11.1
float64 body_box_depth

# Some node checkers will check if a bounding box that describes the body of the robot will move
# through a planar region (e.g. a wall) when going from one footstep to the next one. To avoid these
# collisions, this defines the box width.
# Field default value -11.1
float64 body_box_width

# Some node checkers will check if a bounding box that describes the body of the robot will move
# through a planar region (e.g. a wall) when going from one footstep to the next one. To avoid these
# collisions, this defines the x-offset of a bounding box relative to the average frame between sequential footsteps.
# Field default value -11.1
float64 body_box_base_x

# Some node checkers will check if a bounding box that describes the body of the robot will move
# through a planar region (e.g. a wall) when going from one footstep to the next one. To avoid these
# collisions, this defines the y-offset of a bounding box relative to the average frame between sequential footsteps.
# Field default value -11.1
float64 body_box_base_y

# Some node checkers will check if a bounding box that describes the body of the robot will move
# through a planar region (e.g. a wall) when going from one footstep to the next one. To avoid these
# collisions, this defines the z-offset of a bounding box relative to the average frame between sequential footsteps.
# Field default value -11.1
float64 body_box_base_z

# Maximum height above a stance step that a candidate step is snapped to. Regions above this height are ignored.
# Intended to avoid ceilings or obstacles that are above the top of the robot
# Field default value -11.1
float64 maximum_snap_height

# Parameter used inside the node expansion to avoid footsteps that would be on top of the stance foot.
# Nodes are only added to the expanded list if they are outside the box around the stance foot defined by
# this parameter.
# Field default value -11.1
float64 min_clearance_from_stance

# Radius around the goal inside which the planner should start to turn to match the goal's orientation
# Field default value -11.1
float64 final_turn_proximity

# When using a cost based planning approach this value defined how the yaw of a footstep will be
# weighted in comparison to its position.
# Field default value -11.1
float64 yaw_weight

# When using a cost based planning approach this value defines how the pitch will be weighted.
# Field default value -11.1
float64 pitch_weight

# When using a cost based planning approach this value defines how the roll will be weighted.
# Field default value -11.1
float64 roll_weight

# When using a cost based planning approach, this value defines how the forward (or backward) displacement
# of a footstep will be weighted in comparison to its position. Note that when using a Euclidean distance, this
# weight is averaged with the value returned by {@link #getLateralWeight()}
# Field default value -11.1
float64 forward_weight

# When using a cost based planning approach, this value defines how the lateral displacement
# of a footstep will be weighted in comparison to its position. Note that when using a Euclidean distance, this
# weight is averaged with the value returned by {@link #getForwardWeight()}
# Field default value -11.1
float64 lateral_weight

# When using a cost based planning approach this value defines how the height change when stepping up will be
# weighted.
# Field default value -11.1
float64 step_up_weight

# When using a cost based planning approach this value defines how the height change when stepping down will be
# weighted.
# Field default value -11.1
float64 step_down_weight

# When using a cost based planning approach this value defines how the a missing foothold area will be weighted.
# Field default value -11.1
float64 long_step_weight

# When using a cost based planning approach this value defines how the a missing foothold area will be weighted.
# Field default value -11.1
float64 foothold_area_weight

# When using a reference based ideal step calculator, alpha determines how much we value the reference plan vs. new plan. Value should be between 0 and 1
# Field default value -11.1
float64 reference_plan_alpha

# When using a cost based planning approach this value defines the cost that is added for each step
# taken. Setting this value to a high number will favor plans with less steps.
# Field default value -11.1
float64 cost_per_step

# Gets the weight for the heuristics in the A Star planner.
# Field default value -11.1
float64 a_star_heuristics_weight

# Number of body collision checks done by interpolation between steps. If zero, only collision checks centered at the step are done.
# Field default value 1
uint32 intermediate_body_box_checks

# If the robot's mid-foot pose is within this distance of the body path, it will match the body path heading.
# Otherwise, it will turn towards the body path
# Field default value -11.1
float64 distance_from_path_tolerance

# If the robot's mid-foot pose oriented within this threshold of the body path's heading, it will match the body path heading.
# Otherwise, it will turn in plance towards the body path
# Field default value -11.1
float64 delta_yaw_from_reference_tolerance

# Maximum steps considered at each iteration. If more than this number of steps are available, the closest steps to the
# ideal step are considered and the others are ignored. Set to non-positive number to disable
# Field default value -1
int32 maximum_branch_factor

# If true, enables a mask that reduces the number of calculated steps away from the ideal step
# Field default value True
bool enable_expansion_mask

# If true will try to wiggle steps away from shin collisions. Collisions are checked against all regions.
# Enable concave hull wiggler must be true in order for the shin collision checker to run.
bool enable_shin_collision_check

# How far the shin collision cylinder extends from the toe
# Field default value -11.1
float64 shin_toe_clearance

# How far the shin collision cylinder extends from the heel
# Field default value -11.1
float64 shin_heel_clearance

# Length of the shin collidable cylinder
# Field default value -11.1
float64 shin_length

# Height offset of shin collidable cylinder
# Field default value -11.1
float64 shin_height_offet

# When using a height map, maximum accepted rms error
# Field default value -11.1
float64 rms_error_threshold

# When using a height map, cost of rms value
# Field default value -11.1
float64 rms_error_cost

# When using a height map, minimum rms error to penalize
# Field default value -11.1
float64 rms_min_error_to_penalize

# When using a height map, snapping is done by taking all the points inside the polygon, then removing points below
# z_max - dz, where z_max is the highest point and dz is this value.
# Field default value -11.1
float64 height_map_snap_threshold


