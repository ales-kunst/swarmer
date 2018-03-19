package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmerContext;

public class SwarmDeployer {
   private static final Logger LOG = LogManager.getLogger(SwarmDeployer.class);

   SwarmerContext swarmerCtx;

   public SwarmDeployer() {
      swarmerCtx = SwarmerContext.instance();
   }

   private void sleep(long millis) {
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
         LOG.warn("Sleep error: []", e);
      }
   }

   public void start() {
      while (true) {

         for (DeploymentContainer deploymentContainer : swarmerCtx.getDeploymentContainers()) {
            if (!deploymentContainer.isDeploymentInProgress()) {
               SwarmDeploymentExecutor executor = new SwarmDeploymentExecutor(deploymentContainer);
               executor.run();
            }
         }

         sleep(1000);
      }
   }

}
