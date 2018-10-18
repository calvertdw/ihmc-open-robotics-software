package us.ihmc.footstepPlanning.ui.components;

import javafx.beans.property.Property;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlanningParameters;
import us.ihmc.javaFXToolkit.messager.TopicListener;
import us.ihmc.robotEnvironmentAwareness.ui.properties.ParametersProperty;

import java.util.concurrent.atomic.AtomicReference;

public class FootstepPlannerParametersProperty extends ParametersProperty<SettableFootstepPlannerParameters>
{
   private DoubleField idealFootstepWidth = new DoubleField(SettableFootstepPlannerParameters::getIdealFootstepWidth, (p, v) -> p.setIdealFootstepWidth(v));
   private DoubleField idealFootstepLength = new DoubleField(SettableFootstepPlannerParameters::getIdealFootstepLength, (p, v) -> p.setIdealFootstepLength(v));
   private DoubleField maxStepReach = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepReach, (p, v) -> p.setMaximumStepReach(v));
   private DoubleField maxStepYaw = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepYaw, (p, v) -> p.setMaximumStepYaw(v));
   private DoubleField minStepWidth = new DoubleField(SettableFootstepPlannerParameters::getMinimumStepWidth, (p, v) -> p.setMinimumStepWidth(v));
   private DoubleField minStepLength = new DoubleField(SettableFootstepPlannerParameters::getMinimumStepLength, (p, v) -> p.setMinimumStepLength(v));
   private DoubleField minStepYaw = new DoubleField(SettableFootstepPlannerParameters::getMinimumStepYaw, (p, v) -> p.setMinimumStepYaw(v));
   private DoubleField maxStepZ = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepZ, (p, v) -> p.setMaximumStepZ(v));
   private DoubleField minFootholdPercent = new DoubleField(SettableFootstepPlannerParameters::getMinimumFootholdPercent, (p, v) -> p.setMinimumFootholdPercent(v));
   private DoubleField minSurfaceIncline = new DoubleField(SettableFootstepPlannerParameters::getMinimumSurfaceInclineRadians, (p, v) -> p.setMinimumSurfaceInclineRadians(v));
   private DoubleField maxStepWidth = new DoubleField(SettableFootstepPlannerParameters::getMaximumStepWidth, (p, v) -> p.setMaximumStepWidth(v));

   private BooleanField useQuadraticDistanceCost = new BooleanField(SettableFootstepPlannerParameters::useQuadraticDistanceCost, (p, v) -> p.setUseQuadraticDistanceCost(v));
   private BooleanField useQuadraticHeightCost = new BooleanField(SettableFootstepPlannerParameters::useQuadraticHeightCost, (p, v) -> p.setUseQuadraticHeightCost(v));
   private DoubleField yawWeight = new DoubleField(SettableFootstepPlannerParameters::getYawWeight, (p, v) -> p.setYawWeight(v));
   private DoubleField pitchWeight = new DoubleField(SettableFootstepPlannerParameters::getPitchWeight, (p, v) -> p.setPitchWeight(v));
   private DoubleField rollWeight = new DoubleField(SettableFootstepPlannerParameters::getRollWeight, (p, v) -> p.setRollWeight(v));
   private DoubleField forwardWeight = new DoubleField(SettableFootstepPlannerParameters::getForwardWeight, (p, v) -> p.setForwardWeight(v));
   private DoubleField lateralWeight = new DoubleField(SettableFootstepPlannerParameters::getLateralWeight, (p, v) -> p.setLateralWeight(v));
   private DoubleField stepUpWeight = new DoubleField(SettableFootstepPlannerParameters::getStepUpWeight, (p, v) -> p.setStepUpWeight(v));
   private DoubleField stepDownWeight = new DoubleField(SettableFootstepPlannerParameters::getStepDownWeight, (p, v) -> p.setStepDownWeight(v));
   private DoubleField costPerStep = new DoubleField(SettableFootstepPlannerParameters::getCostPerStep, (p, v) -> p.setCostPerStep(v));
   private DoubleField aStarHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getAStarHeuristicsWeight, (p, v) -> p.setAStarHeuristicsWeight(v));
   private DoubleField visGraphWithAStarHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getVisGraphWithAStarHeuristicsWeight, (p, v) -> p.setVisGraphWithAStarHeuristicsWeight(v));
   private DoubleField depthFirstHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getDepthFirstHeuristicsWeight, (p, v) -> p.setDepthFirstHeuristicsWeight(v));
   private DoubleField bodyPathBasedHeuristicsWeight = new DoubleField(SettableFootstepPlannerParameters::getBodyPathBasedHeuristicsWeight, (p, v) -> p.setBodyPathBasedHeuristicsWeight(v));

   public FootstepPlannerParametersProperty(Object bean, String name)
   {
      super(bean, name, new SettableFootstepPlannerParameters(new DefaultFootstepPlanningParameters()));
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

   public void bidirectionalBindYawWeight(Property<? extends Number> property)
   {
      bindFieldBidirectionalToNumberProperty(property, yawWeight);
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


}
