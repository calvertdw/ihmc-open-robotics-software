# Message used to report the current joint angles for the fingers of a hand.

int8 ROBOT_SIDE_LEFT=0

int8 ROBOT_SIDE_RIGHT=1

# Unique ID used to identify this message, should preferably be consecutively increasing.
uint32 sequence_id

# Field default value 255
int8 robot_side

float64[] joint_angles

bool connected

bool calibrated


