package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmerContext;
import org.swarmer.util.SwarmExecutor;

public class SwarmDeployer {
   private static final Logger LOG = LogManager.getLogger(SwarmDeployer.class);

   SwarmerContext swarmerCtx;

   public SwarmDeployer() {
      swarmerCtx = SwarmerContext.instance();
   }

   public void start() {
      while (true) {

         for (DeploymentContainer deploymentContainer : swarmerCtx.getDeploymentContainers()) {
            if (!deploymentContainer.isDeploymentInProgress()) {
               SwarmDeploymentExecutor executor = new SwarmDeploymentExecutor(deploymentContainer);
               executor.run();
            }
         }

         SwarmExecutor.waitFor(1000);
      }
   }
}
