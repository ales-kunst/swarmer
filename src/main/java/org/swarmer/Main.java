package org.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerContextRetriever;


public class Main {

   private static final Logger LOG = LogManager.getLogger(Main.class);

   public static void main(String[] args) {
      try {
         LOG.info("Program started!!!");
         SwarmerContextRetriever.retrieve(SwarmerInputParams.iniFilePath());

         FolderChangesWatcherRunner folderChangesWatcherRunner = new FolderChangesWatcherRunner();
         SwarmDeployRunner          swarmDeployRunner          = new SwarmDeployRunner();

         Thread watherThread      = folderChangesWatcherRunner.getThread();
         Thread swarmDeployThread = swarmDeployRunner.getThread();

         watherThread.start();
         swarmDeployThread.start();

         watherThread.join();
         swarmDeployThread.join();

      } catch (Exception e) {
         LOG.error("Swarmer ended with error: {}", e);
      }
   }
}
