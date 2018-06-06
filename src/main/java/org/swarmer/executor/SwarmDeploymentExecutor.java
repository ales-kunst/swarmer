package org.swarmer.executor;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.InfiniteThreadOperation;
import org.swarmer.context.*;
import org.swarmer.util.NetUtils;
import org.swarmer.util.SwarmExecutor;

public class SwarmDeploymentExecutor extends InfiniteThreadOperation<SwarmerContext> {
   private static final Logger LOG                                   = LogManager.getLogger(
           SwarmDeploymentExecutor.class);
   private static final String SWARM_DEPLOYMENT_COULD_NOT_BE_STOPPED = "Old swarm deployment [WindowTitle: %s] could not be stopped! Manual intervention needed!";

   public SwarmDeploymentExecutor(SwarmerContext context) {
      super(context);
   }

   @Override
   protected String threadName() {
      return "Swarm Deployment Executor";
   }

   @Override
   protected void operationInitialize() {

   }

   @Override
   protected boolean shouldStop() {
      return false;
   }

   @Override
   protected void loopBlock() {
      for (DeploymentContainer deploymentContainer : getContext().getDeploymentContainers()) {
         SwarmFile swarmFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
         if (!deploymentContainer.isDeploymentInProgress() && (swarmFile != null)) {
            executeSwarmDeployment(deploymentContainer);
            if (deploymentContainer.removeSwarmFile(swarmFile)) {
               LOG.info("Swarm file successfully removed from queue.");
            } else {
               LOG.error("Swarm file was no removed from queue.");
            }
         }
      }
      SwarmExecutor.waitFor(1000);
   }

   @Override
   protected void handleError(Exception exception) {
      LOG.error("Exception from processWatchEvents. Continue with watch. Error stacktrace: \n {}",
                ExceptionUtils.getStackTrace(exception));
   }

   @Override
   protected void operationFinalize() {

   }

   private void executeSwarmDeployment(DeploymentContainer deploymentContainer) {
      try {
         deploymentContainer.setDeploymentInProgress(true);
         SwarmFile copiedSwarmFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
         if (copiedSwarmFile != null) {
            startDeploy(deploymentContainer);
         }
      } finally {
         deploymentContainer.setDeploymentInProgress(false);
      }
   }

   private void startDeploy(DeploymentContainer deploymentContainer) {
      int             port               = NetUtils.getFirstAvailablePort(getContext().getPortRange());
      DeploymentColor colorToDeploy      = deploymentContainer.nextDeploymentColor();
      SwarmDeployment oldSwarmDeployment = deploymentContainer.getDeployment(colorToDeploy);
      SwarmFile       copiedSwarmFile    = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
      SwarmDeployment swarmDeployment;

      if (port != -1) {
         boolean oldDeploymentExitedSuccessful = shutdownOldDeployment(deploymentContainer, colorToDeploy);
         // Check if old swarm was stopped succesfully
         if (oldDeploymentExitedSuccessful) {
            swarmDeployment = new SwarmDeployment(copiedSwarmFile, port);
            boolean execSuccessful = executeSwarmProcess(deploymentContainer, swarmDeployment, colorToDeploy);
            if (execSuccessful) {
               deploymentContainer.setDeployment(colorToDeploy, swarmDeployment);
               LOG.error("Swarm started!");
            } else {
               LOG.error("Swarm could not be started! See log file [{}]", swarmDeployment.getLogFilename());
            }
         } else {
            String errMsg = String.format(SWARM_DEPLOYMENT_COULD_NOT_BE_STOPPED, oldSwarmDeployment.getWindowTitle());
            LOG.error(errMsg);
         }
      } else {
         String errMsg = String.format("No available ports in the range %s",
                                       getContext().getPortRange().toString());
         LOG.error(errMsg);
      }
   }

   private boolean shutdownOldDeployment(DeploymentContainer deploymentContainer, DeploymentColor colorToDeploy) {
      boolean         deploymentExitedSuccessful = true;
      SwarmDeployment oldSwarmDeployment         = deploymentContainer.getDeployment(colorToDeploy);
      if (oldSwarmDeployment != null) {
         oldSwarmDeployment.sigIntProces();
         deploymentExitedSuccessful = oldSwarmDeployment.waitForSwarmToShutdown();
      }
      return deploymentExitedSuccessful;
   }

   private boolean executeSwarmProcess(DeploymentContainer deploymentContainer, SwarmDeployment swarmDeployment,
                                       DeploymentColor colorToDeploy) {
      LOG.info("Starting swarm: [{}]", swarmDeployment.getSwarmFile().getAbsolutePath());
      String windowTitle = String.format("%s %s [jar: %s, port: %d]", deploymentContainer.getName(),
                                         colorToDeploy.toString(), swarmDeployment.getSwarmFile().getFilename(),
                                         swarmDeployment.getPort());
      String jvmArgs = deploymentContainer.getJvmParams();
      String appArgs = "";

      SwarmFile swarmJarFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
      String[] swarmCommand = SwarmExecutor.createSwarmCliArguments(windowTitle,
                                                                    Integer.toString(swarmDeployment.getPort()),
                                                                    jvmArgs,
                                                                    swarmDeployment.getProcessTimeStart(),
                                                                    appArgs,
                                                                    swarmJarFile.getFile());
      swarmDeployment.setSwarmCommand(swarmCommand);
      Process swarmProcess = SwarmExecutor.startSwarmInstance(swarmCommand);
      swarmDeployment.setSwarmState(SwarmFile.State.STARTING_SWARM, null);
      String consulHealthServiceUrl = deploymentContainer.getConsulHealthServiceUrl();
      boolean registeredSuccessful = SwarmExecutor.waitForServiceRegistration(swarmProcess, consulHealthServiceUrl,
                                                                              300, 1000);
      if (registeredSuccessful) {
         swarmDeployment.setWindowTitle(windowTitle);
         swarmDeployment.setSwarmState(SwarmFile.State.SWARM_STARTED, null);
      }

      return registeredSuccessful;
   }
}
