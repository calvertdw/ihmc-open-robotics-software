package us.ihmc.humanoidRobotics.communication.packets.manipulation.constrainedWholeBodyPlanning;

public class NodeDataPacket extends NodeData
{
   public NodeDataPacket()
   {
      super(17);
   }
   
   public NodeDataPacket(NodeData nodeData)
   {
      super(nodeData);
   }
}
