package org.swarmer.operation.executor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.util.NetUtils;

import java.io.File;

public class AppendSwarmDeployment extends SwarmDeploymentProcessor {
   private static final Logger          LOG = LogManager.getLogger(AppendSwarmDeployment.class);
   private              DeploymentColor colorToDeploy;

   public AppendSwarmDeployment(SwarmerCtx ctx) {
      super(ctx);
   }

   @Override
   public SwarmJobProcessor init(SwarmJob swarmJob) {
      super.init(swarmJob);
      colorToDeploy = colorToDeploy();
      return this;
   }

   protected DeploymentColor colorToDeploy() {
      if (containerCfg().swarmDeploymentCfgs().isEmpty()) {
         return null;
      }
      final int          firstElemIndex = 0;
      SwarmDeploymentCfg deploymentCfg  = containerCfg().getSwarmDeploymentCfg(firstElemIndex);
      return deploymentCfg.getDeploymentColorEnum();
   }

   @Override
   public void process() throws Exception {
      if (containerCfg().swarmDeploymentCfgs().isEmpty()) {
         return;
      }

      int port = NetUtils.getFirstAvailablePort(portRange());

      if (port != -1) {
         File destFile = getDestFile();
         FileUtils.copyFile(getSrcFile(), destFile);
         swarmJob().setSwarmJarFile(destFile);
         SwarmDeployment swarmDeployment = startSwarmJar(port, colorToDeploy);

         if (swarmDeployment != null) {
            getCtx().addDeployment(swarmJob().getContainerName(), swarmDeployment);
            LOG.info("Swarm started!");
         } else {
            LOG.error("Swarm could not be started! See log rest file!");
         }
      } else {
         String errMsg = String.format("No available ports in the range %s", portRange().toString());
         LOG.error(errMsg);
      }
   }

   private File getDestFile() {
      File   coreFile    = getSrcFile();
      long   time        = System.currentTimeMillis();
      String newFilename = FilenameUtils.removeExtension(coreFile.getName()) + "-" + time + ".jar";
      String resultPath  = coreFile.getParent() + "\\" + newFilename;
      return new File(resultPath);
   }

   private File getSrcFile() {
      File coreFile = new File(containerCfg().getSwarmDeploymentCfg(0).getSwarmFilePath());
      return coreFile;
   }
}
