package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmFile;
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
            SwarmFile swarmFile = deploymentContainer.getLastSwarmFile(SwarmFile.State.COPIED);
            if (!deploymentContainer.isDeploymentInProgress() && (swarmFile != null)) {
               SwarmDeploymentExecutor executor = new SwarmDeploymentExecutor(deploymentContainer);
               executor.run();
               if (deploymentContainer.removeSwarmFile(swarmFile)) {
                  LOG.info("Swarm file successfully removed from queue.");
               } else {
                  LOG.error("Swarm file was no removed from queue.");
               }
            }
         }
         // System.out.println("-------------- Running Swarm Deployer --------------");
         SwarmExecutor.waitFor(1000);
      }
   }
}
