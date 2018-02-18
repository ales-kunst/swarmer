package com.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerContextRetriever;

import com.swarmer.watcher.FolderChangesWatcher;


public class Main {
   
   private static final Logger LOG = LogManager.getLogger(Main.class); 
   
   public static void main(String[] args) {
      try {
         LOG.info("Program started!!!");
         SwarmerContextRetriever.retrieve(SwarmerInputParams.iniFilePath());
         FolderChangesWatcher folderChangesWatcher = new FolderChangesWatcher();
         folderChangesWatcher.start();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
