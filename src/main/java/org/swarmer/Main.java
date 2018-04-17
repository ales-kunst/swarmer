package org.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmerContext;
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
         SwarmConfig.Builder swarmBuilder = new SwarmConfig.Builder();
         SwarmConfig swarmConfig = swarmBuilder.setSectionName("testSection")
                                               .setSourcePath("D:\\swarmer_additional")
                                               .setTargetPath("D:\\temp\\tmp")
                                               .setFilePattern("demo-swarm.*\\.jar")
                                               .setBlueJvmParams(
                                                       "-Dfile.encoding=UTF-8 -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9991 -Djava.io.tmpdir=D:\\swarm_temp")
                                               .setGreenJvmParams(
                                                       "-Dfile.encoding=UTF-8 -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9992 -Djava.io.tmpdir=D:\\swarm_temp")
                                               .setConsulServiceHeatlhUrl(
                                                       "http://127.0.0.1:8500/v1/health/service/QnstMS")
                                               .build();
         DeploymentContainer deploymentContainer = new DeploymentContainer(swarmConfig);
         deploymentContainer.isValid();

         SwarmerContext.instance().addDeploymentContainer(deploymentContainer);

         // SwarmDeployRunner          swarmDeployRunner          = new SwarmDeployRunner();
         // Thread swarmDeployThread = swarmDeployRunner.getThread();
         // swarmDeployThread.start();

         watcherThread.join();
         // swarmDeployThread.join();

      } catch (Exception e) {
         LOG.error("Swarmer ended with error: {}", e);
      }
   }
}
