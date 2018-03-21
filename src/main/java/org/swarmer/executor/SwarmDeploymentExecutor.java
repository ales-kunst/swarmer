package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmFile;
import org.swarmer.util.NetUtils;

public class SwarmDeploymentExecutor implements Runnable {
   private static final Logger LOG = LogManager.getLogger(SwarmDeploymentExecutor.class);
   private final DeploymentContainer deploymentContainer;

   public SwarmDeploymentExecutor(DeploymentContainer deploymentContainer) {
      this.deploymentContainer = deploymentContainer;
   }

   @Override
   public void run() {
      SwarmDeployment swarmDeployment = null;
      try {
         deploymentContainer.setDeploymentInProgress(true);
         DeploymentColor colorToDeploy   = deploymentContainer.nextDeploymentColor();
         SwarmFile       copiedSwarmFile = deploymentContainer.getLastSwarmFile(SwarmFile.State.COPIED);
         if (copiedSwarmFile != null) {
            swarmDeployment = startDeploy(copiedSwarmFile, colorToDeploy);
         }
      } catch (Exception e) {
         LOG.error("Error when executing swarm instance: {}", e);
         if (swarmDeployment != null) {
            swarmDeployment.setSwarmState(SwarmFile.State.ERROR_STARTING_SWARM, e);
         }
      } finally {
         deploymentContainer.setDeploymentInProgress(false);
      }
   }

   private boolean checkPortAvailability(SwarmDeployment swarmDeployment, DeploymentColor colorToDeploy) {
      int    port          = -1;
      String swarmFilePath = swarmDeployment.getSwarmFile().getAbsolutePath();
      if (colorToDeploy.equals(DeploymentColor.BLUE)) {
         port = deploymentContainer.getSwarmConfig().getBlueUrlPort();
         LOG.info("Checking port {} availability for blue deployment [{}]", port, swarmFilePath);
      } else if (colorToDeploy.equals(DeploymentColor.GREEN)) {
         port = deploymentContainer.getSwarmConfig().getGreenUrlPort();
         LOG.info("Checking port {} availability for green deployment [{}]", port, swarmFilePath);
      }

      return NetUtils.isPortAvailable(port);
   }

   private void executeSwarmProcess(SwarmDeployment swarmDeployment) {
      LOG.info("Starting swarm: [{}]", swarmDeployment.getSwarmFile().getAbsolutePath());
      swarmDeployment.setSwarmState(SwarmFile.State.STARTING_SWARM, null);
   }

   private SwarmDeployment startDeploy(SwarmFile copiedSwarmFile, DeploymentColor colorToDeploy) {
      SwarmDeployment swarmDeployment             = new SwarmDeployment(copiedSwarmFile);
      boolean         shouldCheckPortAvailability = deploymentContainer.getDeployment(colorToDeploy) == null;
      boolean         isPortAvailable             =
              !shouldCheckPortAvailability || checkPortAvailability(swarmDeployment, colorToDeploy);

      if (isPortAvailable) {
         executeSwarmProcess(swarmDeployment);
      } else {
         swarmDeployment.setSwarmState(SwarmFile.State.SWARM_PORT_TAKEN, null);
      }
      return swarmDeployment;
   }
}
