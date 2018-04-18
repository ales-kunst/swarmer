package org.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerContextRetriever;
import org.swarmer.util.FileUtil;

import java.io.File;


public class Main {

   private static final Logger LOG = LogManager.getLogger(Main.class);

   public static void main(String[] args) {
      try {
         LOG.info("Program started!!!");

         new File(FileUtil.KILL_APP_PATH).delete();
         new File(FileUtil.WIN_TEE_APP_PATH).delete();
         FileUtil.copyWindowsKillAppToTmp();
         FileUtil.copyWinTeeAppToTmp();

         SwarmerContextRetriever.retrieve(SwarmerInputParams.iniFilePath());

         FolderChangesWatcherRunner folderChangesWatcherRunner = new FolderChangesWatcherRunner();
         Thread                     watcherThread              = folderChangesWatcherRunner.getThread();
         watcherThread.start();

         SwarmDeployRunner swarmDeployRunner = new SwarmDeployRunner();
         Thread            swarmDeployThread = swarmDeployRunner.getThread();
         swarmDeployThread.start();

         watcherThread.join();
         // swarmDeployThread.join();

      } catch (Exception e) {
         LOG.error("Swarmer ended with error: {}", e);
      }
   }
}
