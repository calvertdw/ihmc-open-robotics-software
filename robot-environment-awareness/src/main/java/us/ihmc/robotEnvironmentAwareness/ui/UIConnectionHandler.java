package us.ihmc.robotEnvironmentAwareness.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Window;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.robotEnvironmentAwareness.communication.REAModuleAPI;
import us.ihmc.robotEnvironmentAwareness.communication.REAUIMessager;

public class UIConnectionHandler
{
   private final AtomicBoolean enable = new AtomicBoolean(false);
   private final Window mainWindow;
   private final REAUIMessager uiMessager;
   
   private final Topic<Boolean> requestEntireModuleState;

   public UIConnectionHandler(Window mainWindow, REAUIMessager uiMessager, Topic<Boolean> requestEntireModuleState)
   {
      this.mainWindow = mainWindow;
      this.uiMessager = uiMessager;
      if(requestEntireModuleState == null)
         this.requestEntireModuleState = REAModuleAPI.RequestEntireModuleState;
      else
         this.requestEntireModuleState = requestEntireModuleState;
      uiMessager.registerModuleMessagerStateListener(isMessagerOpen -> {
         if (isMessagerOpen)
            new Thread(() -> {
               // It seems like the main window has to be up to have access to the communication.
               while (!mainWindow.isShowing())
                  ThreadTools.sleep(100);
               refreshUIControls();
            }, "REAUIConnectionHandler").start();
         else
            displayWarning();
      });
   }
   
   public UIConnectionHandler(Window mainWindow, REAUIMessager uiMessager)
   {
      this(mainWindow, uiMessager, null);
   }

   public void start()
   {
      enable.set(true);
   }

   public void stop()
   {
      enable.set(false);
   }

   private void refreshUIControls()
   {
      if (enable.get())
      {
         uiMessager.submitStateRequestToModule(requestEntireModuleState);
      }
   }

   private void displayWarning()
   {
      if (!enable.get())
         return;

      Platform.runLater(this::createWarning);
   }

   private void createWarning()
   {
      Alert connectionLostAlert = new Alert(AlertType.WARNING);
      connectionLostAlert.setHeaderText("Lost connection");
      connectionLostAlert.initOwner(mainWindow);
      connectionLostAlert.showAndWait();
   }
}
