package us.ihmc.robotics.physics;

public class ConstraintParameters implements ConstraintParametersBasics
{
   private double coefficientOfRestitution;
   private double errorReductionParameter;
   private double constraintForceMixing;

   public ConstraintParameters()
   {
   }

   public ConstraintParameters(double coefficientOfRestitution, double errorReductionParameter, double constraintForceMixing)
   {
      this.coefficientOfRestitution = coefficientOfRestitution;
      this.errorReductionParameter = errorReductionParameter;
      this.constraintForceMixing = constraintForceMixing;
   }

   @Override
   public void setCoefficientOfRestitution(double coefficientOfRestitution)
   {
      this.coefficientOfRestitution = coefficientOfRestitution;
   }

   @Override
   public void setErrorReductionParameter(double errorReductionParameter)
   {
      this.errorReductionParameter = errorReductionParameter;
   }

   @Override
   public void setConstraintForceMixing(double constraintForceMixing)
   {
      this.constraintForceMixing = constraintForceMixing;
   }

   @Override
   public double getCoefficientOfRestitution()
   {
      return coefficientOfRestitution;
   }

   @Override
   public double getErrorReductionParameter()
   {
      return errorReductionParameter;
   }

   @Override
   public double getConstraintForceMixing()
   {
      return constraintForceMixing;
   }
}
