# This is a message that holds data to describe the state.
#
# The state is defined by:
#  * the state at the beginning of the interval, and
#  * the state's rate of change during the interval,
# in which the state contains the robot's posture and velocity.
#
# The initial state and its rate of change allow us to compute the
# state in a desired time.
#
# The order of the state is designed to be consistent with the URDF.
# Quaternions can be accommodated due to the different dimensions of
# the state and its rate of change.

# This represents the state at the beginning of the interval.
# This is broken into (position, rate).
# The first three of position are base position.
# The next four are the quaternion rotation of the base.
# The rate is (linear, angular)
float64[] x

# This represents the state's rate of change during the interval
float64[] dx