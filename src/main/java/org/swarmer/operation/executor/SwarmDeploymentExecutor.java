package org.swarmer.operation.executor;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmFile;
import org.swarmer.util.NetUtils;
import org.swarmer.util.SwarmUtil;

import java.io.IOException;

public class SwarmDeploymentExecutor {
   private static final Logger LOG = LogManager.getLogger(SwarmDeploymentExecutor.class);

   private static final String SWARM_DEPLOYMENT_COULD_NOT_BE_STOPPED =
           "Old swarm deployment [WindowTitle: %s] could not be stopped! Hard killing window! Manual intervention needed!";

   private final DeploymentColor     colorToDeploy;
   private final DeploymentColor     colorToRemove;
   private final SwarmFile           copiedSwarmFile;
   private final DeploymentContainer deploymentContainer;
   private final IntRange            portRange;

   public SwarmDeploymentExecutor(DeploymentContainer deploymentContainer, SwarmFile copiedSwarmFile,
                                  IntRange portRange) {
      this.deploymentContainer = deploymentContainer;
      this.copiedSwarmFile = copiedSwarmFile;
      this.colorToDeploy = deploymentContainer.nextDeploymentColor();
      this.colorToRemove = deploymentContainer.currentDeploymentColor();
      this.portRange = portRange;
   }

   public void execute() throws IOException {
      int port = NetUtils.getFirstAvailablePort(portRange);

      if (port != -1) {
         SwarmDeployment swarmDeployment = new SwarmDeployment(copiedSwarmFile, port);
         boolean         execSuccessful  = executeSwarmProcess(swarmDeployment);
         if (execSuccessful) {
            deploymentContainer.setDeployment(colorToDeploy, swarmDeployment);
            LOG.info("Swarm started!");

            // Only if we have already running instance then shutdown that instance of swarm
            if (colorToRemove != null) {
               // Shutdown old process
               boolean shutDownSuccess = shutdownOldDeployment();
               if (!shutDownSuccess) {
                  SwarmDeployment oldSwarmDeployment = deploymentContainer.getDeployment(colorToRemove);
                  String errMsg = String.format(SWARM_DEPLOYMENT_COULD_NOT_BE_STOPPED,
                                                oldSwarmDeployment.getWindowTitle());
                  LOG.error(errMsg);
                  swarmDeployment.hardKillSwarm();
               }
               deploymentContainer.clearDeployment(colorToRemove);
            }
         } else {
            LOG.error("Swarm could not be started! See log file [{}]", swarmDeployment.getLogFilename());
         }
      } else {
         String errMsg = String.format("No available ports in the range %s", portRange.toString());
         LOG.error(errMsg);
      }
   }

   private boolean executeSwarmProcess(SwarmDeployment swarmDeployment) throws IOException {
      LOG.info("Starting swarm: [{}]", swarmDeployment.getSwarmFile().getAbsolutePath());
      String windowTitle = String.format("%s %s [jar: %s, port: %d]", deploymentContainer.getName(),
                                         colorToDeploy.toString(), swarmDeployment.getSwarmFile().getFilename(),
                                         swarmDeployment.getPort());
      String jvmArgs = deploymentContainer.getJvmParams();
      String appArgs = "";

      SwarmFile swarmJarFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
      String[] swarmCommand = SwarmUtil.createSwarmCliArguments(windowTitle,
                                                                Integer.toString(swarmDeployment.getPort()),
                                                                jvmArgs,
                                                                swarmDeployment.getProcessTimeStart(),
                                                                appArgs,
                                                                swarmJarFile.getFile());
      swarmDeployment.setSwarmCommand(swarmCommand);
      SwarmUtil.startSwarmInstance(swarmCommand);
      swarmDeployment.setSwarmState(SwarmFile.State.STARTING_SWARM, null);
      String serviceId = getServiceId(swarmDeployment);
      boolean registeredSuccessful = SwarmUtil.waitForServiceRegistration(deploymentContainer.getConsulUrl(),
                                                                          deploymentContainer.getConsulServiceName(),
                                                                          serviceId,
                                                                          300, 1000);
      if (registeredSuccessful) {
         swarmDeployment.setWindowTitle(windowTitle);
         swarmDeployment.setSwarmState(SwarmFile.State.SWARM_STARTED, null);
      }

      return registeredSuccessful;
   }

   private boolean shutdownOldDeployment() {
      boolean         deploymentExitedSuccessful = true;
      SwarmDeployment deploymentToRemove         = deploymentContainer.getDeployment(colorToRemove);
      if (deploymentToRemove != null) {
         deploymentToRemove.sigIntProces();
         deploymentExitedSuccessful = deploymentToRemove.waitForSwarmToShutdown();
      }
      return deploymentExitedSuccessful;
   }

   private String getServiceId(SwarmDeployment swarmDeployment) {
      return deploymentContainer.getConsulServiceName() + ":127.0.0.1:" + swarmDeployment.getPort();
   }
}
