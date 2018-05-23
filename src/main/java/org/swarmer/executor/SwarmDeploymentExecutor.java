package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.*;
import org.swarmer.util.NetUtils;
import org.swarmer.util.SwarmExecutor;

public class SwarmDeploymentExecutor implements Runnable {
   public static final  String              SWARM_DEPLOYMENT_COLD_NOT_BE_STOPPED = "Old swarm deployment [WindowTitle: %s] could not be stopped! Manual intervention needed!";
   private static final Logger              LOG                                  = LogManager.getLogger(
           SwarmDeploymentExecutor.class);
   private final        DeploymentContainer deploymentContainer;
   private final        SwarmerContext      swarmerCtx;

   public SwarmDeploymentExecutor(DeploymentContainer deploymentContainer) {
      this.deploymentContainer = deploymentContainer;
      swarmerCtx = SwarmerContext.instance();
   }

   @Override
   public void run() {
      try {
         deploymentContainer.setDeploymentInProgress(true);
         DeploymentColor colorToDeploy   = deploymentContainer.nextDeploymentColor();
         SwarmFile       copiedSwarmFile = deploymentContainer.getLastSwarmFile(SwarmFile.State.COPIED);
         if (copiedSwarmFile != null) {
            startDeploy(copiedSwarmFile, colorToDeploy);
         }
      } catch (Exception e) {
         LOG.error("Error when executing swarm instance: {}", e);
      } finally {
         deploymentContainer.setDeploymentInProgress(false);
      }
   }

   private void startDeploy(SwarmFile copiedSwarmFile,
                            DeploymentColor colorToDeploy) {
      int             port               = NetUtils.getFirstAvailablePort(swarmerCtx.getPortRange());
      SwarmDeployment oldSwarmDeployment = deploymentContainer.getDeployment(colorToDeploy);
      SwarmDeployment swarmDeployment;

      if (port != -1) {
         boolean oldDeploymentExitedSuccessful = shutdownOldDeployment(colorToDeploy);
         // Check if old swarm was stopped succesfully
         if (oldDeploymentExitedSuccessful) {
            swarmDeployment = new SwarmDeployment(copiedSwarmFile, port);
            boolean execSuccessful = executeSwarmProcess(swarmDeployment, colorToDeploy);
            if (execSuccessful) {
               deploymentContainer.setDeployment(colorToDeploy, swarmDeployment);
               LOG.error("Swarm started!");
            } else {
               LOG.error("Swarm could not be started! See log file [{}]", swarmDeployment.getLogFilename());
            }
         } else {
            String errMsg = String.format(SWARM_DEPLOYMENT_COLD_NOT_BE_STOPPED, oldSwarmDeployment.getWindowTitle());
            LOG.error(errMsg);
         }
      } else {
         String errMsg = String.format("No available ports in the range %s",
                                       swarmerCtx.getPortRange().toString());
         LOG.error(errMsg);
      }
   }

   private boolean shutdownOldDeployment(DeploymentColor colorToDeploy) {
      boolean         deploymentExitedSuccessful = true;
      SwarmDeployment oldSwarmDeployment         = deploymentContainer.getDeployment(colorToDeploy);
      if (oldSwarmDeployment != null) {
         oldSwarmDeployment.sigIntProces();
         deploymentExitedSuccessful = oldSwarmDeployment.waitForSwarmToShutdown();
      }
      return deploymentExitedSuccessful;
   }

   private boolean executeSwarmProcess(SwarmDeployment swarmDeployment, DeploymentColor colorToDeploy) {
      LOG.info("Starting swarm: [{}]", swarmDeployment.getSwarmFile().getAbsolutePath());
      String windowTitle = String.format("%s %s [jar: %s, port: %d]", deploymentContainer.getName(),
                                         colorToDeploy.toString(), swarmDeployment.getSwarmFile().getFilename(),
                                         swarmDeployment.getPort());
      String jvmArgs;
      String appArgs = "";
      if (colorToDeploy.equals(DeploymentColor.BLUE)) {
         jvmArgs = deploymentContainer.getSwarmConfig().getBlueJvmParams();
      } else {
         jvmArgs = deploymentContainer.getSwarmConfig().getGreenJvmParams();
      }

      SwarmFile swarmJarFile = deploymentContainer.getLastSwarmFile(SwarmFile.State.COPIED);
      String[] swarmCommand = SwarmExecutor.createSwarmCliArguments(windowTitle,
                                                                    Integer.toString(swarmDeployment.getPort()),
                                                                    jvmArgs,
                                                                    swarmDeployment.getProcessTimeStart(),
                                                                    appArgs,
                                                                    swarmJarFile.getFile());
      swarmDeployment.setSwarmCommand(swarmCommand);
      Process swarmProcess = SwarmExecutor.startSwarmInstance(swarmCommand);
      swarmDeployment.setSwarmState(SwarmFile.State.STARTING_SWARM, null);
      String consulHealthServiceUrl = deploymentContainer.getSwarmConfig().getConsulHealthServiceUrl();
      boolean registeredSuccessful = SwarmExecutor.waitForServiceRegistration(swarmProcess, consulHealthServiceUrl,
                                                                              300, 1000);
      if (registeredSuccessful) {
         swarmDeployment.setWindowTitle(windowTitle);
         swarmDeployment.setSwarmState(SwarmFile.State.SWARM_STARTED, null);
      }

      return registeredSuccessful;
   }
}
