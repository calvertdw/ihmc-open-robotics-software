package us.ihmc.commonWalkingControlModules.messageHandlers;

import us.ihmc.communication.controllerAPI.RequestMessageOutputManager;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.RequestPlanarRegionsListMessage;
import us.ihmc.communication.packets.RequestPlanarRegionsListMessage.RequestType;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PlanarRegionsListCommand;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoInteger;

public class PlanarRegionsListHandler
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private static final int maxNumberOfPlanarRegions = 100;
   private final YoBoolean hasNewPlanarRegionsList = new YoBoolean("hasNewPlanarRegionsList", registry);
   private final YoInteger currentNumberOfPlanarRegions = new YoInteger("currentNumberOfPlanarRegions", registry);
   private final RecyclingArrayList<PlanarRegion> planarRegions = new RecyclingArrayList<>(maxNumberOfPlanarRegions, PlanarRegion.class);

   private final YoBoolean waitingOnNewPlanarRegions = new YoBoolean("waitingOnNewPlanarRegions", registry);

   private final RequestMessageOutputManager requestOutputManager;
   private final RequestPlanarRegionsListMessage planarRegionsRequestMessage = new RequestPlanarRegionsListMessage(RequestType.SINGLE_UPDATE);

   public PlanarRegionsListHandler(RequestMessageOutputManager requestOutputManager, YoVariableRegistry parentRegistry)
   {
      this.requestOutputManager = requestOutputManager;

      planarRegions.clear();
      planarRegionsRequestMessage.setDestination(PacketDestination.CONTROLLER);

      parentRegistry.addChild(registry);
   }

   public void handlePlanarRegionsListCommand(PlanarRegionsListCommand planarRegionsListCommand)
   {
      for (int i = 0; i < planarRegionsListCommand.getNumberOfPlanarRegions(); i++)
      {
         planarRegionsListCommand.getPlanarRegionCommand(i).getPlanarRegion(planarRegions.add());
         currentNumberOfPlanarRegions.increment();
      }

      hasNewPlanarRegionsList.set(true);
      waitingOnNewPlanarRegions.set(false);
   }

   public void requestPlanarRegions()
   {
      requestOutputManager.reportRequestMessage(planarRegionsRequestMessage);
      waitingOnNewPlanarRegions.set(true);
   }

   public boolean pollHasNewPlanarRegionsList(PlanarRegionsList planarRegionsListToPack)
   {
      if (!hasNewPlanarRegionsList.getBooleanValue())
         return false;

      planarRegionsListToPack.clear();
      for (int i = 0; i <planarRegions.size(); i++)
         planarRegionsListToPack.addPlanarRegion(planarRegions.get(i));

      hasNewPlanarRegionsList.set(false);
      planarRegions.clear();

      return true;
   }
}
