package us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner;

import org.ejml.data.D1Matrix64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;

import us.ihmc.euclid.Axis;

/**
 * {@code DenseMatrixVector3D} represents a 3D vector holding onto three independent matrices for
 * its x, y, and z components.
 * <p>
 * This class is mostly meant for simplifying the data structure for matrix calculation when applied
 * onto 3D objects and the three components are independent from each other.
 * </p>
 */
public class DenseMatrixVector3D
{
   /** Array of the x, y, and z matrices for quick index based access. */
   private final DenseMatrix64F[] matrixArray;
   /** The three components of this vector. */
   private final DenseMatrix64F x, y, z;

   /**
    * Creates a new vector 3D of {@code DenseMatrix64F} and initializes the size of each
    * matrix-component.
    * 
    * @param numRows the number of rows for each component.
    * @param numCols the number of columns for each component.
    */
   public DenseMatrixVector3D(int numRows, int numCols)
   {
      x = new DenseMatrix64F(numRows, numCols);
      y = new DenseMatrix64F(numRows, numCols);
      z = new DenseMatrix64F(numRows, numCols);
      matrixArray = new DenseMatrix64F[] {x, y, z};
   }

   /**
    * Reshapes independently each matrix of this vector 3D.
    * 
    * @param numRows the number of rows for each component.
    * @param numCols the number of columns for each component.
    * @see DenseMatrix64F#reshape(int, int)
    */
   public void reshape(int numRows, int numCols)
   {
      x.reshape(numRows, numCols);
      y.reshape(numRows, numCols);
      z.reshape(numRows, numCols);
   }

   /**
    * Sets all components to zero.
    * 
    * @see DenseMatrix64F#zero()
    */
   public void zero()
   {
      x.zero();
      y.zero();
      z.zero();
   }

   /**
    * Sets this vector 3D to {@code other}.
    * 
    * @param other the other vector to copy values from. Not modified.
    */
   public void set(DenseMatrixVector3D other)
   {
      x.set(other.x);
      y.set(other.y);
      z.set(other.z);
   }

   /**
    * Adds independently the x, y, and z components of {@code other} to the components of
    * {@code this}:
    * 
    * <pre>
    * this += other
    * </pre>
    * 
    * @param other the other vector to add to {@code this}. Not modified.
    * @see CommonOps#addEquals(D1Matrix64F, D1Matrix64F)
    */
   public void add(DenseMatrixVector3D other)
   {
      CommonOps.addEquals(x, other.x);
      CommonOps.addEquals(y, other.y);
      CommonOps.addEquals(z, other.z);
   }

   /**
    * Sets the components of this vector to the sum of {@code a} and {@code b}:
    * 
    * <pre>
    * this = a + b
    * </pre>
    * 
    * @param a the first term in the addition. Not modified.
    * @param b the second term in the addition. Not modified.
    * @see CommonOps#add(D1Matrix64F, D1Matrix64F, D1Matrix64F)
    */
   public void add(DenseMatrixVector3D a, DenseMatrixVector3D b)
   {
      CommonOps.add(a.x, b.x, x);
      CommonOps.add(a.y, b.y, y);
      CommonOps.add(a.z, b.z, z);
   }

   /**
    * Subtracts independently the x, y, and z components of {@code other} to the components of
    * {@code this}:
    * 
    * <pre>
    * this -= other
    * </pre>
    * 
    * @param other the other vector to subtract to {@code this}. Not modified.
    * @see CommonOps#subtractEquals(D1Matrix64F, D1Matrix64F)
    */
   public void sub(DenseMatrixVector3D other)
   {
      CommonOps.subtractEquals(x, other.x);
      CommonOps.subtractEquals(y, other.y);
      CommonOps.subtractEquals(z, other.z);
   }

   /**
    * Sets the components of this vector to the difference of {@code a} and {@code b}:
    * 
    * <pre>
    * this = a - b
    * </pre>
    * 
    * @param a the first term in the difference. Not modified.
    * @param b the second term in the difference. Not modified.
    * @see CommonOps#subtract(D1Matrix64F, D1Matrix64F, D1Matrix64F)
    */
   public void sub(DenseMatrixVector3D a, DenseMatrixVector3D b)
   {
      CommonOps.subtract(a.x, b.x, x);
      CommonOps.subtract(a.y, b.y, y);
      CommonOps.subtract(a.z, b.z, z);
   }

   /**
    * Gets the matrix corresponding to the given {@code axis}.
    * 
    * @param axis either X, Y, or Z.
    * @return the corresponding matrix.
    */
   public DenseMatrix64F getMatrix(Axis axis)
   {
      return matrixArray[axis.ordinal()];
   }

   /**
    * Gets the matrix corresponding to the given {@code axisIndex}: 0 for the x matrix, 1 for the y
    * matrix, and 2 for the z matrix.
    * 
    * @param axisIndex the axis index &in; [0, 2].
    * @return the corresponding matrix.
    */
   public DenseMatrix64F getMatrix(int axisIndex)
   {
      return matrixArray[axisIndex];
   }

   /**
    * The matrix corresponding to the x-component of this vector.
    * 
    * @return the x matrix.
    */
   public DenseMatrix64F getMatrixX()
   {
      return x;
   }

   /**
    * The matrix corresponding to the y-component of this vector.
    * 
    * @return the y matrix.
    */
   public DenseMatrix64F getMatrixY()
   {
      return y;
   }

   /**
    * The matrix corresponding to the z-component of this vector.
    * 
    * @return the z matrix.
    */
   public DenseMatrix64F getMatrixZ()
   {
      return z;
   }

   /**
    * Tests on a per matrix-coefficient basis if this vector of matrices is equal to {@code other}
    * to an {@code epsilon}.
    * 
    * @param other the other vector to compare against this. Not modified.
    * @param epsilon the tolerance to use for this test.
    * @return {@code true} if the two vectors are considered equal, {@code false} otherwise.
    */
   public boolean epsilonEquals(DenseMatrixVector3D other, double epsilon)
   {
      if (!MatrixFeatures.isEquals(x, other.x, epsilon))
         return false;
      if (!MatrixFeatures.isEquals(y, other.y, epsilon))
         return false;
      if (!MatrixFeatures.isEquals(z, other.z, epsilon))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      return "x = " + x + "\ny = " + y + "\nz = " + z;
   }
}
