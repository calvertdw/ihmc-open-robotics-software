package us.ihmc.footstepPlanning.ui.components;

import javafx.beans.property.Property;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlanningParameters;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.robotEnvironmentAwareness.ui.properties.ParametersProperty;

import java.util.concurrent.atomic.AtomicReference;

public class FootstepPlannerParametersProperty extends ParametersProperty<SettableFootstepPlannerParameters>
{
   private DoubleField idealFootstepWidth = new DoubleField(SettableFootstepPlannerParameters::getIdealFootstepWidth, SettableFootstepPlannerParameters::setIdealFootstepWidth);
   private DoubleField idealFootstepLength = new DoubleField(SettableFootstepPlannerParameters::getIdealFootstepLength, SettableFootstepPlannerParameters::setIdealFootstepLength);
   private DoubleField maxStepReach = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepReach, SettableFootstepPlannerParameters::setMaximumStepReach);
   private DoubleField maxStepYaw = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepYaw, SettableFootstepPlannerParameters::setMaximumStepYaw);
   private DoubleField minStepWidth = new DoubleField(SettableFootstepPlannerParameters::getMinimumStepWidth, SettableFootstepPlannerParameters::setMinimumStepWidth);
   private DoubleField minStepLength = new DoubleField(SettableFootstepPlannerParameters::getMinimumStepLength, SettableFootstepPlannerParameters::setMinimumStepLength);
   private DoubleField minStepYaw = new DoubleField(SettableFootstepPlannerParameters::getMinimumStepYaw, SettableFootstepPlannerParameters::setMinimumStepYaw);
   private DoubleField maxStepZ = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepZ, SettableFootstepPlannerParameters::setMaximumStepZ);
   private DoubleField minFootholdPercent = new DoubleField(SettableFootstepPlannerParameters::getMinimumFootholdPercent, SettableFootstepPlannerParameters::setMinimumFootholdPercent);
   private DoubleField minSurfaceIncline = new DoubleField(SettableFootstepPlannerParameters::getMinimumSurfaceInclineRadians, SettableFootstepPlannerParameters::setMinimumSurfaceInclineRadians);
   private DoubleField maxStepWidth = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepWidth, SettableFootstepPlannerParameters::setMaximumStepWidth);
   private DoubleField minXClearanceFromStance = new DoubleField(SettableFootstepPlannerParameters::getMinXClearanceFromStance, SettableFootstepPlannerParameters::setMinXClearanceFromStance);
   private DoubleField minYClearanceFromStance = new DoubleField(SettableFootstepPlannerParameters::getMinYClearanceFromStance, SettableFootstepPlannerParameters::setMinYClearanceFromStance);

   private BooleanField returnBestEffortPlan = new BooleanField(SettableFootstepPlannerParameters::getReturnBestEffortPlan, SettableFootstepPlannerParameters::setReturnBestEffortPlan);
   private BooleanField useQuadraticDistanceCost = new BooleanField(SettableFootstepPlannerParameters::useQuadraticDistanceCost, SettableFootstepPlannerParameters::setUseQuadraticDistanceCost);
   private BooleanField useQuadraticHeightCost = new BooleanField(SettableFootstepPlannerParameters::useQuadraticHeightCost, SettableFootstepPlannerParameters::setUseQuadraticHeightCost);
   private DoubleField yawWeight = new DoubleField(SettableFootstepPlannerParameters::getYawWeight, SettableFootstepPlannerParameters::setYawWeight);
   private DoubleField pitchWeight = new DoubleField(SettableFootstepPlannerParameters::getPitchWeight, SettableFootstepPlannerParameters::setPitchWeight);
   private DoubleField rollWeight = new DoubleField(SettableFootstepPlannerParameters::getRollWeight, SettableFootstepPlannerParameters::setRollWeight);
   private DoubleField forwardWeight = new DoubleField(SettableFootstepPlannerParameters::getForwardWeight, SettableFootstepPlannerParameters::setForwardWeight);
   private DoubleField lateralWeight = new DoubleField(SettableFootstepPlannerParameters::getLateralWeight, SettableFootstepPlannerParameters::setLateralWeight);
   private DoubleField stepUpWeight = new DoubleField(SettableFootstepPlannerParameters::getStepUpWeight, SettableFootstepPlannerParameters::setStepUpWeight);
   private DoubleField stepDownWeight = new DoubleField(SettableFootstepPlannerParameters::getStepDownWeight, SettableFootstepPlannerParameters::setStepDownWeight);
   private DoubleField costPerStep = new DoubleField(SettableFootstepPlannerParameters::getCostPerStep, SettableFootstepPlannerParameters::setCostPerStep);
   private DoubleField aStarHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getAStarHeuristicsWeight, SettableFootstepPlannerParameters::setAStarHeuristicsWeight);
   private DoubleField visGraphWithAStarHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getVisGraphWithAStarHeuristicsWeight, SettableFootstepPlannerParameters::setVisGraphWithAStarHeuristicsWeight);
   private DoubleField depthFirstHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getDepthFirstHeuristicsWeight, SettableFootstepPlannerParameters::setDepthFirstHeuristicsWeight);
   private DoubleField bodyPathBasedHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getBodyPathBasedHeuristicsWeight, SettableFootstepPlannerParameters::setBodyPathBasedHeuristicsWeight);

   private BooleanField checkForBodyBoxCollision = new BooleanField(SettableFootstepPlannerParameters::checkForBodyBoxCollisions, SettableFootstepPlannerParameters::setCheckForBodyBoxCollisions);
   private BooleanField performHeuristicSearchPolicies = new BooleanField(SettableFootstepPlannerParameters::performHeuristicSearchPolicies, SettableFootstepPlannerParameters::setPerformHeuristicSearchPolicies);
   private DoubleField bodyBoxWidth = new DoubleField(SettableFootstepPlannerParameters::getBodyBoxWidth, SettableFootstepPlannerParameters::setBodyBoxWidth);
   private DoubleField bodyBoxDepth = new DoubleField(SettableFootstepPlannerParameters::getBodyBoxDepth, SettableFootstepPlannerParameters::setBodyBoxDepth);
   private DoubleField bodyBoxHeight = new DoubleField(SettableFootstepPlannerParameters::getBodyBoxHeight, SettableFootstepPlannerParameters::setBodyBoxHeight);
   private DoubleField bodyBoxBaseX = new DoubleField(SettableFootstepPlannerParameters::getBodyBoxBaseX, SettableFootstepPlannerParameters::setBodyBoxBaseX);
   private DoubleField bodyBoxBaseY = new DoubleField(SettableFootstepPlannerParameters::getBodyBoxBaseY, SettableFootstepPlannerParameters::setBodyBoxBaseY);
   private DoubleField bodyBoxBaseZ = new DoubleField(SettableFootstepPlannerParameters::getBodyBoxBaseZ, SettableFootstepPlannerParameters::setBodyBoxBaseZ);

   public FootstepPlannerParametersProperty(Object bean, String name)
   {
      this(bean, name, new DefaultFootstepPlanningParameters());
   }

   public FootstepPlannerParametersProperty(Object bean, String name, FootstepPlannerParameters footstepPlannerParameters)
   {
      super(bean, name, new SettableFootstepPlannerParameters(footstepPlannerParameters));
   }

   public void setPlannerParameters(FootstepPlannerParameters parameters)
   {
      setValue(new SettableFootstepPlannerParameters(parameters));
   }

   @Override
   protected SettableFootstepPlannerParameters getValueCopy(SettableFootstepPlannerParameters valueToCopy)
   {
      return new SettableFootstepPlannerParameters(valueToCopy);
   }

   public void bidirectionalBindIdealFootstepWidth(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, idealFootstepWidth);
   }

   public void bidirectionalBindIdealFootstepLength(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, idealFootstepLength);
   }

   public void bidirectionalBindReturnBestEffortPlan(Property<Boolean> property)
   {
      bindFieldBidirectionalToBooleanProperty(property, returnBestEffortPlan);
   }

   public void bidirectionalBindMaxStepReach(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, maxStepReach);
   }

   public void bidirectionalBindMaxStepYaw(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, maxStepYaw);
   }

   public void bidirectionalBindMinStepWidth(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, minStepWidth);
   }

   public void bidirectionalBindMinStepLength(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, minStepLength);
   }

   public void bidirectionalBindMinStepYaw(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, minStepYaw);
   }

   public void bidirectionalBindMaxStepZ(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, maxStepZ);
   }

   public void bidirectionalBindMinFootholdPercent(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, minFootholdPercent);
   }

   public void bidirectionalBindMinSurfaceIncline(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, minSurfaceIncline);
   }

   public void bidirectionalBindMaxStepWidth(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, maxStepWidth);
   }

   public void bidirectionalBindMinXClearanceFromStance(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, minXClearanceFromStance);
   }

   public void bidirectionalBindMinYClearanceFromStance(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, minYClearanceFromStance);
   }


   public void bidirectionalBindCheckBodyBoxCollisions(Property<Boolean> property)
   {
      bindFieldBidirectionalToBooleanProperty(property, checkForBodyBoxCollision);
   }

   public void bidirectionalBindPerformHeuristicSearchPolicies(Property<Boolean> property)
   {
      bindFieldBidirectionalToBooleanProperty(property, performHeuristicSearchPolicies);
   }

   public void bidirectionalBindBodyBoxWidth(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, bodyBoxWidth);
   }

   public void bidirectionalBindBodyBoxDepth(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, bodyBoxDepth);
   }

   public void bidirectionalBindBodyBoxHeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, bodyBoxHeight);
   }

   public void bidirectionalBindBodyBoxBaseX(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, bodyBoxBaseX);
   }

   public void bidirectionalBindBodyBoxBaseY(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, bodyBoxBaseY);
   }

   public void bidirectionalBindBodyBoxBaseZ(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, bodyBoxBaseZ);
   }

   public void bidirectionalBindYawWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, yawWeight);
   }

   public void bidirectionalBindPitchWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, pitchWeight);
   }

   public void bidirectionalBindRollWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, rollWeight);
   }

   public void bidirectionalBindForwardWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, forwardWeight);
   }

   public void bidirectionalBindLateralWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, lateralWeight);
   }

   public void bidirectionalBindStepUpWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, stepUpWeight);
   }

   public void bidirectionalBindStepDownWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, stepDownWeight);
   }

   public void bidirectionalBindCostPerStep(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, costPerStep);
   }

   public void bidirectionalBindHeuristicsWeight(AtomicReference<FootstepPlannerType> plannerTypeReference, Property<? extends Number> property)
   {
      if (plannerTypeReference.get() == null)
         return;

      bindFieldBidirectionalToConditionalNumberProperty(() -> plannerTypeReference.get().equals(FootstepPlannerType.A_STAR), property, aStarHeuristicsWeight);
      bindFieldBidirectionalToConditionalNumberProperty(() -> plannerTypeReference.get().equals(FootstepPlannerType.VIS_GRAPH_WITH_A_STAR), property, visGraphWithAStarHeuristicsWeight);
      bindFieldBidirectionalToConditionalNumberProperty(() -> plannerTypeReference.get().equals(FootstepPlannerType.PLANAR_REGION_BIPEDAL), property, depthFirstHeuristicsWeight);
      bindFieldBidirectionalToConditionalNumberProperty(() -> plannerTypeReference.get().equals(FootstepPlannerType.SIMPLE_BODY_PATH), property, bodyPathBasedHeuristicsWeight);
   }

   public void bidirectionalBindUseQuadraticHeightCost(Property<Boolean> property)
   {
      bindFieldBidirectionalToBooleanProperty(property, useQuadraticHeightCost);
   }

   public void bidirectionalBindUseQuadraticDistanceCost(Property<Boolean> property)
   {
      bindFieldBidirectionalToBooleanProperty(property, useQuadraticDistanceCost);
   }
}
