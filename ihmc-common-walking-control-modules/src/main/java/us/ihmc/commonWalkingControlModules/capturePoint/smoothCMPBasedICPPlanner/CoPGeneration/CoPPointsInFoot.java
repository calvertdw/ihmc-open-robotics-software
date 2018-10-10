package us.ihmc.commonWalkingControlModules.capturePoint.smoothCMPBasedICPPlanner.CoPGeneration;

import us.ihmc.commonWalkingControlModules.configurations.CoPPointName;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.robotics.math.frames.YoFramePointInMultipleFrames;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFramePoint3D;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CoPPointsInFoot
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final int maxNumberOfTrajectoryPoints = 10;

   private final FrameVector3D zeroVector = new FrameVector3D();
   private final FrameEuclideanTrajectoryPoint tempVariableForSetting = new FrameEuclideanTrajectoryPoint();

   private final List<CoPPointName> copPointsList = new ArrayList<>(maxNumberOfTrajectoryPoints); // List of CoP way points defined for this footstep.

   private final RecyclingArrayList<CoPTrajectoryPoint> copLocations; // Location of CoP points defined

   private final YoFramePointInMultipleFrames swingFootCentroid;
   private final YoFramePointInMultipleFrames supportFootCentroid;
   private final String name;

   public CoPPointsInFoot(String namePrefix, int stepNumber, ReferenceFrame[] framesToRegister, YoVariableRegistry registry)
   {
      if (registry == null)
         registry = new YoVariableRegistry("localRegistry");

      this.name = namePrefix + "Step" + stepNumber;

      copLocations = new RecyclingArrayList<>(maxNumberOfTrajectoryPoints, new CoPTrajectoryPointSupplier(framesToRegister, registry));
      copLocations.clear();
      pointsConstructed = true;

      swingFootCentroid = new YoFramePointInMultipleFrames(name + "SwingCentroid", registry, framesToRegister);
      supportFootCentroid = new YoFramePointInMultipleFrames(name + "SupportCentroid", registry, framesToRegister);
   }

   private int pointNumber = 0;
   private boolean pointsConstructed = false;

   private class CoPTrajectoryPointSupplier implements Supplier<CoPTrajectoryPoint>
   {
      private final ReferenceFrame[] framesToRegister;
      private final YoVariableRegistry registry;

      public CoPTrajectoryPointSupplier(ReferenceFrame[] framesToRegister, YoVariableRegistry registry)
      {
         this.framesToRegister = framesToRegister;
         this.registry = registry;
      }

      public CoPTrajectoryPoint get()
      {
         CoPTrajectoryPoint copPoint;
         if (!pointsConstructed)
         {
            copPoint = new CoPTrajectoryPoint(name + "CoP" + pointNumber, "", registry, framesToRegister);
         }
         else
         {
            copPoint = new CoPTrajectoryPoint(name + "CoP" + pointNumber, "", null, framesToRegister);
         }
         copPoint.setToNaN();
         pointNumber++;
         return copPoint;
      }
   }

   public void setupVisualizers(YoGraphicsList graphicsList, ArtifactList artifactList, double pointSize)
   {
      for (int i = 0; i < maxNumberOfTrajectoryPoints; i++)
      {
         YoFramePoint3D copLocation = copLocations.getAndGrowIfNeeded(i).getPosition();
         YoGraphicPosition yoGraphicPosition = new YoGraphicPosition(copLocation.getNamePrefix(), copLocation, pointSize, YoAppearance.Green(),
                                                                     YoGraphicPosition.GraphicType.BALL_WITH_CROSS);
         graphicsList.add(yoGraphicPosition);
         artifactList.add(yoGraphicPosition.createArtifact());
      }
      copLocations.clear();
   }

   public void notifyVariableChangedListeners()
   {
      for (int i = 0; i < copLocations.size(); i++)
         copLocations.get(i).getPosition().notifyVariableChangedListeners();
   }

   public void reset()
   {
      swingFootCentroid.setToNaN();
      supportFootCentroid.setToNaN();
      copPointsList.clear();
      for (int i = 0; i < copLocations.size(); i++)
      {
         copLocations.get(i).setToNaN(worldFrame);
      }
      copLocations.clear();
   }

   public void addWaypoint(CoPPointName copPointName, double time, FramePoint3DReadOnly location)
   {
      zeroVector.setToZero(location.getReferenceFrame());
      tempVariableForSetting.setIncludingFrame(time, location, zeroVector);

      CoPTrajectoryPoint trajectoryPoint = copLocations.add();
      trajectoryPoint.registerReferenceFrame(location.getReferenceFrame());
      trajectoryPoint.setIncludingFrame(tempVariableForSetting);
      trajectoryPoint.changeFrame(worldFrame);

      addWayPointName(copPointName);
   }

   void addWaypoint(CoPPointName copPointName, double time, CoPTrajectoryPoint location)
   {
      tempVariableForSetting.setIncludingFrame(time, location.getPosition(), location.getLinearVelocity());

      CoPTrajectoryPoint trajectoryPoint = copLocations.add();
      trajectoryPoint.registerReferenceFrame(location.getReferenceFrame());
      trajectoryPoint.setIncludingFrame(tempVariableForSetting);
      addWayPointName(copPointName);
   }

   private void addWayPointName(CoPPointName copPointName)
   {
      this.copPointsList.add(copPointName);
   }

   public void set(CoPPointsInFoot other)
   {
      this.swingFootCentroid.setIncludingFrame(other.swingFootCentroid);
      this.supportFootCentroid.setIncludingFrame(other.supportFootCentroid);
      this.copPointsList.clear();
      this.copLocations.clear();
      for (int index = 0; index < other.copPointsList.size(); index++)
      {
         this.copPointsList.add(other.copPointsList.get(index));
         this.copLocations.add().setIncludingFrame(other.get(index));
      }
   }

   public void setToNaN(int waypointIndex)
   {
      copLocations.get(waypointIndex).setToNaN();
   }

   public CoPTrajectoryPoint get(int copPointIndex)
   {
      return copLocations.get(copPointIndex);
   }

   public FramePoint3DReadOnly getWaypointInWorld(int copPointIndex)
   {
      copLocations.get(copPointIndex).checkReferenceFrameMatch(worldFrame);
      return copLocations.get(copPointIndex).getPosition();
   }

   public void changeFrame(ReferenceFrame desiredFrame)
   {
      swingFootCentroid.changeFrame(desiredFrame);
      supportFootCentroid.changeFrame(desiredFrame);
      for (int i = 0; i < copLocations.size(); i++)
         copLocations.get(i).changeFrame(desiredFrame);
   }

   public void registerReferenceFrame(ReferenceFrame newReferenceFrame)
   {
      swingFootCentroid.registerReferenceFrame(newReferenceFrame);
      supportFootCentroid.registerReferenceFrame(newReferenceFrame);
      for (int i = 0; i < copLocations.size(); i++)
         copLocations.get(i).registerReferenceFrame(newReferenceFrame);
   }

   public void setSwingFootLocation(FramePoint3DReadOnly footLocation)
   {
      this.swingFootCentroid.setIncludingFrame(footLocation);
   }

   public void getSwingFootLocation(FramePoint3D footLocationToPack)
   {
      footLocationToPack.setIncludingFrame(swingFootCentroid);
   }

   public void setSupportFootLocation(FramePoint3DReadOnly footLocation)
   {
      this.supportFootCentroid.setIncludingFrame(footLocation);
   }

   public void getSupportFootLocation(FramePoint3D footLocationToPack)
   {
      footLocationToPack.setIncludingFrame(supportFootCentroid);
   }

   public void setFeetLocation(FramePoint3DReadOnly swingFootLocation, FramePoint3DReadOnly supportFootLocation)
   {
      setSwingFootLocation(swingFootLocation);
      setSupportFootLocation(supportFootLocation);
   }

   public List<CoPPointName> getCoPPointList()
   {
      return copPointsList;
   }

   public void getFinalCoPPosition(FixedFramePoint3DBasics tempFinalICPToPack)
   {
      copLocations.getLast().getPosition(tempFinalICPToPack);
   }

   public boolean isEmpty()
   {
      return copLocations.isEmpty();
   }

   public int getNumberOfCoPPoints()
   {
      return copLocations.size();
   }

   public String toString()
   {
      String string = name;
      for (int i = 0; i < getNumberOfCoPPoints(); i++)
      {
         string += getCoPPointList().get(i).toString() + ": " + get(i).toString() + "\n";
      }
      return string;
   }

}
