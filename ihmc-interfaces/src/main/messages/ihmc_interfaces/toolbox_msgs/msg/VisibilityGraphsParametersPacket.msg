
# This message is part of the IHMC footstep planning module.

float64 DEFAULT_NO_VALUE = -11.1

# Unique ID used to identify this message, should preferably be consecutively increasing.
uint32 sequence_id

float64 max_inter_region_connection_length -11.1

float64 normal_z_threshold_for_accessible_regions -11.1

float64 navigable_extrusion_distance -11.1

float64 obstacle_extrusion_distance -11.1

float64 obstacle_extrusion_distance_if_not_too_high_to_step -11.1

float64 too_high_to_step_distance -11.1

float64 height_for_max_avoidance -11.1

float64 cluster_resolution -11.1

float64 exploration_distance_from_start_goal -11.1

float64 planar_region_min_area 0.0

uint32 planar_region_min_size 0

# Defines the angle from which two regions are considered orthogonal.
# It is used to determine if a region should be projected onto another as a polygon or a line.
# It should be close to 90 degrees.
# Returns the angle threshold to use to determine if a line or polygon projection method should be used.
float64 region_orthogonal_angle -11.1

# This epsilon is is used when searching to which region the start/goal belongs to.
# A positive value corresponds to growing all the regions before testing if the start/goal is inside.
# Returns the value of the epsilon to use.
float64 search_host_region_epsilon 0.03

float64 can_duck_under_height -11.1

float64 can_easily_step_over_height -11.1

float64 length_for_long_inter_region_edge -11.1

bool perform_post_processing_node_shifting false

bool introduce_midpoints_in_post_processing true

bool compute_orientations_to_avoid_obstacles false

float64 heuristic_weight -11.1

float64 distance_weight -11.1

float64 elevation_weight -11.1

float64 occluded_goal_edge_weight -11.1

float64 weight_for_inter_region_edge -11.1

bool return_best_effort_solution false

bool optimize_for_narrow_passage false