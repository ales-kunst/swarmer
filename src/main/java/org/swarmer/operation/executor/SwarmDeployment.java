package org.swarmer.operation.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.util.FileUtil;
import org.swarmer.util.NetUtils;

import java.io.File;

class SwarmDeployment extends SwarmDeploymentProcessor {
   private static final Logger          LOG = LoggerFactory.getLogger(SwarmDeployment.class);
   // Local variables
   private              DeploymentColor colorToDeploy;
   private              DeploymentColor colorToRemove;


   SwarmDeployment(SwarmerCtx ctx) {
      super(ctx);
   }

   @Override
   public SwarmJobProcessor init(SwarmJob swarmJob) {
      super.init(swarmJob);
      colorToDeploy = colorToDeploy();
      colorToRemove = colorToRemove();
      return this;
   }

   protected DeploymentColor colorToDeploy() {
      boolean isEmptyDeploymentList = containerCfg().swarmDeploymentCfgsSize() == 0;
      if (isEmptyDeploymentList) {
         return DeploymentColor.BLUE;
      }
      final int          firstElemIndex = 0;
      SwarmDeploymentCfg deploymentCfg  = containerCfg().getSwarmDeploymentCfg(firstElemIndex);
      return deploymentCfg.isBlueDeployment() ? DeploymentColor.GREEN : DeploymentColor.BLUE;
   }

   private DeploymentColor colorToRemove() {
      boolean isEmptyDeploymentList = containerCfg().swarmDeploymentCfgsSize() == 0;
      if (isEmptyDeploymentList) {
         return null;
      }
      final int          firstElemIndex = 0;
      SwarmDeploymentCfg deploymentCfg  = containerCfg().getSwarmDeploymentCfg(firstElemIndex);
      return deploymentCfg.getDeploymentColorEnum();
   }

   @Override
   public void processImpl() throws Exception {
      int port = NetUtils.getFirstAvailablePort(portRange());

      if (port != -1) {
         org.swarmer.context.SwarmDeployment swarmDeployment = startSwarmJar(port, colorToDeploy);

         if (swarmDeployment != null) {
            getCtx().addDeployment(swarmJob().getContainerName(), swarmDeployment);
            if (colorToRemove != null) {
               shutdownOldDeployment();
               getCtx().clearDeployment(swarmJob().getContainerName(), colorToRemove);
            }
         }
      } else {
         String errMsg = String.format("No available ports in the range %s", portRange().toString());
         LOG.warn(errMsg);
      }
   }

   private void shutdownOldDeployment() {
      for (int index = 0; index < containerCfg().swarmDeploymentCfgsSize(); index++) {
         SwarmDeploymentCfg deploymentCfg = containerCfg().getSwarmDeploymentCfg(index);
         int                pid           = deploymentCfg.getPid();
         String             windowTitle   = deploymentCfg.getWindowTitle();

         shutdownSwarmInstance(containerCfg().getConsulUrl(), containerCfg().getConsulServiceName(), pid, windowTitle);
         File fileToRemove = new File(deploymentCfg.getSwarmFilePath());
         if (fileToRemove.exists()) {
            LOG.info("Removing old deployment file [{}].", fileToRemove.getAbsolutePath());
            FileUtil.forceRemoveFile(fileToRemove);
         }
      }
   }
}
