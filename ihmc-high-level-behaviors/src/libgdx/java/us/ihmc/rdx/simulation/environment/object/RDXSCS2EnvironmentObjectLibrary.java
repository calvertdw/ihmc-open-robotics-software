package us.ihmc.rdx.simulation.environment.object;

import us.ihmc.rdx.simulation.environment.object.objects.*;

import java.util.ArrayList;

public class RDXSCS2EnvironmentObjectLibrary
{
   private static final ArrayList<RDXSCS2EnvironmentObjectFactory> objectFactories = new ArrayList<>();
   static
   {
      objectFactories.add(RDXSCS2SmallCinderBlockRoughed.FACTORY);
//      objectFactories.add(RDXMediumCinderBlockRoughed.FACTORY);
//      objectFactories.add(RDXLargeCinderBlockRoughed.FACTORY);
//      objectFactories.add(RDXLabFloorObject.FACTORY);
//      objectFactories.add(RDXPalletObject.FACTORY);
//      objectFactories.add(RDXStairsObject.FACTORY);
//      objectFactories.add(RDXDoorFrameObject.FACTORY);
//      objectFactories.add(RDXDoorPanelObject.FACTORY);
//      objectFactories.add(RDXDoorLeverHandleObject.FACTORY);
//      objectFactories.add(RDXDoorObject.FACTORY);
//      objectFactories.add(RDXPointLightObject.FACTORY);
//      objectFactories.add(RDXDirectionalLightObject.FACTORY);
//      objectFactories.add(RDXL515SensorObject.FACTORY);
//      objectFactories.add(RDXMultiBodySnakeObject.FACTORY);
   }

   public static ArrayList<RDXSCS2EnvironmentObjectFactory> getObjectFactories()
   {
      return objectFactories;
   }

   public static RDXSCS2EnvironmentObject loadBySimpleClassName(String objectClassName)
   {
      for (RDXSCS2EnvironmentObjectFactory objectFactory : objectFactories)
      {
         if (objectFactory.getClazz().getSimpleName().equals(objectClassName))
         {
            return objectFactory.getSupplier().get();
         }
      }

      throw new RuntimeException("Library does not contain object of the name: " + objectClassName);
   }
}
