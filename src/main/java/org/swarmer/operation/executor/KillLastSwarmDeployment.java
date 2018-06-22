package org.swarmer.operation.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.SwarmDeploymentCfg;

import java.io.File;

public class KillLastSwarmDeployment extends SwarmDeploymentProcessor {
   private static final Logger LOG = LogManager.getLogger(KillLastSwarmDeployment.class);

   public KillLastSwarmDeployment(SwarmerCtx ctx) {
      super(ctx);
   }


   @Override
   public void process() {
      int                lastIndex         = containerCfg().swarmDeploymentCfgsSize() - 1;
      SwarmDeploymentCfg lastDeploymentCfg = containerCfg().getSwarmDeploymentCfg(lastIndex);
      shutdownLastSwarmDeployment(lastDeploymentCfg);
      getCtx().removeDeployment(containerCfg().getName(), lastDeploymentCfg.getPid());
   }


   private void shutdownLastSwarmDeployment(SwarmDeploymentCfg lastDeploymentCfg) {
      int    pid         = lastDeploymentCfg.getPid();
      String windowTitle = lastDeploymentCfg.getWindowTitle();
      shutdownSwarmInstance(pid, windowTitle);
      File fileToRemove = new File(lastDeploymentCfg.getSwarmFilePath());
      if (fileToRemove.exists()) {
         fileToRemove.delete();
      }
   }
}
