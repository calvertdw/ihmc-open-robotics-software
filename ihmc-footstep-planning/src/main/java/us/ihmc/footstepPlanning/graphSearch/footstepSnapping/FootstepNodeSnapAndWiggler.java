package us.ihmc.footstepPlanning.graphSearch.footstepSnapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.ihmc.commonWalkingControlModules.polygonWiggling.ConcavePolygonWiggler;
import us.ihmc.commonWalkingControlModules.polygonWiggling.PolygonWiggler;
import us.ihmc.commonWalkingControlModules.polygonWiggling.WiggleParameters;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNodeTools;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.footstepPlanning.polygonSnapping.PlanarRegionsListPolygonSnapper;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.SideDependentList;

public class FootstepNodeSnapAndWiggler implements FootstepNodeSnapperReadOnly
{
   private final SideDependentList<ConvexPolygon2D> footPolygonsInSoleFrame;
   private final FootstepPlannerParametersReadOnly parameters;

   private final ConcavePolygonWiggler concavePolygonWiggler = new ConcavePolygonWiggler();
   private final WiggleParameters wiggleParameters = new WiggleParameters();
   private final PlanarRegion planarRegionToPack = new PlanarRegion();
   private final ConvexPolygon2D footPolygon = new ConvexPolygon2D();

   private final HashMap<FootstepNode, FootstepNodeSnapData> snapDataHolder = new HashMap<>();
   protected PlanarRegionsList planarRegionsList;
   private final RigidBodyTransform tempTransform = new RigidBodyTransform();

   public FootstepNodeSnapAndWiggler(SideDependentList<ConvexPolygon2D> footPolygonsInSoleFrame, FootstepPlannerParametersReadOnly parameters)
   {
      this.footPolygonsInSoleFrame = footPolygonsInSoleFrame;
      this.parameters = parameters;
   }

   public void setPlanarRegions(PlanarRegionsList planarRegionsList)
   {
      this.planarRegionsList = planarRegionsList;
      snapDataHolder.clear();
   }

   public void initialize()
   {
      updateWiggleParameters(wiggleParameters, parameters);
   }

   private boolean flatGroundMode()
   {
      return planarRegionsList == null || planarRegionsList.isEmpty();
   }

   public FootstepNodeSnapData snapFootstepNode(FootstepNode footstepNode)
   {
      return snapFootstepNode(footstepNode, null,false);
   }

   @Override
   public FootstepNodeSnapData snapFootstepNode(FootstepNode footstepNode, FootstepNode stanceNode, boolean computeWiggleTransform)
   {
      if (snapDataHolder.containsKey(footstepNode))
      {
         FootstepNodeSnapData snapData = snapDataHolder.get(footstepNode);
         if (snapData.getSnapTransform().containsNaN())
         {
            return snapData;
         }
         else if (snapData.getWiggleTransformInWorld().containsNaN() && computeWiggleTransform)
         {
            computeWiggleTransform(footstepNode, snapData);
         }

         return snapData;
      }
      else if (flatGroundMode())
      {
         return FootstepNodeSnapData.identityData();
      }
      else
      {
         FootstepNodeSnapData snapData = computeSnapTransform(footstepNode, stanceNode);
         snapDataHolder.put(footstepNode, snapData);

         if (snapData.getSnapTransform().containsNaN())
         {
            return snapData;
         }
         else if (computeWiggleTransform)
         {
            computeWiggleTransform(footstepNode, snapData);
         }

         return snapData;
      }
   }

   /**
    * Can manually add snap data for a footstep node to bypass the snapper.
    */
   public void addSnapData(FootstepNode footstepNode, FootstepNodeSnapData snapData)
   {
      snapDataHolder.put(footstepNode, snapData);
   }

   protected FootstepNodeSnapData computeSnapTransform(FootstepNode footstepNode, FootstepNode stanceNode)
   {
      double maximumRegionHeightToConsider = getMaximumRegionHeightToConsider(stanceNode);
      FootstepNodeTools.getFootPolygon(footstepNode, footPolygonsInSoleFrame.get(footstepNode.getRobotSide()), footPolygon);

      RigidBodyTransform snapTransform = PlanarRegionsListPolygonSnapper.snapPolygonToPlanarRegionsList(footPolygon, planarRegionsList, maximumRegionHeightToConsider, planarRegionToPack);
      if (snapTransform == null)
      {
         return FootstepNodeSnapData.emptyData();
      }
      else
      {
         FootstepNodeSnapData snapData = new FootstepNodeSnapData(snapTransform);
         snapData.setPlanarRegionId(planarRegionToPack.getRegionId());
         computeCroppedFoothold(footstepNode, snapData);
         return snapData;
      }
   }

   private double getMaximumRegionHeightToConsider(FootstepNode stanceNode)
   {
      if (stanceNode == null)
      {
         return Double.POSITIVE_INFINITY;
      }

      FootstepNodeSnapData snapData = snapDataHolder.get(stanceNode);
      if (snapData == null || snapData.getSnapTransform().containsNaN())
      {
         return Double.POSITIVE_INFINITY;
      }
      else
      {
         return parameters.getMaximumSnapHeight() + FootstepNodeTools.getSnappedNodeHeight(stanceNode, snapData.getSnapTransform());
      }
   }

   protected void computeWiggleTransform(FootstepNode footstepNode, FootstepNodeSnapData snapData)
   {
      boolean foundMatchingRegion = false;
      planarRegionToPack.isEmpty();
      for (PlanarRegion region : planarRegionsList.getPlanarRegionsAsList())
      {
         if (region.getRegionId() == snapData.getPlanarRegionId())
         {
            foundMatchingRegion = true;
            planarRegionToPack.set(region);
            break;
         }
      }

      if (!foundMatchingRegion)
      {
         LogTools.warn("Could not find matching region id, unable to find wiggle transform. Region id = " + snapData.getPlanarRegionId());
         snapData.getWiggleTransformInWorld().setIdentity();
         return;
      }

      FootstepNodeTools.getFootPolygon(footstepNode, footPolygonsInSoleFrame.get(footstepNode.getRobotSide()), footPolygon);
      tempTransform.set(snapData.getSnapTransform());
      tempTransform.preMultiply(planarRegionToPack.getTransformToLocal());
      ConvexPolygon2D footPolygonInRegionFrame = FootstepNodeSnappingTools.computeTransformedPolygon(footPolygon, tempTransform);

      RigidBodyTransform wiggleTransformInLocal;
      if (parameters.getEnableConcaveHullWiggler() && !planarRegionToPack.getConcaveHull().isEmpty())
      {
         wiggleTransformInLocal = concavePolygonWiggler.wigglePolygon(footPolygonInRegionFrame,
                                                               Vertex2DSupplier.asVertex2DSupplier(planarRegionToPack.getConcaveHull()),
                                                               wiggleParameters);
      }
      else
      {
         if (isConvexConstraintSatisfied(footPolygonInRegionFrame, planarRegionToPack, parameters.getWiggleInsideDelta()))
         {
            snapData.getWiggleTransformInWorld().setIdentity();
            return;
         }
         else
         {
            if ((wiggleTransformInLocal = wiggleIntoConvexHull(footPolygonInRegionFrame)) == null)
            {
               snapData.getWiggleTransformInWorld().setIdentity();
               return;
            }
         }
      }

      snapData.getWiggleTransformInWorld().set(planarRegionToPack.getTransformToLocal());
      snapData.getWiggleTransformInWorld().preMultiply(wiggleTransformInLocal);
      snapData.getWiggleTransformInWorld().preMultiply(planarRegionToPack.getTransformToWorld());
      computeCroppedFoothold(footstepNode, snapData);
   }

   /** Extracted to method for testing purposes */
   protected RigidBodyTransform wiggleIntoConvexHull(ConvexPolygon2D footPolygonInRegionFrame)
   {
      return PolygonWiggler.wigglePolygonIntoConvexHullOfRegion(footPolygonInRegionFrame, planarRegionToPack, wiggleParameters);
   }

   protected void computeCroppedFoothold(FootstepNode footstepNode, FootstepNodeSnapData snapData)
   {
      if (flatGroundMode())
      {
         snapData.getCroppedFoothold().clearAndUpdate();
         return;
      }

      FootstepNodeTools.getFootPolygon(footstepNode, footPolygonsInSoleFrame.get(footstepNode.getRobotSide()), footPolygon);

      snapData.packSnapAndWiggleTransform(tempTransform);
      ConvexPolygon2D snappedPolygonInWorld = FootstepNodeSnappingTools.computeTransformedPolygon(footPolygon, tempTransform);
      ConvexPolygon2D croppedFootPolygon = FootstepNodeSnappingTools.computeRegionIntersection(planarRegionToPack, snappedPolygonInWorld);
      if (!croppedFootPolygon.isEmpty())
      {
         FootstepNodeSnappingTools.changeFromPlanarRegionToSoleFrame(planarRegionToPack, footstepNode, tempTransform, croppedFootPolygon);
         snapData.getCroppedFoothold().set(croppedFootPolygon);
      }
   }

   private static void updateWiggleParameters(WiggleParameters wiggleParameters, FootstepPlannerParametersReadOnly parameters)
   {
      wiggleParameters.deltaInside = parameters.getWiggleInsideDelta();
      wiggleParameters.maxX = parameters.getMaximumXYWiggleDistance();
      wiggleParameters.minX = -parameters.getMaximumXYWiggleDistance();
      wiggleParameters.maxY = parameters.getMaximumXYWiggleDistance();
      wiggleParameters.minY = -parameters.getMaximumXYWiggleDistance();
      wiggleParameters.maxYaw = parameters.getMaximumYawWiggle();
      wiggleParameters.minYaw = -parameters.getMaximumYawWiggle();
   }

   private static boolean isConvexConstraintSatisfied(ConvexPolygon2DReadOnly footPolygon, PlanarRegion planarRegion, double wiggleInsideDelta)
   {
      for (int i = 0; i < footPolygon.getNumberOfVertices(); i++)
      {
         Point2DReadOnly vertex = footPolygon.getVertex(i);
         if (planarRegion.getConvexHull().signedDistance(vertex) > - wiggleInsideDelta)
         {
            return false;
         }
      }

      return true;
   }

   private static boolean checkForTooMuchPenetrationAfterWiggle(PlanarRegion highestElevationPlanarRegion,
                                                                ConvexPolygon2D footPolygonInWorld,
                                                                List<PlanarRegion> planarRegionsIntersectingSnappedAndWiggledPolygon,
                                                                double maximumZPenetrationOnValleyRegions)
   {
      if (planarRegionsIntersectingSnappedAndWiggledPolygon != null)
      {
         ArrayList<ConvexPolygon2D> intersectionsInPlaneFrameToPack = new ArrayList<>();
         RigidBodyTransform transformToWorldFromIntersectingPlanarRegion = new RigidBodyTransform();

         for (PlanarRegion planarRegionIntersectingSnappedAndWiggledPolygon : planarRegionsIntersectingSnappedAndWiggledPolygon)
         {
            intersectionsInPlaneFrameToPack.clear();

            planarRegionIntersectingSnappedAndWiggledPolygon.getTransformToWorld(transformToWorldFromIntersectingPlanarRegion);
            planarRegionIntersectingSnappedAndWiggledPolygon.getPolygonIntersectionsWhenProjectedVertically(footPolygonInWorld,
                                                                                                            intersectionsInPlaneFrameToPack);

            // If any points are above the plane of the planarRegionToPack, then this is stepping into a v type problem.
            for (ConvexPolygon2D intersectionPolygon : intersectionsInPlaneFrameToPack)
            {
               int numberOfVertices = intersectionPolygon.getNumberOfVertices();
               for (int i = 0; i < numberOfVertices; i++)
               {
                  Point2DReadOnly vertex2d = intersectionPolygon.getVertex(i);
                  Point3D vertex3dInWorld = new Point3D(vertex2d.getX(), vertex2d.getY(), 0.0);
                  transformToWorldFromIntersectingPlanarRegion.transform(vertex3dInWorld);
                  double planeZGivenXY = highestElevationPlanarRegion.getPlaneZGivenXY(vertex3dInWorld.getX(), vertex3dInWorld.getY());

                  double zPenetration = vertex3dInWorld.getZ() - planeZGivenXY;

                  if (zPenetration > maximumZPenetrationOnValleyRegions)
                  {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   /**
    * Clears snapper history
    */
   public void reset()
   {
      snapDataHolder.clear();
   }
}
