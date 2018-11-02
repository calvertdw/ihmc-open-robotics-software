package us.ihmc.robotics.screwTheory;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.commons.MathTools;

/**
 * Class represents the spatial acceleration of a rigid body. A spatial acceleration is the
 * derivative of a Twist.
 * 
 * @author Twan Koolen
 *
 */
public class SpatialAccelerationVector extends SpatialMotionVector
{
   private final Vector3D tempVector = new Vector3D();
   private final RigidBodyTransform tempTransform = new RigidBodyTransform();

   public SpatialAccelerationVector()
   {
      super();
   }

   /**
    * Initiates the angular velocity and linear velocity to zero
    * 
    * @param bodyFrame what we're specifying the spatial acceleration of
    * @param baseFrame with respect to what we're specifying the spatial acceleration
    * @param expressedInFrame in which reference frame the spatial acceleration is expressed
    */
   public SpatialAccelerationVector(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame)
   {
      super(bodyFrame, baseFrame, expressedInFrame);
   }

   /**
    * @param bodyFrame what we're specifying the spatial acceleration of
    * @param baseFrame with respect to what we're specifying the spatial acceleration
    * @param expressedInFrame in which reference frame the spatial acceleration is expressed
    * @param angularAcceleration angular velocity part of the spatial acceleration
    * @param linearAcceleration linear acceleration part of the spatial acceleration
    */
   public SpatialAccelerationVector(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, Vector3DReadOnly angularAcceleration,
                                    Vector3DReadOnly linearAcceleration)
   {
      super(bodyFrame, baseFrame, expressedInFrame, angularAcceleration, linearAcceleration);
   }

   /**
    * Construct using a Matrix ([omegadot; a])
    */
   public SpatialAccelerationVector(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, DenseMatrix64F matrix)
   {
      super(bodyFrame, baseFrame, expressedInFrame, matrix);
   }

   /**
    * Construct using a double array ([omegadot; a])
    */
   public SpatialAccelerationVector(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, double[] spatialAcceleration)
   {
      super(bodyFrame, baseFrame, expressedInFrame, spatialAcceleration);
   }

   /**
    * Construct based on a screw representation of the twist
    *
    * @param bodyFrame what we're specifying the twist of
    * @param baseFrame with respect to what we're specifying the twist
    * @param expressedInFrame in which reference frame the twist is expressed
    * @param angularVelocityMagnitude magnitude of angular velocity about axisOfRotation
    * @param linearVelocityMagnitude magnitude of linear velocity in the direction of axisOfRotation
    * @param axisOfRotation axis of rotation
    * @param offset any vector from the origin of expressedInFrame to axisOfRotation
    */
   public SpatialAccelerationVector(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, double angularVelocityMagnitude,
                                    double angularAccelerationMagnitude, double linearVelocityMagnitude, double linearAccelerationMagnitude,
                                    Vector3DReadOnly axisOfRotation, Vector3DReadOnly axisOfRotationDot, Vector3DReadOnly offset, Vector3DReadOnly offsetDot)
   {
      setScrew(bodyFrame, baseFrame, expressedInFrame, angularVelocityMagnitude, angularAccelerationMagnitude, linearVelocityMagnitude,
               linearAccelerationMagnitude, axisOfRotation, axisOfRotationDot, offset, offsetDot);
   }

   /**
    * Copy constructor
    */
   public SpatialAccelerationVector(SpatialAccelerationVector other)
   {
      super(other);
   }

   /**
    * Sets this spatial acceleration vector so that it is the same as another spatial acceleration
    * vector
    */
   public void set(SpatialAccelerationVector other)
   {
      super.checkAndSet(other);
   }

   /**
    * Changes the body frame, assuming there is no relative acceleration between the old body frame
    * and the new body frame A consequence of Duindam, Port-Based Modeling and Control for Efficient
    * Bipedal Walking Robots, page 25, lemma 2.8 (a)
    * http://sites.google.com/site/vincentduindam/publications
    */
   public void setBodyFrame(ReferenceFrame newBodyFrame)
   {
      this.bodyFrame = newBodyFrame;
   }

   /**
    * Changes the base frame, assuming there is no relative acceleration between the old base frame
    * and the new base frame A consequence of Duindam, Port-Based Modeling and Control for Efficient
    * Bipedal Walking Robots, page 25, lemma 2.8 (a)
    * http://sites.google.com/site/vincentduindam/publications
    */
   public void setBaseFrame(ReferenceFrame newBaseFrame)
   {
      this.baseFrame = newBaseFrame;
   }

   /**
    * Changes the reference frame in which this spatial acceleration vector is expressed See
    * Duindam, Port-Based Modeling and Control for Efficient Bipedal Walking Robots, page 25.
    * http://sites.google.com/site/vincentduindam/publications Differentiate lemma 2.8 (c) using the
    * product rule, and use lemma 2.8 (f) in one of the terms. Result:
    *
    * new = Ad_H * (old + ad_twistOfCurrentWithRespectToNew * twistOfBodyWithRespectToBase)
    *
    * where Ad_H = [R, 0; tilde(p) * R, R] for H = [R, p; 0, 1] ad_T = [tilde(omega), 0; tilde(v),
    * tilde(omega)] for T = [omega, v]
    *
    */
   public void changeFrame(ReferenceFrame newReferenceFrame, Twist twistOfCurrentWithRespectToNew, Twist twistOfBodyWithRespectToBase)
   {
      // reference frame checks
      expressedInFrame.checkReferenceFrameMatch(twistOfCurrentWithRespectToNew.getReferenceFrame());
      expressedInFrame.checkReferenceFrameMatch(twistOfCurrentWithRespectToNew.getBodyFrame());
      newReferenceFrame.checkReferenceFrameMatch(twistOfCurrentWithRespectToNew.getBaseFrame());

      expressedInFrame.checkReferenceFrameMatch(twistOfBodyWithRespectToBase.getReferenceFrame());
      bodyFrame.checkReferenceFrameMatch(twistOfBodyWithRespectToBase.getBodyFrame());
      baseFrame.checkReferenceFrameMatch(twistOfBodyWithRespectToBase.getBaseFrame());

      // trivial case:
      if (this.expressedInFrame == newReferenceFrame)
      {
         return;
      }

      // first step: add cross terms:
      tempVector.cross(twistOfCurrentWithRespectToNew.getLinearPart(), twistOfBodyWithRespectToBase.getAngularPart()); // v_1 x omega_2
      getLinearPart().add(tempVector);

      tempVector.cross(twistOfCurrentWithRespectToNew.getAngularPart(), twistOfBodyWithRespectToBase.getLinearPart()); // omega_1 x v_2
      getLinearPart().add(tempVector);

      tempVector.cross(twistOfCurrentWithRespectToNew.getAngularPart(), twistOfBodyWithRespectToBase.getAngularPart()); // omega_1 x omega_2
      getAngularPart().add(tempVector);

      /*
       * The relative motion being dealt with the acceleration can be now transformed as if there
       * was no relative motion.
       */
      changeFrame(newReferenceFrame);
   }

   /**
    * Changes the reference frame in which this spatial motion vector is expressed, in case the new
    * frame in which this acceleration should be expressed does not move with respect to the old
    * one.
    */
   public void changeFrame(ReferenceFrame newReferenceFrame)
   {
      // trivial case:
      if (this.expressedInFrame == newReferenceFrame)
      {
         return;
      }

      // essentially premultiply the Adjoint operator, Ad_H = [R, 0; tilde(p) * R, R] (Matlab notation), but without creating a 6x6 matrix
      expressedInFrame.getTransformToDesiredFrame(tempTransform, newReferenceFrame);

      // transform the accelerations so that they are expressed in newReferenceFrame
      if (tempTransform.hasRotation())
      {
         tempTransform.transform(getAngularPart()); // only performs a rotation, since we're passing in a vector
         tempTransform.transform(getLinearPart());
      }
      if (tempTransform.hasTranslation())
      {
         tempVector.cross(tempTransform.getTranslationVector(), getAngularPart());
         getLinearPart().add(tempVector);
      }

      // change this spatial motion vector's expressedInFrame to newReferenceFrame
      this.expressedInFrame = newReferenceFrame;
   }

   /**
    * Adds another spatial acceleration to this spatial acceleration, after doing some reference
    * frame checks. See Duindam, Port-Based Modeling and Control for Efficient Bipedal Walking
    * Robots, page 25, lemma 2.8 (e) http://sites.google.com/site/vincentduindam/publications
    *
    * Duindam proves this fact for twists, but differentiating the statement results in the same
    * thing for derivatives of twists, i.e. spatial accelerations
    */
   public void add(SpatialAccelerationVector other)
   {
      // make sure they're expressed in the same reference frame
      expressedInFrame.checkReferenceFrameMatch(other.expressedInFrame);

      // make sure that the bodyFrame of this frame equals the baseFrame of the values that's being added
      bodyFrame.checkReferenceFrameMatch(other.baseFrame);

      // now it should be safe to add, and change the bodyFrame
      getAngularPart().add(other.getAngularPart());
      getLinearPart().add(other.getLinearPart());
      bodyFrame = other.bodyFrame;
   }

   public void sub(SpatialAccelerationVector other)
   {
      // make sure they're expressed in the same reference frame
      expressedInFrame.checkReferenceFrameMatch(other.expressedInFrame);

      // make sure that either the bodyFrames or baseFrames are the same
      if (baseFrame == other.baseFrame)
      {
         getAngularPart().sub(other.getAngularPart());
         getLinearPart().sub(other.getLinearPart());
         baseFrame = other.bodyFrame;
      }
      else if (bodyFrame == other.bodyFrame)
      {
         getAngularPart().sub(other.getAngularPart());
         getLinearPart().sub(other.getLinearPart());
         bodyFrame = other.baseFrame;
      }
      else
      {
         throw new RuntimeException("frames don't match");
      }
   }

   public void setIncludingFrame(SpatialAccelerationVector other)
   {
      super.setIncludingFrame(other);
   }

   /**
    * Packs the linear acceleration of a point that is fixed in bodyFrame, with respect to
    * baseFrame. The resulting vector is expressed in {@code this.getExpressedInFrame()}.
    */
   public void getAccelerationOfPointFixedInBodyFrame(Twist twist, FramePoint3D pointFixedInBodyFrame, FrameVector3D linearAccelerationToPack)
   {
      pointFixedInBodyFrame.checkReferenceFrameMatch(expressedInFrame);

      expressedInFrame.checkReferenceFrameMatch(twist.getReferenceFrame());
      bodyFrame.checkReferenceFrameMatch(twist.getBodyFrame());
      baseFrame.checkReferenceFrameMatch(twist.getBaseFrame());

      linearAccelerationToPack.setToZero(expressedInFrame);

      tempVector.set(pointFixedInBodyFrame);
      linearAccelerationToPack.cross(getAngularPart(), tempVector);
      linearAccelerationToPack.add(getLinearPart());

      tempVector.set(pointFixedInBodyFrame);
      tempVector.cross(twist.getAngularPart(), tempVector);
      tempVector.add(twist.getLinearPart());
      tempVector.cross(twist.getAngularPart(), tempVector);
      linearAccelerationToPack.add(tempVector);
   }

   public void setBasedOnOriginAcceleration(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame,
                                            FrameVector3D angularAcceleration, FrameVector3D originAcceleration, Twist twistOfBodyWithRespectToBase)
   {
      this.bodyFrame = bodyFrame;
      this.baseFrame = baseFrame;
      this.expressedInFrame = expressedInFrame;

      setBasedOnOriginAcceleration(angularAcceleration, originAcceleration, twistOfBodyWithRespectToBase);
   }

   public void setBasedOnOriginAcceleration(FrameVector3D angularAcceleration, FrameVector3D originAcceleration, Twist twistOfBodyWithRespectToBase)
   {
      bodyFrame.checkReferenceFrameMatch(expressedInFrame);
      twistOfBodyWithRespectToBase.getBodyFrame().checkReferenceFrameMatch(bodyFrame);
      twistOfBodyWithRespectToBase.getBaseFrame().checkReferenceFrameMatch(baseFrame);

      angularAcceleration.changeFrame(bodyFrame);
      getAngularPart().set(angularAcceleration);

      originAcceleration.changeFrame(bodyFrame);
      twistOfBodyWithRespectToBase.changeFrame(bodyFrame);
      getLinearPart().cross(twistOfBodyWithRespectToBase.getAngularPart(), twistOfBodyWithRespectToBase.getLinearPart());
      getLinearPart().sub(originAcceleration, getLinearPart());
   }

   public void getLinearAccelerationFromOriginAcceleration(Twist twistOfBodyWithRespectToBase, FrameVector3D linearAccelerationToPack)
   {
      bodyFrame.checkReferenceFrameMatch(expressedInFrame);
      twistOfBodyWithRespectToBase.getBodyFrame().checkReferenceFrameMatch(bodyFrame);
      twistOfBodyWithRespectToBase.getBaseFrame().checkReferenceFrameMatch(baseFrame);

      twistOfBodyWithRespectToBase.changeFrame(bodyFrame);

      linearAccelerationToPack.setToZero(bodyFrame);
      linearAccelerationToPack.cross(twistOfBodyWithRespectToBase.getAngularPart(), twistOfBodyWithRespectToBase.getLinearPart());
      linearAccelerationToPack.add(getLinearPart());
   }

   public void setScrew(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, double angularVelocityMagnitude,
                        double angularAccelerationMagnitude, double linearVelocityMagnitude, double linearAccelerationMagnitude,
                        Vector3DReadOnly axisOfRotation, Vector3DReadOnly axisOfRotationDot, Vector3DReadOnly offset, Vector3DReadOnly offsetDot)
   {
      double epsilon = 1e-12;
      if (!MathTools.epsilonEquals(1.0, axisOfRotation.lengthSquared(), epsilon))
         throw new RuntimeException("axis of rotation must be of unit magnitude. axisOfRotation: " + axisOfRotation);

      if (!MathTools.epsilonEquals(0.0, axisOfRotation.dot(axisOfRotationDot), epsilon))
         throw new RuntimeException("derivative of axis of rotation has a component along the axis of rotation");

      this.bodyFrame = bodyFrame;
      this.baseFrame = baseFrame;
      this.expressedInFrame = expressedInFrame;

      getLinearPart().cross(offsetDot, axisOfRotation);
      tempVector.cross(offset, axisOfRotationDot);
      getLinearPart().add(tempVector);
      getLinearPart().scale(angularVelocityMagnitude);

      tempVector.set(axisOfRotation);
      tempVector.scale(linearAccelerationMagnitude);
      getLinearPart().add(tempVector);

      tempVector.set(axisOfRotationDot);
      tempVector.scale(linearVelocityMagnitude);
      getLinearPart().add(tempVector);

      tempVector.cross(offset, axisOfRotation);
      tempVector.scale(angularAccelerationMagnitude);
      getLinearPart().add(tempVector);

      getAngularPart().set(axisOfRotation);
      getAngularPart().scale(angularAccelerationMagnitude);
      tempVector.set(axisOfRotationDot);
      tempVector.scale(angularVelocityMagnitude);
      getAngularPart().add(tempVector);
   }

   ///CLOVER:OFF
   @Override
   public String toString()
   {
      String ret = new String("Spatial acceleration of " + bodyFrame + ", with respect to " + baseFrame + ", expressed in " + expressedInFrame + "\n"
            + "Linear part: " + getLinearPart() + "\n" + "Angular part: " + getAngularPart());

      return ret;
   }
   ///CLOVER:ON
}
