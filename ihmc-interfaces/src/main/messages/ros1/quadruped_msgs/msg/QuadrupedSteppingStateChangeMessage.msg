# This message is part of the IHMC quadruped controller API.
# This message notifies the user of a change in the stepping controller state.
# This message's primary use is to signal a requested state change is completed.

int8 STAND=0

int8 STEP=1

int8 SOLE_WAYPOINT=2

# Specifies the controller's state prior to transition.
# Field default value 255
int8 initial_quadruped_stepping_state_enum

# Specifies the state the controller has transitioned into.
# Field default value 255
int8 end_quadruped_stepping_state_enum


