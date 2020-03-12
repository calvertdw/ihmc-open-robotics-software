package us.ihmc.footstepPlanning.log;

import controller_msgs.msg.dds.*;
import us.ihmc.commons.nio.FileTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.tools.EuclidCoreIOTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlanningModule;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.tools.FootstepPlannerMessageTools;
import us.ihmc.idl.serializers.extra.JSONSerializer;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.pathPlanning.graph.structure.GraphEdge;
import us.ihmc.robotics.robotSide.RobotSide;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FootstepPlannerLogger
{
   private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
   private static final String logDirectory = System.getProperty("user.home") + File.separator + ".ihmc" + File.separator + "logs" + File.separator;

   static final String requestPacketFileName = "RequestPacket.json";
   static final String footstepParametersFileName = "FootstepParametersPacket.json";
   static final String bodyPathParametersFileName = "BodyPathParametersPacket.json";
   static final String statusPacketFileName = "StatusPacket.json";
   static final String dataFileName = "PlannerIterationData.log";

   private static final String iterationDataPrefix = "\t";
   private static final String edgeDataPrefix = "\t\t";

   private final FootstepPlanningModule planner;
   private final AtomicBoolean generatingLog = new AtomicBoolean();
   private String latestLogDirectory;
   private FileOutputStream outputStream = null;
   private PrintStream printStream = null;
   private FileWriter fileWriter = null;

   private final FootstepPlanningRequestPacket requestPacket = new FootstepPlanningRequestPacket();
   private final FootstepPlannerParametersPacket footstepParametersPacket = new FootstepPlannerParametersPacket();
   private final VisibilityGraphsParametersPacket bodyPathParametersPacket = new VisibilityGraphsParametersPacket();
   private final FootstepPlanningToolboxOutputStatus outputStatus = new FootstepPlanningToolboxOutputStatus();

   private final JSONSerializer<FootstepPlanningRequestPacket> requestPacketSerializer = new JSONSerializer<>(new FootstepPlanningRequestPacketPubSubType());
   private final JSONSerializer<FootstepPlannerParametersPacket> footstepParametersPacketSerializer = new JSONSerializer<>(new FootstepPlannerParametersPacketPubSubType());
   private final JSONSerializer<VisibilityGraphsParametersPacket> bodyPathParametersPacketSerializer = new JSONSerializer<>(new VisibilityGraphsParametersPacketPubSubType());
   private final JSONSerializer<FootstepPlanningToolboxOutputStatus> statusPacketSerializer = new JSONSerializer<>(new FootstepPlanningToolboxOutputStatusPubSubType());

   public FootstepPlannerLogger(FootstepPlanningModule planner)
   {
      this.planner = planner;
   }

   public void logSessionAndReportToMessager(Messager messager)
   {
      if(generatingLog.get())
         return;

      generatingLog.set(true);
      messager.submitMessage(FootstepPlannerMessagerAPI.GenerateLogStatus, "Writing log...");
      boolean success = logSession();

      String status = success ? latestLogDirectory : "Error writing log.";
      messager.submitMessage(FootstepPlannerMessagerAPI.GenerateLogStatus, status);

      generatingLog.set(false);
   }

   public boolean logSession()
   {
      String sessionDirectory = logDirectory + dateFormat.format(new Date()) + "_" + "FootstepPlannerLog" + File.separator;
      latestLogDirectory = sessionDirectory;

      try
      {
         // log request packet
         String requestPacketFile = sessionDirectory + requestPacketFileName;
         planner.getRequest().setPacket(requestPacket);
         byte[] serializedRequest = requestPacketSerializer.serializeToBytes(requestPacket);
         writeToFile(requestPacketFile, serializedRequest);

         // log footstep planner parameters packet
         String footstepParametersPacketFile = sessionDirectory + footstepParametersFileName;
         FootstepPlannerMessageTools.copyParametersToPacket(footstepParametersPacket, planner.getFootstepPlannerParameters());
         byte[] serializedFootstepParameters = footstepParametersPacketSerializer.serializeToBytes(footstepParametersPacket);
         writeToFile(footstepParametersPacketFile, serializedFootstepParameters);

         // log footstep planner parameters packet
         String bodyPathParametersPacketFile = sessionDirectory + bodyPathParametersFileName;
         FootstepPlannerMessageTools.copyParametersToPacket(bodyPathParametersPacket, planner.getVisibilityGraphParameters());
         byte[] serializedBodyPathParameters = bodyPathParametersPacketSerializer.serializeToBytes(bodyPathParametersPacket);
         writeToFile(bodyPathParametersPacketFile, serializedBodyPathParameters);

         // log status packet
         String statusPacketFile = sessionDirectory + statusPacketFileName;
         planner.getOutput().setPacket(outputStatus);
         byte[] serializedStatus = statusPacketSerializer.serializeToBytes(outputStatus);
         writeToFile(statusPacketFile, serializedStatus);
      }
      catch (Exception e)
      {
         LogTools.error("Error generating log");
         outputStream = null;
         printStream = null;
         e.printStackTrace();
         return false;
      }

      HashSet<GraphEdge<FootstepNode>> solutionEdges = new HashSet<>();
      List<FootstepNode> solutionPath = planner.getFootstepPlanner().getGraph().getPathFromStart(planner.getEndNode());
      for (int i = 0; i < solutionPath.size() - 1; i++)
      {
         solutionEdges.add(new GraphEdge<>(solutionPath.get(i), solutionPath.get(i + 1)));
      }

      // log planner iteration data
      String plannerIterationDataFileName = sessionDirectory + "PlannerIterationData.log";
      try
      {
         File plannerDataFile = new File(plannerIterationDataFileName);
         FileTools.ensureFileExists(plannerDataFile.toPath());
         fileWriter = new FileWriter(plannerIterationDataFileName);

         fileWriter.write(
               "edgeData:" + "rejectionReason, " + "footAreaPercentage, " + "stepWidth, " + "stepLength, " + "stepHeight, " + "stepReach, " + "costFromStart, "
               + "edgeCost, " + "heuristicCost," + "solutionEdge" + "\n");

         List<FootstepPlannerIterationData> iterationDataList = planner.getIterationData();
         for (int i = 0; i < iterationDataList.size(); i++)
         {
            FootstepPlannerIterationData iterationData = iterationDataList.get(i);
            fileWriter.write("Iteration " + i + "\n");
            writeNode(iterationDataPrefix + "stanceNode", iterationData.getStanceNode());
            writeNode(iterationDataPrefix + "idealStep", iterationData.getIdealStep());
            fileWriter.write(iterationDataPrefix + "edges:" + iterationData.getChildNodes().size() + "\n");
            writeSnapData(iterationDataPrefix, iterationData.getStanceNodeSnapData());

            for (int j = 0; j < iterationData.getChildNodes().size(); j++)
            {
               FootstepPlannerEdgeData edgeData = planner.getEdgeDataMap().get(new GraphEdge<>(iterationData.getStanceNode(), iterationData.getChildNodes().get(j)));

               // indicate start of data
               fileWriter.write(iterationDataPrefix + "Edge:\n");
               writeNode(edgeDataPrefix + "candidateNode", edgeData.getCandidateNode());
               writeSnapData(edgeDataPrefix, edgeData.getCandidateNodeSnapData());

               // write additional data as doubles
               boolean solutionEdge = solutionEdges.contains(new GraphEdge<>(iterationData.getStanceNode(), edgeData.getCandidateNode()));
               fileWriter.write(edgeDataPrefix + "edgeData:" + EuclidCoreIOTools.getStringOf(",",
                                                                                             EuclidCoreIOTools.getStringFormat(8, 8),
                                                                                             edgeData.getRejectionReason() == null ?
                                                                                                   -1.0 :
                                                                                                   (double) edgeData.getRejectionReason().ordinal(),
                                                                                             edgeData.getFootAreaPercentage(),
                                                                                             edgeData.getStepWidth(),
                                                                                             edgeData.getStepLength(),
                                                                                             edgeData.getStepHeight(),
                                                                                             edgeData.getStepReach(),
                                                                                             edgeData.getCostFromStart(),
                                                                                             edgeData.getEdgeCost(),
                                                                                             edgeData.getHeuristicCost(),
                                                                                             solutionEdge ? 1.0 : 0.0));
               fileWriter.write("\n");
            }
         }

         fileWriter.flush();
      }
      catch (Exception e)
      {
         LogTools.error("Error logging edge data");
         outputStream = null;
         printStream = null;
         e.printStackTrace();
         return false;
      }

      return true;
   }

   private void writeToFile(String file, byte[] fileContents) throws Exception
   {
      FileTools.ensureFileExists(new File(file).toPath());
      outputStream = new FileOutputStream(file);
      printStream = new PrintStream(outputStream);

      FootstepPlanningRequestPacket requestPacket = new FootstepPlanningRequestPacket();
      planner.getRequest().setPacket(requestPacket);
      printStream.write(fileContents);
      printStream.flush();
      outputStream.close();
      printStream.close();
   }

   private void writeNode(String name, FootstepNode node) throws IOException
   {
      if (node == null)
         fileWriter.write(name + ":null" + "\n");
      else
         fileWriter.write(name + ":" + node.getXIndex() + "," + node.getYIndex() + "," + node.getYawIndex() + "," + node.getRobotSide().ordinal() + "\n");
   }

   private void writeSnapData(String prefix, FootstepNodeSnapData snapData) throws IOException
   {
      RigidBodyTransform snapTransform = snapData.getSnapTransform();
      Quaternion quaternion = new Quaternion(snapTransform.getRotation());
      fileWriter.write(prefix + "snapTransform:" + EuclidCoreIOTools.getStringOf(",",
                                                                        EuclidCoreIOTools.getStringFormat(8, 8),
                                                                        quaternion.getX(),
                                                                        quaternion.getY(),
                                                                        quaternion.getZ(),
                                                                        quaternion.getS(),
                                                                        snapTransform.getTranslation().getX(),
                                                                        snapTransform.getTranslation().getY(),
                                                                        snapTransform.getTranslation().getZ()) + "\n");

      ConvexPolygon2D croppedFoothold = snapData.getCroppedFoothold();
      if (croppedFoothold.isEmpty() || croppedFoothold.containsNaN())
      {
         fileWriter.write(prefix + "croppedFoothold: null \n");
      }
      else
      {
         writeFootPolygon(prefix + "croppedFoothold:", croppedFoothold);
      }
   }

   private void writeFootPolygon(String prefix, ConvexPolygon2D croppedFoothold) throws IOException
   {
      fileWriter.write(prefix);

      for (int vertexIndex = 0; vertexIndex < croppedFoothold.getNumberOfVertices(); vertexIndex++)
      {
         Point2DReadOnly vertex = croppedFoothold.getVertex(vertexIndex);
         fileWriter.write(vertex.getX() + ", " + vertex.getY() + (vertexIndex == croppedFoothold.getNumberOfVertices() - 1 ? "" : ","));
      }

      fileWriter.write("\n");
   }
}
