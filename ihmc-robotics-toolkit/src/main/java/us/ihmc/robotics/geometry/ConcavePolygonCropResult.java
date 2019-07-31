package us.ihmc.robotics.geometry;

// Colinear parallel edge intersection not possible due to EuclidGeometryTools#intersectionBetweenLine2DAndLineSegment2D implementation,
// which reads "When the line and the line segment are collinear, they are assumed to intersect at lineSegmentStart."
public enum ConcavePolygonCropResult
{
   KEEP_ALL,
   REMOVE_ALL,
   CUT
   ;

   public static ConcavePolygonCropResult fromConvexPolygonCropResult(ConvexPolygonCropResult convexPolygonCropResult)
   {
      switch (convexPolygonCropResult)
      {
         case REMOVE_ALL:
            return REMOVE_ALL;
         case KEEP_ALL:
            return KEEP_ALL;
         case CUT:
         default:
            return CUT;
      }
   }
}
