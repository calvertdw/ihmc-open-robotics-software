package us.ihmc.commonWalkingControlModules.controllerCore.parameters;

import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.JointAccelerationIntegrationCalculator;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.registry.YoRegistry;

/**
 * This is a parameterized implementation of the
 * {@link JointAccelerationIntegrationParametersReadOnly} interface.
 * <p>
 * Each parameter will be represented by a {@link DoubleParameter}. Default values can be provided
 * at construction time. After that the only way to set these parameters is through a tuning
 * application. If a parameter file is loaded that defines the parameters created by this class the
 * default values will be overwritten.
 * </p>
 */
public class TunableJointAccelerationIntegrationParameters implements JointAccelerationIntegrationParametersReadOnly
{
   private static final String POSITION_BREAK_FREQUENCY_NAME = "PositionBreakFrequency";
   private static final String VELOCITY_BREAK_FREQUENCY_NAME = "VelocityBreakFrequency";
   private static final String POSITION_INTEGRATION_MAX_ERROR_NAME = "PositionIntegrationMaxError";
   private static final String VELOCITY_INTEGRATION_MAX_ERROR_NAME = "VelocityIntegrationMaxError";
   private static final String VELOCITY_REFERENCE_ALPHA_NAME = "VelocityReferenceAlpha";

   private static final double SUGGESTED_MAXIMUM_POSITION_ERROR = 2.0 * Math.PI;
   private static final double SUGGESTED_MAXIMUM_VELOCITY = SUGGESTED_MAXIMUM_POSITION_ERROR / 0.1;
   private static final double SUGGESTED_MAXIMUM_FREQUENCY = 5.0;

   private final DoubleParameter positionBreakFrequency;
   private final DoubleParameter velocityBreakFrequency;
   private final DoubleParameter maxPositionError;
   private final DoubleParameter maxVelocityError;
   private final DoubleParameter velocityReferenceAlpha;

   /**
    * Creates a new sets of parameters for acceleration integration.
    * <p>
    * If the parameters can not be loaded from file they will be initialized with the default values
    * from the {@link JointAccelerationIntegrationCalculator}.
    * </p>
    *
    * @param namePrefix the {@code String} to be prepended to each {@code YoVariable} of this class.
    * @param registry   the registry to which the {@code YoVariable}s of this class are registered to.
    */
   public TunableJointAccelerationIntegrationParameters(String namePrefix, YoRegistry registry)
   {
      this(namePrefix, JointAccelerationIntegrationCalculator.DEFAULT_POSITION_BREAK_FREQUENCY,
           JointAccelerationIntegrationCalculator.DEFAULT_VELOCITY_BREAK_FREQUENCY, JointAccelerationIntegrationCalculator.DEFAULT_MAX_POSITION_ERROR,
           JointAccelerationIntegrationCalculator.DEFAULT_MAX_VELOCITY_ERROR, JointAccelerationIntegrationCalculator.DEFAULT_VELOCITY_REFERENCE_ALPHA, registry);
   }

   /**
    * Creates a new sets of parameters for acceleration integration.
    * <p>
    * If the parameters can not be loaded from file they will be initialized with the default values
    * from the provided {@link JointAccelerationIntegrationParametersReadOnly}.
    * </p>
    *
    * @param namePrefix the {@code String} to be prepended to each {@code YoVariable} of this class.
    * @param registry   the registry to which the {@code YoVariable}s of this class are registered to.
    * @param defaults   the default values to be used if the parameters can not be loaded from file.
    */
   public TunableJointAccelerationIntegrationParameters(String namePrefix, YoRegistry registry, JointAccelerationIntegrationParametersReadOnly defaults)
   {
      this(namePrefix, defaults.getPositionBreakFrequency(), defaults.getVelocityBreakFrequency(), defaults.getMaxPositionError(),
           defaults.getMaxVelocityError(), defaults.getVelocityReferenceAlpha(), registry);
   }

   /**
    * Creates a new sets of parameters for acceleration integration.
    * <p>
    * If the parameters can not be loaded from file they will be initialized with the provided default
    * values.
    * </p>
    *
    * @param namePrefix             the {@code String} to be prepended to each {@code YoVariable} of
    *                               this class.
    * @param positionBreakFrequency the break frequency used to compute the desired position, see
    *                               {@link #getPositionBreakFrequency()}.
    * @param velocityBreakFrequency the break frequency used to compute the desired velocity, see
    *                               {@link #getVelocityBreakFrequency()}.
    * @param maxPositionError       the default maximum position error used to saturate the desired
    *                               position, see {@link #getMaxPositionError()}.
    * @param maxVelocityError       the maximum default desired velocity, see
    *                               {@link #getMaxVelocityError()}.
    * @param registry               the registry to which the {@code YoVariable}s of this class are
    *                               registered to.
    */
   public TunableJointAccelerationIntegrationParameters(String namePrefix,
                                                        double positionBreakFrequency,
                                                        double velocityBreakFrequency,
                                                        double maxPositionError,
                                                        double maxVelocityError,
                                                        double velocityReferenceAlpha,
                                                        YoRegistry registry)
   {
      this.positionBreakFrequency = new DoubleParameter(namePrefix
            + POSITION_BREAK_FREQUENCY_NAME, registry, positionBreakFrequency, 0.0, SUGGESTED_MAXIMUM_FREQUENCY);
      this.velocityBreakFrequency = new DoubleParameter(namePrefix
            + VELOCITY_BREAK_FREQUENCY_NAME, registry, velocityBreakFrequency, 0.0, SUGGESTED_MAXIMUM_FREQUENCY);
      this.maxPositionError = new DoubleParameter(namePrefix
            + POSITION_INTEGRATION_MAX_ERROR_NAME, registry, maxPositionError, 0.0, SUGGESTED_MAXIMUM_POSITION_ERROR);
      this.maxVelocityError = new DoubleParameter(namePrefix
            + VELOCITY_INTEGRATION_MAX_ERROR_NAME, registry, maxVelocityError, 0.0, SUGGESTED_MAXIMUM_VELOCITY);
      this.velocityReferenceAlpha = new DoubleParameter(namePrefix + VELOCITY_REFERENCE_ALPHA_NAME, registry, velocityReferenceAlpha, 0.0, 1.0);
   }

   /** {@inheritDoc} */
   @Override
   public double getPositionBreakFrequency()
   {
      return positionBreakFrequency.getValue();
   }

   /** {@inheritDoc} */
   @Override
   public double getVelocityBreakFrequency()
   {
      return velocityBreakFrequency.getValue();
   }

   /** {@inheritDoc} */
   @Override
   public double getMaxPositionError()
   {
      return maxPositionError.getValue();
   }

   /** {@inheritDoc} */
   @Override
   public double getMaxVelocityError()
   {
      return maxVelocityError.getValue();
   }

   /** {@inheritDoc} */
   @Override
   public double getVelocityReferenceAlpha()
   {
      return velocityReferenceAlpha.getValue();
   }
}
