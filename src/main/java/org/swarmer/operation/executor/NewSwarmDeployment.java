package org.swarmer.operation.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.util.NetUtils;

import java.io.File;

class NewSwarmDeployment extends SwarmDeploymentProcessor {
   private static final Logger          LOG                             = LogManager.getLogger(
           NewSwarmDeployment.class);
   // Local variables
   private              DeploymentColor colorToDeploy;
   private              DeploymentColor colorToRemove;


   NewSwarmDeployment(SwarmerCtx ctx) {
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
   public void process() throws Exception {
      int port = NetUtils.getFirstAvailablePort(portRange());

      if (port != -1) {
         SwarmDeployment swarmDeployment = startSwarmJar(port, colorToDeploy);

         if (swarmDeployment != null) {
            getCtx().addDeployment(swarmJob().getContainerName(), swarmDeployment);
            LOG.info("Swarm started!");
            if (colorToRemove != null) {
               shutdownOldDeployment();
               getCtx().clearDeployment(swarmJob().getContainerName(), colorToRemove);
            }
         } else {
            LOG.error("Swarm could not be started! See log rest file!");
         }
      } else {
         String errMsg = String.format("No available ports in the range %s", portRange().toString());
         LOG.error(errMsg);
      }
   }

   private void shutdownOldDeployment() {
      for (int index = 0; index < containerCfg().swarmDeploymentCfgsSize(); index++) {
         SwarmDeploymentCfg deploymentCfg = containerCfg().getSwarmDeploymentCfg(index);
         int                pid           = deploymentCfg.getPid();
         String             windowTitle   = deploymentCfg.getWindowTitle();
         shutdownSwarmInstance(pid, windowTitle);
         File fileToRemove = new File(deploymentCfg.getSwarmFilePath());
         if (fileToRemove.exists()) {
            fileToRemove.delete();
         }
      }
   }
}
