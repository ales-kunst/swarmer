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
         SwarmDeployment swarmDeployment = executeSwarmProcess(port);
         if (swarmDeployment != null) {
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
                  LOG.info("Trying hard killing window [{}]", oldSwarmDeployment.getWindowTitle());
                  oldSwarmDeployment.hardKillSwarm();
               }
               deploymentContainer.clearDeployment(colorToRemove);
            }
         } else {
            LOG.error("Swarm could not be started! See log swarm file!");
         }
      } else {
         String errMsg = String.format("No available ports in the range %s", portRange.toString());
         LOG.error(errMsg);
      }
   }

   private SwarmDeployment executeSwarmProcess(int port) throws IOException {
      LOG.info("Starting swarm: [{}]", copiedSwarmFile.getAbsolutePath());
      String windowTitle = String.format("%s %s [jar: %s, port: %d]", deploymentContainer.getName(),
                                         colorToDeploy.toString(), copiedSwarmFile.getFilename(),
                                         port);
      String jvmArgs = deploymentContainer.getJvmParams();
      String appArgs = "";

      // SwarmFile swarmJarFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
      long timeStarted = System.currentTimeMillis();
      String[] swarmCommand = SwarmUtil.createSwarmCliArguments(windowTitle,
                                                                Integer.toString(port),
                                                                jvmArgs,
                                                                timeStarted,
                                                                appArgs,
                                                                copiedSwarmFile.getFile());
      SwarmUtil.startSwarmInstance(swarmCommand);
      String serviceId = getServiceId(port);
      boolean registeredSuccessful = SwarmUtil.waitForServiceRegistration(deploymentContainer.getConsulUrl(),
                                                                          deploymentContainer.getConsulServiceName(),
                                                                          serviceId,
                                                                          300, 1000);
      SwarmDeployment resultDeployment = null;
      if (registeredSuccessful) {
         int pid = SwarmUtil.getSwarmPID(copiedSwarmFile.getFilename(), timeStarted);
         resultDeployment = SwarmDeployment.builder()
                                           .file(copiedSwarmFile)
                                           .pid(pid)
                                           .windowTitle(windowTitle)
                                           .timeStart(timeStarted)
                                           .build();
      }

      return resultDeployment;
   }

   private boolean shutdownOldDeployment() {
      boolean         deploymentExitedSuccessful = true;
      SwarmDeployment deploymentToRemove         = deploymentContainer.getDeployment(colorToRemove);
      if (deploymentToRemove != null) {
         boolean processSigInted = deploymentToRemove.sigIntProces();
         deploymentExitedSuccessful = processSigInted && deploymentToRemove.waitForSwarmToShutdown();
      }
      return deploymentExitedSuccessful;
   }

   private String getServiceId(int port) {
      return deploymentContainer.getConsulServiceName() + ":127.0.0.1:" + port;
   }
}
