package us.ihmc.robotiq.communication;

import static us.ihmc.robotics.Assert.*;

import org.junit.jupiter.api.Test;

import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HandConfiguration;
import us.ihmc.robotiq.RobotiqGraspMode;

public class RobotiqWriteRequestFactoryTest
{
   @Test
   public void testOpenMessage()
   {
      Register[] request = new Register[8];
      request[0] = new SimpleRegister((byte)9, (byte)12);
      request[1] = new SimpleRegister((byte)0, (byte)0);
      request[2] = new SimpleRegister((byte)0xFF, (byte)0xFF);
      request[3] = new SimpleRegister((byte)0, (byte)0xFF);
      request[4] = new SimpleRegister((byte)0xFF, (byte)0);
      request[5] = new SimpleRegister((byte)0xFF, (byte)0xFF);
      request[6] = new SimpleRegister((byte)0x8C, (byte)0xFF);
      request[7] = new SimpleRegister((byte)0xFF, (byte)0);
      
      RobotiqWriteRequestFactory writeRequestFactory = new RobotiqWriteRequestFactory();
      writeRequestFactory.createActivationRequest();
      Register[] factoryRequest = writeRequestFactory.createWholeHandPositionRequest(RobotiqGraspMode.BASIC_MODE, HandConfiguration.OPEN);
      
      assertEquals(request.length, factoryRequest.length);
      
      for(int i = 0; i < request.length; i++)
      {
         byte[] requestBytes = request[i].toBytes();
         byte[] factoryRequestBytes = factoryRequest[i].toBytes();
         
         assertEquals(requestBytes.length, factoryRequestBytes.length);
         
         for(int j = 0; j < requestBytes.length; j++)
         {
            assertEquals(requestBytes[j], factoryRequestBytes[j]);
         }
      }
   }
}
