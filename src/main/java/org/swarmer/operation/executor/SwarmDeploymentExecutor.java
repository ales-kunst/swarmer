package org.swarmer.operation.executor;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.util.NetUtils;
import org.swarmer.util.SwarmUtil;

import java.io.File;
import java.io.IOException;

class SwarmDeploymentExecutor {
   private static final Logger LOG = LogManager.getLogger(SwarmDeploymentExecutor.class);

   private static final String                 SWARM_DEPLOYMENT_COULD_NOT_BE_STOPPED =
           "Old rest deployment [WindowTitle: %s] could not be stopped! Hard killing window! Manual intervention needed!";
   private final        DeploymentColor        colorToDeploy;
   private final        DeploymentColor        colorToRemove;
   private final        DeploymentContainerCfg containerCfg;
   private final        SwarmerCtx             ctx;
   private final        IntRange               portRange;
   private final        SwarmJob               swarmJob;


   SwarmDeploymentExecutor(SwarmerCtx ctx, DeploymentContainerCfg containerCfg, SwarmJob swarmJob) {
      this.ctx = ctx;
      this.portRange = ctx.getPortRange();
      this.swarmJob = swarmJob;
      this.containerCfg = containerCfg;
      colorToDeploy = colorToDeploy();
      colorToRemove = colorToRemove();
   }

   private DeploymentColor colorToDeploy() {
      boolean isEmptyDeploymentList = containerCfg.swarmDeploymentCfgsSize() == 0;
      if (isEmptyDeploymentList) {
         return DeploymentColor.BLUE;
      }
      final int          firstElemIndex = 0;
      SwarmDeploymentCfg deploymentCfg  = containerCfg.getSwarmDeploymentCfg(firstElemIndex);
      return deploymentCfg.isBlueDeployment() ? DeploymentColor.GREEN : DeploymentColor.BLUE;
   }

   private DeploymentColor colorToRemove() {
      boolean isEmptyDeploymentList = containerCfg.swarmDeploymentCfgsSize() == 0;
      if (isEmptyDeploymentList) {
         return null;
      }
      final int          firstElemIndex = 0;
      SwarmDeploymentCfg deploymentCfg  = containerCfg.getSwarmDeploymentCfg(firstElemIndex);
      return deploymentCfg.getDeploymentColorEnum();
   }

   void execute() throws Exception {
      int port = NetUtils.getFirstAvailablePort(portRange);

      if (port != -1) {
         SwarmDeployment swarmDeployment = executeSwarmProcess(port);

         if (swarmDeployment != null) {
            ctx.setDeployment(swarmJob.getContainerName(), colorToDeploy, swarmDeployment);
            LOG.info("Swarm started!");
            if (colorToRemove != null) {
               shutdownOldDeployment();
               ctx.clearDeployment(swarmJob.getContainerName(), colorToRemove);
            }
         } else {
            LOG.error("Swarm could not be started! See log rest file!");
         }
      } else {
         String errMsg = String.format("No available ports in the range %s", portRange.toString());
         LOG.error(errMsg);
      }
   }

   private SwarmDeployment executeSwarmProcess(int port) throws IOException {
      File copiedJarFile = swarmJob.getSwarmJarFile();
      LOG.info("Starting rest: [{}]", copiedJarFile.getAbsolutePath());
      String windowTitle = String.format("%s %s [jar: %s, port: %d]", swarmJob.getContainerName(),
                                         colorToDeploy.toString(), copiedJarFile.getName(),
                                         port);
      String jvmArgs = containerCfg.getJvmParams();
      String appArgs = "";

      // SwarmFile swarmJarFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
      long timeStarted = System.currentTimeMillis();
      String[] swarmCommand = SwarmUtil.createSwarmCliArguments(windowTitle,
                                                                Integer.toString(port),
                                                                jvmArgs,
                                                                timeStarted,
                                                                appArgs,
                                                                copiedJarFile);
      SwarmUtil.startSwarmInstance(swarmCommand);
      String serviceId = getServiceId(port);
      boolean registeredSuccessful = SwarmUtil.waitForServiceRegistration(containerCfg.getConsulUrl(),
                                                                          containerCfg.getConsulServiceName(),
                                                                          serviceId,
                                                                          300, 1000);
      SwarmDeployment resultDeployment = null;
      if (registeredSuccessful) {
         int pid = SwarmUtil.getSwarmPID(copiedJarFile.getName(), timeStarted);
         resultDeployment = SwarmDeployment.builder()
                                           .deploymentColor(colorToDeploy)
                                           .file(copiedJarFile)
                                           .pid(pid)
                                           .windowTitle(windowTitle)
                                           .build();
      }

      return resultDeployment;
   }

   private void shutdownOldDeployment() {
      for (int index = 0; index < containerCfg.swarmDeploymentCfgsSize(); index++) {
         SwarmDeploymentCfg deploymentCfg   = containerCfg.getSwarmDeploymentCfg(index);
         int                pid             = deploymentCfg.getPid();
         boolean            processSigInted = (pid != -1) && SwarmUtil.sigIntSwarm(pid);
         boolean            swarmExited     = false;
         if (processSigInted) {
            swarmExited = SwarmUtil.waitUntilSwarmProcExits(deploymentCfg.getPid(), 300, 1000);
         }
         if (!swarmExited) {
            String windowTitle = deploymentCfg.getWindowTitle();
            String errMsg = String.format(SWARM_DEPLOYMENT_COULD_NOT_BE_STOPPED,
                                          windowTitle);
            LOG.error(errMsg);
            LOG.info("Trying hard killing window [{}]", windowTitle);
            SwarmUtil.killSwarmWindow(windowTitle);
         }
      }
   }

   private String getServiceId(int port) {
      return containerCfg.getConsulServiceName() + ":127.0.0.1:" + port;
   }
}
