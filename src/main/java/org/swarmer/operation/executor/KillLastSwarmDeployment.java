package org.swarmer.operation.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.util.FileUtil;

import java.io.File;

public class KillLastSwarmDeployment extends SwarmDeploymentProcessor {
   private static final Logger LOG = LoggerFactory.getLogger(KillLastSwarmDeployment.class);

   public KillLastSwarmDeployment(SwarmerCtx ctx) {
      super(ctx);
   }


   @Override
   public void processImpl() {
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
         LOG.info("Removing old deployment file [{}].", fileToRemove.getAbsolutePath());
         FileUtil.forceRemoveFile(fileToRemove);
      }
   }
}
