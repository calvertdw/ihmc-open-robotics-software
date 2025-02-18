package us.ihmc.footstepPlanning.graphSearch.stepExpansion;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.footstepPlanning.PlannedFootstep;
import us.ihmc.footstepPlanning.graphSearch.graph.DiscreteFootstep;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepGraphNode;
import us.ihmc.footstepPlanning.graphSearch.graph.LatticePoint;

import java.util.List;

public class ReferenceBasedStepExpansion implements FootstepExpansion
{
   private final ReferenceBasedIdealStepCalculator idealStepCalculator;
   private final FootstepExpansion nominalExpansion;

   private final TIntArrayList referenceXOffsets = new TIntArrayList();
   private final TIntArrayList referenceYOffsets = new TIntArrayList();
   private final TIntArrayList referenceYawOffsets = new TIntArrayList();

   public ReferenceBasedStepExpansion(ReferenceBasedIdealStepCalculator idealStepCalculator, FootstepExpansion nominalExpansion)
   {
      this.idealStepCalculator = idealStepCalculator;
      this.nominalExpansion = nominalExpansion;

      // Reference Offsets
      referenceXOffsets.add(-1);
      referenceXOffsets.add(0);
      referenceXOffsets.add(1);

      referenceYOffsets.add(-1);
      referenceYOffsets.add(0);
      referenceYOffsets.add(1);

      referenceYawOffsets.add(-1);
      referenceYawOffsets.add(0);
      referenceYawOffsets.add(1);
   }

   @Override
   public void doFullExpansion(FootstepGraphNode nodeToExpand, List<FootstepGraphNode> expansionToPack)
   {
      PlannedFootstep referenceStep = idealStepCalculator.getReferenceStep(nodeToExpand);
      if (referenceStep == null)
      {
         nominalExpansion.doFullExpansion(nodeToExpand, expansionToPack);
      }
      else
      {
         expansionToPack.clear();

         DiscreteFootstep referenceDiscreteFootstep = new DiscreteFootstep(referenceStep.getFootstepPose().getX(),
                                                                           referenceStep.getFootstepPose().getY(),
                                                                           referenceStep.getFootstepPose().getYaw(),
                                                                           referenceStep.getRobotSide());

         int referenceX = referenceDiscreteFootstep.getXIndex();
         int referenceY = referenceDiscreteFootstep.getYIndex();
         int referenceYaw = referenceDiscreteFootstep.getYawIndex();

         for (int xi = 0; xi < referenceXOffsets.size(); xi++)
         {
            for (int yi = 0; yi < referenceYOffsets.size(); yi++)
            {
               for (int yawi = 0; yawi < referenceYawOffsets.size(); yawi++)
               {
                  int xOffset = referenceXOffsets.get(xi);
                  int yOffset = referenceYOffsets.get(yi);
                  int yawOffset = referenceYawOffsets.get(yawi);

                  // hack to get offset in the correct range...
                  int yawIndex = referenceYaw + yawOffset;
                  yawIndex = yawIndex % LatticePoint.yawDivisions;
                  if (yawIndex < 0)
                     yawIndex += LatticePoint.yawDivisions;

                  DiscreteFootstep footstep = new DiscreteFootstep(referenceX + xOffset,
                                                                   referenceY + yOffset, yawIndex,
                                                                   referenceDiscreteFootstep.getRobotSide());
                  expansionToPack.add(new FootstepGraphNode(nodeToExpand.getSecondStep(), footstep));
               }
            }
         }
      }
   }
}
