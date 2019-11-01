package us.ihmc.commonWalkingControlModules.polygonWiggling;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.convexOptimization.quadraticProgram.QuadProgSolver;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.geometry.ConvexPolygonTools;
import us.ihmc.robotics.geometry.PlanarRegion;

public class PolygonWiggler
{
   private static final boolean DEBUG = false;
   private static final boolean coldStart = true;

   /** Weight associated with moving into the polygon. */
   private static final double polygonWeight = 1.0e6;
   /** Regularization weight preferring a zero solution. */
   private static final double regularization = 1.0e-10;
   /** Weight associated with moving the polygon. */
   private static final double moveWeight = 1.0;

   private static int[] emptyArray = new int[0];

   /**
    * Returns a transform that will move the given polygon into the convex hull of a planar region.
    *
    * @param polygonToWiggleInRegionFrame
    * @param regionToWiggleInto
    * @param wiggleParameters
    * @return
    */
   public static RigidBodyTransform wigglePolygonIntoConvexHullOfRegion(ConvexPolygon2D polygonToWiggleInRegionFrame, PlanarRegion regionToWiggleInto, WiggleParameters parameters)
   {
      return findWiggleTransform(polygonToWiggleInRegionFrame, regionToWiggleInto.getConvexHull(), parameters);
   }

   /**
    * Returns a transform that will move the given polygon into a planar region. Problematic if the planar region consists of
    * multiple sub convex polygons. The polygon to wiggle must have the same transform to world as the planar region.
    *
    * @param polygonToWiggleInRegionFrame
    * @param regionToWiggleInto
    * @param wiggleParameters
    * @return
    */
   public static RigidBodyTransform wigglePolygonIntoRegion(ConvexPolygon2D polygonToWiggleInRegionFrame, PlanarRegion regionToWiggleInto, WiggleParameters parameters)
   {
      // find the part of the region that has the biggest intersection with the polygon
      ConvexPolygon2D bestMatch = null;
      double overlap = Double.NEGATIVE_INFINITY;
      ConvexPolygonTools convexPolygonTools = new ConvexPolygonTools();
      for (int i = 0; i < regionToWiggleInto.getNumberOfConvexPolygons(); i++)
      {
         ConvexPolygon2D intersection = new ConvexPolygon2D();
         convexPolygonTools.computeIntersectionOfPolygons(regionToWiggleInto.getConvexPolygon(i), polygonToWiggleInRegionFrame, intersection);
         if (intersection.getArea() > overlap)
         {
            overlap = intersection.getArea();
            bestMatch = regionToWiggleInto.getConvexPolygon(i);
         }
      }

      if (bestMatch == null)
         return null;

      return findWiggleTransform(polygonToWiggleInRegionFrame, bestMatch, parameters);
   }

   /**
    * This method moves a convex polygon into a given convex region. It will return the new polygon without modifying the given one.
    * The algorithm assumes a small rotation angle (it will linearize sin and cos around 0.0). For that reason it is possible to
    * specify a maximum and a minimum rotation.
    *
    * @param polygonToWiggle
    * @param planeToWiggleInto
    * @param wiggleParameters
    * @return
    */
   public static ConvexPolygon2D wigglePolygon(ConvexPolygon2D polygonToWiggle, ConvexPolygon2DReadOnly planeToWiggleInto, WiggleParameters parameters)
   {
      return wigglePolygon(polygonToWiggle, planeToWiggleInto, parameters, emptyArray);
   }

   /**
    * This method moves a convex polygon into a given convex region. It will return the new polygon without modifying the given one.
    * The algorithm assumes a small rotation angle (it will linearize sin and cos around 0.0). For that reason it is possible to
    * specify a maximum and a minimum rotation.
    *
    * @param polygonToWiggle
    * @param planeToWiggleInto
    * @param wiggleParameters
    * @return
    */
   public static ConvexPolygon2D wigglePolygon(ConvexPolygon2D polygonToWiggle, ConvexPolygon2DReadOnly planeToWiggleInto, WiggleParameters parameters, int[] indicesToExclude)
   {
      ConvexPolygon2D wiggledPolygon = new ConvexPolygon2D(polygonToWiggle);
      RigidBodyTransform wiggleTransform = findWiggleTransform(polygonToWiggle, planeToWiggleInto, parameters, indicesToExclude);
      if (wiggleTransform == null)
         return null;
      wiggledPolygon.applyTransform(wiggleTransform, false);
      return wiggledPolygon;
   }

   /**
    * This method will find a transform that moves a convex polygon into a given convex region. The algorithm assumes a small rotation
    * angle (it will linearize sin and cos around 0.0). For that reason it is possible to specify a maximum and a minimum rotation.
    */
   public static RigidBodyTransform findWiggleTransform(ConvexPolygon2DReadOnly polygonToWiggle, ConvexPolygon2DReadOnly planeToWiggleInto, WiggleParameters parameters)
   {
      return findWiggleTransform(polygonToWiggle, planeToWiggleInto, parameters, emptyArray);
   }

   /**
    * This method will find a transform that moves a convex polygon into a given convex region. The algorithm assumes a small rotation
    * angle (it will linearize sin and cos around 0.0). For that reason it is possible to specify a maximum and a minimum rotation.
    */
   public static RigidBodyTransform findWiggleTransform(ConvexPolygon2DReadOnly polygonToWiggle, ConvexPolygon2DReadOnly planeToWiggleInto, WiggleParameters parameters,
                                                        int[] startingVerticesToIgnore)
   {
      int numberOfPoints = polygonToWiggle.getNumberOfVertices();
      Point2DReadOnly pointToRotateAbout = polygonToWiggle.getCentroid();

      // This creates inequality constraints for points to lie inside the desired polygon.
      DenseMatrix64F A = new DenseMatrix64F(0);
      DenseMatrix64F b = new DenseMatrix64F(0);
      convertToInequalityConstraints(planeToWiggleInto, A, b, parameters.deltaInside, startingVerticesToIgnore);

      int constraintsPerPoint = A.getNumRows();

      int boundConstraints = 6;
      DenseMatrix64F A_full = new DenseMatrix64F(constraintsPerPoint * numberOfPoints + boundConstraints, 3 + constraintsPerPoint * numberOfPoints);
      DenseMatrix64F b_full = new DenseMatrix64F(constraintsPerPoint * numberOfPoints + boundConstraints, 1);
      // add limits on allowed rotation and translation
      A_full.set(constraintsPerPoint * numberOfPoints , 0, 1.0);
      b_full.set(constraintsPerPoint * numberOfPoints , parameters.maxX);
      A_full.set(constraintsPerPoint * numberOfPoints + 1, 0, -1.0);
      b_full.set(constraintsPerPoint * numberOfPoints + 1, -parameters.minX);
      A_full.set(constraintsPerPoint * numberOfPoints + 2, 1, 1.0);
      b_full.set(constraintsPerPoint * numberOfPoints + 2, parameters.maxY);
      A_full.set(constraintsPerPoint * numberOfPoints + 3, 1, -1.0);
      b_full.set(constraintsPerPoint * numberOfPoints + 3, -parameters.minY);
      A_full.set(constraintsPerPoint * numberOfPoints + 4, 2, 1.0);
      b_full.set(constraintsPerPoint * numberOfPoints + 4, parameters.maxYaw);
      A_full.set(constraintsPerPoint * numberOfPoints + 5, 2, -1.0);
      b_full.set(constraintsPerPoint * numberOfPoints + 5, -parameters.minYaw);

      // The inequality constraints of form
      // Ax <= b
      // are converted to new constraints with a new optimization vector s:
      // Ax - s - b == 0.0
      // s <= 0
      // The equality constraint will be converted to an objective causing the wiggler to do the best it can instead of failing when the wiggle is not possible.
      DenseMatrix64F Aeq = new DenseMatrix64F(constraintsPerPoint * numberOfPoints, 3 + constraintsPerPoint * numberOfPoints);
      DenseMatrix64F beq = new DenseMatrix64F(constraintsPerPoint * numberOfPoints, 1);
      DenseMatrix64F indentity = new DenseMatrix64F(constraintsPerPoint * numberOfPoints, constraintsPerPoint * numberOfPoints);
      CommonOps.setIdentity(indentity);
      CommonOps.insert(indentity, A_full, 0, 3);
      CommonOps.scale(-1.0, indentity);
      CommonOps.insert(indentity, Aeq, 0, 3);

      for (int i = 0; i < numberOfPoints; i++)
      {
         DenseMatrix64F p = new DenseMatrix64F(2, 1);
         p.set(0, polygonToWiggle.getVertex(i).getX());
         p.set(1, polygonToWiggle.getVertex(i).getY());

         // inequality constraint becomes A*V * x <= b - A*p
         Point2D point = new Point2D(polygonToWiggle.getVertex(i));
         point.sub(pointToRotateAbout);
         DenseMatrix64F V = new DenseMatrix64F(new double[][] {{1.0, 0.0, -point.getY()}, {0.0, 1.0, point.getX()}});

         DenseMatrix64F A_new = new DenseMatrix64F(constraintsPerPoint, 3);
         DenseMatrix64F b_new = new DenseMatrix64F(constraintsPerPoint, 1);
         CommonOps.mult(A, V, A_new);
         CommonOps.mult(A, p, b_new);
         CommonOps.changeSign(b_new);
         CommonOps.add(b, b_new, b_new);

         CommonOps.insert(A_new, Aeq, constraintsPerPoint * i, 0);
         CommonOps.insert(b_new, beq, constraintsPerPoint * i, 0);
      }

      // Convert the inequality constraint for being inside the polygon to an objective.
      DenseMatrix64F costMatrix = new DenseMatrix64F(3 + constraintsPerPoint * numberOfPoints, 3 + constraintsPerPoint * numberOfPoints);
      CommonOps.multInner(Aeq, costMatrix);
      DenseMatrix64F costVector = new DenseMatrix64F(3 + constraintsPerPoint * numberOfPoints, 1);
      CommonOps.multTransA(Aeq, beq, costVector);
      CommonOps.changeSign(costVector);
      CommonOps.scale(polygonWeight, costMatrix);
      CommonOps.scale(polygonWeight, costVector);

      // Add regularization
      indentity.reshape(3 + constraintsPerPoint * numberOfPoints, 3 + constraintsPerPoint * numberOfPoints);
      CommonOps.setIdentity(indentity);
      CommonOps.scale(regularization, indentity);
      CommonOps.add(costMatrix, indentity, costMatrix);

      // Add movement weight
      costMatrix.add(0, 0, moveWeight);
      costMatrix.add(1, 1, moveWeight);
      costMatrix.add(2, 2, moveWeight * parameters.rotationWeight);

      QuadProgSolver solver = new QuadProgSolver();
      DenseMatrix64F result = new DenseMatrix64F(3 + constraintsPerPoint * numberOfPoints, 1);
      Aeq = new DenseMatrix64F(0, 3 + constraintsPerPoint * numberOfPoints);
      beq = new DenseMatrix64F(0, 3 + constraintsPerPoint * numberOfPoints);
      try
      {
         int iterations = solver.solve(costMatrix, costVector, Aeq, beq, A_full, b_full, result, coldStart);
         if (DEBUG)
         {
            LogTools.info("Iterations: " + iterations);
            LogTools.info("Result: " + result);
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
         return null;
      }

      if (Double.isInfinite(solver.getCost()))
      {
         LogTools.info("Could not wiggle!");
         return null;
      }

      // assemble the transform
      double theta = result.get(2);
      Vector3D translation = new Vector3D(result.get(0), result.get(1), 0.0);
      Vector3D offset = new Vector3D(pointToRotateAbout.getX(), pointToRotateAbout.getY(), 0.0);

      RigidBodyTransform toOriginTransform = new RigidBodyTransform();
      toOriginTransform.setTranslationAndIdentityRotation(offset);

      RigidBodyTransform rotationTransform = new RigidBodyTransform();
      rotationTransform.appendYawRotation(theta);

      RigidBodyTransform fullTransform = new RigidBodyTransform(toOriginTransform);
      fullTransform.multiply(rotationTransform);
      toOriginTransform.invert();
      fullTransform.multiply(toOriginTransform);

      RotationMatrix rotationMatrix = new RotationMatrix();
      rotationTransform.getRotation(rotationMatrix);
      rotationMatrix.transpose();
      rotationMatrix.transform(translation);
      RigidBodyTransform translationTransform = new RigidBodyTransform();
      translationTransform.setTranslationAndIdentityRotation(translation);
      fullTransform.multiply(translationTransform);

      return fullTransform;
   }

   /**
    * Packs the matrices A and b such that any point x is inside the polygon if it satisfies the equation A*x <= b.
    */
   public static void convertToInequalityConstraints(ConvexPolygon2DReadOnly polygon, DenseMatrix64F A, DenseMatrix64F b, double deltaInside)
   {
      convertToInequalityConstraints(polygon, A, b, deltaInside, emptyArray);
   }

   /**
    * Packs the matrices A and b such that any point x is inside the polygon if it satisfies the equation A*x <= b.
    */
   public static void convertToInequalityConstraints(ConvexPolygon2DReadOnly polygon, DenseMatrix64F A, DenseMatrix64F b, double deltaInside,
                                                     int[] startingVerticesToIgnore)
   {
      int constraints = polygon.getNumberOfVertices();

      if (constraints > 2)
         convertToInequalityConstraintsPolygon(polygon, A, b, deltaInside, startingVerticesToIgnore);
      else if (constraints > 1)
         convertToInequalityConstraintsLine(polygon, A, b, deltaInside);
      else
         convertToInequalityConstraintsPoint(polygon, A, b);
   }

   public static void convertToInequalityConstraintsPoint(ConvexPolygon2DReadOnly polygon, DenseMatrix64F A, DenseMatrix64F b)
   {
      convertToInequalityConstraintsPoint(polygon.getVertex(0), A, b);
   }

   public static void convertToInequalityConstraintsPoint(Point2DReadOnly point, DenseMatrix64F A, DenseMatrix64F b)
   {
      convertToInequalityConstraintsPoint(point.getX(), point.getY(), A, b);
   }

   public static void convertToInequalityConstraintsPoint(double x, double y, DenseMatrix64F A, DenseMatrix64F b)
   {
      A.reshape(4, 2);
      b.reshape(4, 1);

      A.set(0, 0, 1.0);
      A.set(0, 1, 0.0);
      A.set(1, 0, 0.0);
      A.set(1, 1, 1.0);
      A.set(2, 0, -1.0);
      A.set(2, 1, 0.0);
      A.set(3, 0, 0.0);
      A.set(3, 1, -1.0);

      b.set(0, 0, x);
      b.set(1, 0, y);
      b.set(2, 0, -x);
      b.set(3, 0, -y);
   }

   public static void convertToInequalityConstraintsLine(ConvexPolygon2DReadOnly polygon, DenseMatrix64F A, DenseMatrix64F b, double deltaInside)
   {
      // constrain to lying on 2d line
      Point2DReadOnly firstPoint = polygon.getVertex(0);
      Point2DReadOnly secondPoint = polygon.getNextVertex(0);

      // if the distance isn't long enough, then this is equivalently a line constraint.
      if (firstPoint.distance(secondPoint) <= 2.0 * deltaInside)
      {
         double x = 0.5 * (firstPoint.getX() + secondPoint.getX());
         double y = 0.5 * (firstPoint.getY() + secondPoint.getY());

         convertToInequalityConstraintsPoint(x, y, A, b);
      }
      else
      {
         A.reshape(4, 2);
         b.reshape(4, 1);

         double vectorX = secondPoint.getX() - firstPoint.getX();
         double vectorY = secondPoint.getY() - firstPoint.getY();

         // set regular line. This is done by saying y <= ax + b and y >= ax + b, which only has a solution at when y = ax + b.
         /*
         The standard line equation is y <= ax + b. This can then be written as -ax + y <= b. Another way to write this is then the same thing as saying
         -(y2 - y1) / (x2 - x1) x + y <= y1 - (y2 - y1) / (x2 - x1) x1, which can then again be rewritten as
         -(y2 - y1) x + (x2 - x1) y <= (x2 - x1) y1 - (y2 - y1) x1.

         If we say that vy = y2 - y1 and vx = x2 - x1, then this is simply -vy x + vx y <= vx y1 - vy x1. We can additionally shift this inside by d by saying
         -vy x + vx y <= vx y1 - vy x1 - d.
          */
         A.set(0, 0, -vectorY);
         A.set(0, 1, vectorX);
         b.set(0, firstPoint.getY() * vectorX - firstPoint.getX() * vectorY);

         A.set(1, 0, vectorY);
         A.set(1, 1, -vectorX);
         b.set(1, -b.get(0));

         // set first point boundary line
         A.set(2, 0, -vectorX);
         A.set(2, 1, -vectorY);
         b.set(2, -deltaInside - firstPoint.getX() * vectorX - firstPoint.getY() * vectorY);

         // set second point boundary line
         A.set(3, 0, vectorX);
         A.set(3, 1, vectorY);
         b.set(3, -deltaInside + secondPoint.getX() * vectorX + secondPoint.getY() * vectorY);
      }
   }

   public static void convertToInequalityConstraintsPolygon(ConvexPolygon2DReadOnly polygon, DenseMatrix64F A, DenseMatrix64F b, double deltaInside)
   {
      convertToInequalityConstraintsPolygon(polygon, A, b, deltaInside, emptyArray);
   }

   public static void convertToInequalityConstraintsPolygon(ConvexPolygon2DReadOnly polygon, DenseMatrix64F A, DenseMatrix64F b, double deltaInside,
                                                            int[] startingVerticesToIgnore)
   {
      int constraints = polygon.getNumberOfVertices();
      A.reshape(constraints, 2);
      b.reshape(constraints, 1);

      for (int i = 0; i < constraints; i++)
      {
         Point2DReadOnly firstPoint = polygon.getVertex(i);
         Point2DReadOnly secondPoint = polygon.getNextVertex(i);

         double x = secondPoint.getX() - firstPoint.getX();
         double y = secondPoint.getY() - firstPoint.getY();
         double norm = Math.sqrt(x * x + y * y);
         x = x / norm;
         y = y / norm;

         double desiredDistanceInside;
         if (isAllowed(i, startingVerticesToIgnore))
            desiredDistanceInside = deltaInside;
         else
            desiredDistanceInside = 0.0;

         A.set(i, 0, -y);
         A.set(i, 1, x);
         b.set(i, -desiredDistanceInside + firstPoint.getY() * x - firstPoint.getX() * y);

         //         A.set(i, 0, firstPoint.y - secondPoint.y);
         //         A.set(i, 1, -firstPoint.x + secondPoint.x);
         //         b.set(i, firstPoint.y * (secondPoint.x - firstPoint.x) - firstPoint.x * (secondPoint.y - firstPoint.y));
      }
   }

   private static boolean isAllowed(int index, int... indicesToIgnore)
   {
      for (int i = 0; i < indicesToIgnore.length; i++)
      {
         if (index == indicesToIgnore[i])
            return false;
      }

      return true;
   }

   public static void constrainPolygonInsideOtherPolygon(ConvexPolygon2DReadOnly polygonExteriorInWorld, ConvexPolygon2DReadOnly polygonInteriorRelative, DenseMatrix64F A, DenseMatrix64F b, double deltaInside)
   {
      int constraints = polygonExteriorInWorld.getNumberOfVertices();
      A.reshape(constraints, 2);
      b.reshape(constraints, 1);

      Vector2D tempVector = new Vector2D();

      for (int exteriorVertexIndex = 0; exteriorVertexIndex < constraints; exteriorVertexIndex++)
      {
         Point2DReadOnly firstPoint = polygonExteriorInWorld.getVertex(exteriorVertexIndex);
         Point2DReadOnly secondPoint = polygonExteriorInWorld.getNextVertex(exteriorVertexIndex);

         tempVector.set(secondPoint);
         tempVector.sub(firstPoint);

         tempVector.normalize();

         A.set(exteriorVertexIndex, 0, -tempVector.getY());
         A.set(exteriorVertexIndex, 1, tempVector.getX());

         double additionalDistanceInside = 0.0;
         for (int interiorVertexIndex = 0; interiorVertexIndex < polygonInteriorRelative.getNumberOfVertices(); interiorVertexIndex++)
         {
            Point2DReadOnly interiorVertex = polygonInteriorRelative.getVertex(interiorVertexIndex);
            additionalDistanceInside = Math.max(additionalDistanceInside, tempVector.getY() * interiorVertex.getX() - tempVector.getX() * interiorVertex.getY());
         }

         A.set(exteriorVertexIndex, 0, -tempVector.getY());
         A.set(exteriorVertexIndex, 1, tempVector.getX());
         b.set(exteriorVertexIndex, -(deltaInside + additionalDistanceInside) + firstPoint.getY() * (tempVector.getX()) - firstPoint.getX() * (tempVector.getY()));
      }
   }

}
