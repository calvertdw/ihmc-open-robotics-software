package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import java.util.LinkedHashMap;
import java.util.List;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.mecano.multiBodySystem.OneDoFJoint;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.JointReadOnly;
import us.ihmc.robotics.screwTheory.ScrewTools;

public class JointIndexHandler
{
   private final TIntArrayList indicesIntoCompactBlock = new TIntArrayList();
   private final LinkedHashMap<JointReadOnly, int[]> columnsForJoints = new LinkedHashMap<>();

   private final int numberOfDoFs;
   private final JointBasics[] indexedJoints;
   private final OneDoFJoint[] indexedOneDoFJoints;

   public JointIndexHandler(JointBasics[] jointsToIndex)
   {
      indexedJoints = jointsToIndex;
      indexedOneDoFJoints = ScrewTools.filterJoints(indexedJoints, OneDoFJoint.class);

      numberOfDoFs = ScrewTools.computeDegreesOfFreedom(jointsToIndex);
      populateColumnIndices();
   }

   public JointIndexHandler(List<? extends JointBasics> jointsToIndex)
   {
      indexedJoints = new JointBasics[jointsToIndex.size()];
      jointsToIndex.toArray(indexedJoints);
      indexedOneDoFJoints = ScrewTools.filterJoints(indexedJoints, OneDoFJoint.class);

      numberOfDoFs = ScrewTools.computeDegreesOfFreedom(jointsToIndex);
      populateColumnIndices();
   }

   private void populateColumnIndices()
   {
      for (JointBasics joint : indexedJoints)
      {
         TIntArrayList listToPackIndices = new TIntArrayList();
         ScrewTools.computeIndexForJoint(indexedJoints, listToPackIndices, joint);
         int[] indices = listToPackIndices.toArray();

         columnsForJoints.put(joint, indices);
      }
   }

   public boolean compactBlockToFullBlock(JointReadOnly[] joints, DenseMatrix64F compactMatrix, DenseMatrix64F fullMatrix)
   {
      fullMatrix.zero();

      for (JointReadOnly joint : joints)
      {
         indicesIntoCompactBlock.reset();
         ScrewTools.computeIndexForJoint(joints, indicesIntoCompactBlock, joint);
         int[] indicesIntoFullBlock = columnsForJoints.get(joint);

         if (indicesIntoFullBlock == null) // don't do anything for joints that are not in the list
            return false;

         for (int i = 0; i < indicesIntoCompactBlock.size(); i++)
         {
            int compactBlockIndex = indicesIntoCompactBlock.get(i);
            int fullBlockIndex = indicesIntoFullBlock[i];
            CommonOps.extract(compactMatrix, 0, compactMatrix.getNumRows(), compactBlockIndex, compactBlockIndex + 1, fullMatrix, 0, fullBlockIndex);
         }
      }

      return true;
   }

   public boolean compactBlockToFullBlock(List<? extends JointReadOnly> joints, DenseMatrix64F compactMatrix, DenseMatrix64F fullMatrix)
   {
      fullMatrix.zero();

      for (int index = 0; index < joints.size(); index++)
      {
         JointReadOnly joint = joints.get(index);
         indicesIntoCompactBlock.reset();
         ScrewTools.computeIndexForJoint(joints, indicesIntoCompactBlock, joint);
         int[] indicesIntoFullBlock = columnsForJoints.get(joint);

         if (indicesIntoFullBlock == null) // don't do anything for joints that are not in the list
            return false;

         for (int i = 0; i < indicesIntoCompactBlock.size(); i++)
         {
            int compactBlockIndex = indicesIntoCompactBlock.get(i);
            int fullBlockIndex = indicesIntoFullBlock[i];
            CommonOps.extract(compactMatrix, 0, compactMatrix.getNumRows(), compactBlockIndex, compactBlockIndex + 1, fullMatrix, 0, fullBlockIndex);
         }
      }

      return true;
   }

   public void compactBlockToFullBlockIgnoreUnindexedJoints(List<? extends JointReadOnly> joints, DenseMatrix64F compactMatrix, DenseMatrix64F fullMatrix)
   {
      fullMatrix.reshape(compactMatrix.getNumRows(), fullMatrix.getNumCols());
      fullMatrix.zero();

      for (int index = 0; index < joints.size(); index++)
      {
         JointReadOnly joint = joints.get(index);
         indicesIntoCompactBlock.reset();
         ScrewTools.computeIndexForJoint(joints, indicesIntoCompactBlock, joint);
         int[] indicesIntoFullBlock = columnsForJoints.get(joint);

         if (indicesIntoFullBlock == null) // don't do anything for joints that are not in the list
            continue;

         for (int i = 0; i < indicesIntoCompactBlock.size(); i++)
         {
            int compactBlockIndex = indicesIntoCompactBlock.get(i);
            int fullBlockIndex = indicesIntoFullBlock[i];
            CommonOps.extract(compactMatrix, 0, compactMatrix.getNumRows(), compactBlockIndex, compactBlockIndex + 1, fullMatrix, 0, fullBlockIndex);
         }
      }
   }

   public void compactBlockToFullBlockIgnoreUnindexedJoints(JointBasics[] joints, DenseMatrix64F compactMatrix, DenseMatrix64F fullMatrix)
   {
      fullMatrix.reshape(compactMatrix.getNumRows(), fullMatrix.getNumCols());
      fullMatrix.zero();

      for (int index = 0; index < joints.length; index++)
      {
         JointBasics joint = joints[index];
         indicesIntoCompactBlock.reset();
         ScrewTools.computeIndexForJoint(joints, indicesIntoCompactBlock, joint);
         int[] indicesIntoFullBlock = columnsForJoints.get(joint);

         if (indicesIntoFullBlock == null) // don't do anything for joints that are not in the list
            continue;

         for (int i = 0; i < indicesIntoCompactBlock.size(); i++)
         {
            int compactBlockIndex = indicesIntoCompactBlock.get(i);
            int fullBlockIndex = indicesIntoFullBlock[i];
            CommonOps.extract(compactMatrix, 0, compactMatrix.getNumRows(), compactBlockIndex, compactBlockIndex + 1, fullMatrix, 0, fullBlockIndex);
         }
      }
   }

   public JointBasics[] getIndexedJoints()
   {
      return indexedJoints;
   }

   public OneDoFJoint[] getIndexedOneDoFJoints()
   {
      return indexedOneDoFJoints;
   }

   public boolean isJointIndexed(JointBasics joint)
   {
      return columnsForJoints.containsKey(joint);
   }

   public boolean areJointsIndexed(JointBasics[] joints)
   {
      for (int i = 0; i < joints.length; i++)
      {
         if (!isJointIndexed(joints[i]))
            return false;
      }
      return true;
   }

   public int getOneDoFJointIndex(OneDoFJoint joint)
   {
      int[] jointIndices = columnsForJoints.get(joint);
      if (jointIndices == null)
         return -1;
      else
         return jointIndices[0];
   }

   public int[] getJointIndices(JointReadOnly joint)
   {
      return columnsForJoints.get(joint);
   }

   public int getNumberOfDoFs()
   {
      return numberOfDoFs;
   }
}
