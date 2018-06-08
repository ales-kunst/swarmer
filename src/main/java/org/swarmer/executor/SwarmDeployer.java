package org.swarmer.executor;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.InfiniteThreadOperation;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmFile;
import org.swarmer.context.SwarmerContext;
import org.swarmer.util.SwarmUtil;

import java.io.IOException;

public class SwarmDeployer extends InfiniteThreadOperation<SwarmerContext> {
   private static final Logger LOG = LogManager.getLogger(SwarmDeployer.class);

   public SwarmDeployer(SwarmerContext context) {
      super(context);
   }

   @Override
   protected String threadName() {
      return "Swarm Deployment Executor";
   }

   @Override
   protected void operationInitialize() { }

   @Override
   protected boolean shouldStop() {
      return false;
   }

   @Override
   protected void loopBlock() {
      for (DeploymentContainer deploymentContainer : getContext().getDeploymentContainers()) {
         SwarmFile copiedSwarmFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
         if (!deploymentContainer.isDeploymentInProgress() && (copiedSwarmFile != null)) {
            executeSwarmDeployment(deploymentContainer, copiedSwarmFile);
            if (deploymentContainer.removeSwarmFile(copiedSwarmFile)) {
               LOG.info("Swarm file successfully removed from queue.");
            } else {
               LOG.error("Swarm file was no removed from queue.");
            }
         }
      }
      SwarmUtil.waitFor(1000);
   }

   @Override
   protected void handleError(Exception exception) {
      LOG.error("Exception from processWatchEvents. Continue with watch. Error stacktrace: \n {}",
                ExceptionUtils.getStackTrace(exception));
   }

   @Override
   protected void operationFinalize() {

   }

   private void executeSwarmDeployment(DeploymentContainer deploymentContainer, SwarmFile swarmFile) {
      try {
         deploymentContainer.setDeploymentInProgress(true);
         SwarmFile copiedSwarmFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
         if (copiedSwarmFile != null) {
            SwarmDeploymentExecutor swarmDeploymentExecutor = new SwarmDeploymentExecutor(deploymentContainer,
                                                                                          swarmFile,
                                                                                          getContext().getPortRange());
            swarmDeploymentExecutor.execute();
         }
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         deploymentContainer.setDeploymentInProgress(false);
      }
   }
}
