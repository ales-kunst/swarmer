package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.*;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.SwarmerException;
import org.swarmer.util.FileUtil;
import org.swarmer.util.NetUtils;
import org.swarmer.util.SwarmExecutor;

import java.io.File;

public class SwarmDeploymentExecutor implements Runnable {
   private static final Logger              LOG = LogManager.getLogger(SwarmDeploymentExecutor.class);
   private final        DeploymentContainer deploymentContainer;
   private final        SwarmerContext      swarmerCtx;

   public SwarmDeploymentExecutor(DeploymentContainer deploymentContainer) {
      this.deploymentContainer = deploymentContainer;
      swarmerCtx = SwarmerContext.instance();
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

   private SwarmDeployment startDeploy(SwarmFile copiedSwarmFile,
                                       DeploymentColor colorToDeploy) throws SwarmerException {
      int             port            = NetUtils.getFirstAvailablePort(swarmerCtx.getPortRange());
      SwarmDeployment swarmDeployment = new SwarmDeployment(copiedSwarmFile, port);

      if (port == -1) {
         executeSwarmProcess(swarmDeployment, colorToDeploy);
         deploymentContainer.setDeployment(colorToDeploy, swarmDeployment);
      } else {
         String errMsg = String.format("No available ports in the range %s",
                                       swarmerCtx.getPortRange().toString());
         ExceptionThrower.throwSwarmerException(errMsg);
      }

      return swarmDeployment;
   }

   private boolean executeSwarmProcess(SwarmDeployment swarmDeployment, DeploymentColor colorToDeploy) {
      LOG.info("Starting swarm: [{}]", swarmDeployment.getSwarmFile().getAbsolutePath());
      new File(FileUtil.KILL_APP_PATH).delete();
      new File(FileUtil.WIN_TEE_APP_PATH).delete();
      FileUtil.copyWindowsKillAppToTmp();
      FileUtil.copyWinTeeAppToTmp();
      String windowTitle = deploymentContainer.getName() + " " + colorToDeploy.toString();
      String jvmArgs     = null;
      String appArgs     = "";
      if (colorToDeploy.equals(DeploymentColor.BLUE)) {
         jvmArgs = deploymentContainer.getSwarmConfig().getBlueJvmParams();
      } else {
         jvmArgs = deploymentContainer.getSwarmConfig().getGreenJvmParams();
      }
      File swarmJarFile = deploymentContainer.getTargetPath();
      String[] swarmCommand = SwarmExecutor.createSwarmCliArguments(windowTitle,
                                                                    Integer.toString(swarmDeployment.getPort()),
                                                                    jvmArgs,
                                                                    appArgs,
                                                                    swarmJarFile);
      Process swarmProcess = SwarmExecutor.startSwarmInstance(swarmCommand);
      swarmDeployment.setSwarmState(SwarmFile.State.STARTING_SWARM, null);

      return true;
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
}
