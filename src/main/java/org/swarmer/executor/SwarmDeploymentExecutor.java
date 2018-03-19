package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmFile;

public class SwarmDeploymentExecutor implements Runnable {
   private static final Logger LOG = LogManager.getLogger(SwarmDeploymentExecutor.class);
   private final DeploymentContainer deploymentContainer;

   public SwarmDeploymentExecutor(DeploymentContainer deploymentContainer) {
      this.deploymentContainer = deploymentContainer;
   }

   private void executeSwarmProcess(SwarmDeployment swarmDeployment) {
      LOG.info("Starting swarm: []", swarmDeployment.getSwarmFile());
      swarmDeployment.setSwarmState(SwarmFile.State.STARTING_SWARM, null);
   }

   @Override
   public void run() {
      SwarmDeployment swarmDeployment = null;
      try {
         deploymentContainer.setDeploymentInProgress(true);
         SwarmFile copiedSwarmFile = deploymentContainer.getLastSwarmFile(SwarmFile.State.COPIED);
         swarmDeployment = new SwarmDeployment(copiedSwarmFile);
         executeSwarmProcess(swarmDeployment);
      } catch (Exception e) {
         LOG.error("Error when executing swarm instance: []", e.getMessage());
         if (swarmDeployment != null) {
            swarmDeployment.setSwarmState(SwarmFile.State.ERROR_STARTING_SWARM, e);
         }
      } finally {
         deploymentContainer.setDeploymentInProgress(false);
      }
   }
}
