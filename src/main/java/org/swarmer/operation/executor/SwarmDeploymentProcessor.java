package org.swarmer.operation.executor;

import org.apache.commons.lang.math.IntRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.operation.SaveCtxStateToFile;
import org.swarmer.util.SwarmUtil;
import org.swarmer.util.SwarmerInputParams;

import java.io.File;
import java.io.IOException;

public abstract class SwarmDeploymentProcessor extends SwarmJobProcessor {
   protected static final int    DEFAULT_LOOP_WAIT_IN_MILLIS     = 1000;
   protected static final String DEPLOYMENT_COULD_NOT_BE_STOPPED = "Old rest deployment [WindowTitle: %s] could not be stopped! Hard killing window! Manual intervention needed!";
   private static final   Logger LOG                             = LoggerFactory.getLogger(
           SwarmDeploymentProcessor.class);

   // Local variables
   private final IntRange               portRange;
   private       DeploymentContainerCfg containerCfg;
   private       SwarmJob               swarmJob;

   public SwarmDeploymentProcessor(SwarmerCtx ctx) {
      super(ctx);
      this.portRange = getCtx().getPortRange();
   }

   @Override
   public SwarmJobProcessor init(SwarmJob swarmJob) {
      this.swarmJob = swarmJob;
      containerCfg = getCtx().getDeploymentContainerCfg(swarmJob.getContainerName());
      return this;
   }

   @Override
   public final void process() throws Exception {
      processImpl();
      saveConfigState();
   }

   protected abstract void processImpl() throws Exception;

   private void saveConfigState() {
      String filename = SwarmerInputParams.LAST_STATE_CFG_JSON_FILENAME;
      LOG.debug("Writing last state of Ctx into file [{}].", SwarmerInputParams.getConfigFile(filename));
      new SaveCtxStateToFile(filename, getCtx()).execute();
   }

   protected IntRange portRange() {
      return portRange;
   }

   protected void shutdownSwarmInstance(int pid, String windowTitle) {
      boolean processSigInted = (pid != -1) && SwarmUtil.sigIntSwarm(pid);
      boolean swarmExited     = false;
      if (processSigInted) {
         LOG.info("Trying to SIGINT process with PID [{}]", pid);
         int shutdownTimeout = getCtx().getGeneralCfgData().getLockWaitTimeout();
         swarmExited = SwarmUtil.waitUntilSwarmProcExits(pid, shutdownTimeout,
                                                         DEFAULT_LOOP_WAIT_IN_MILLIS);
      }
      if (!swarmExited) {
         String errMsg = String.format(DEPLOYMENT_COULD_NOT_BE_STOPPED,
                                       windowTitle);
         LOG.error(errMsg);
         LOG.info("Hard killing window [{}]", windowTitle);
         SwarmUtil.killSwarmWindow(windowTitle);
      } else {
         LOG.info("Process with PID [{}] successfully SIGINT-ed!", pid);
      }
   }

   protected SwarmDeployment startSwarmJar(int port, DeploymentColor colorToDeploy) throws IOException {
      File copiedJarFile = swarmJob().getSwarmJarFile();
      LOG.info("Starting Swarm Instance: [{}]", copiedJarFile.getAbsolutePath());
      String windowTitle = String.format("%s %s [jar: %s, port: %d]", swarmJob().getContainerName(),
                                         colorToDeploy.toString(), copiedJarFile.getName(),
                                         port);
      String jvmArgs = containerCfg().getJvmParams();
      String appArgs = containerCfg().getAppParams();

      // SwarmFile swarmJarFile = deploymentContainer.findFirstSwarmFileWithState(SwarmFile.State.COPIED);
      long timeStarted = System.currentTimeMillis();
      String[] swarmCommand = SwarmUtil.createSwarmCliArguments(windowTitle,
                                                                Integer.toString(port),
                                                                jvmArgs,
                                                                timeStarted,
                                                                appArgs,
                                                                copiedJarFile);
      SwarmUtil.startSwarmInstance(swarmCommand);
      String serviceId      = getServiceId(port);
      int    startupTimeout = getCtx().getGeneralCfgData().getLockWaitTimeout();
      boolean registeredSuccessful = SwarmUtil.waitForServiceRegistration(containerCfg().getConsulUrl(),
                                                                          containerCfg().getConsulServiceName(),
                                                                          serviceId,
                                                                          startupTimeout,
                                                                          DEFAULT_LOOP_WAIT_IN_MILLIS);
      SwarmDeployment resultDeployment = null;
      if (registeredSuccessful) {
         int pid = SwarmUtil.getSwarmPid(copiedJarFile.getName(), timeStarted);
         resultDeployment = SwarmDeployment.builder()
                                           .deploymentColor(colorToDeploy)
                                           .file(copiedJarFile)
                                           .pid(pid)
                                           .windowTitle(windowTitle)
                                           .build();
      }

      return resultDeployment;
   }

   protected SwarmJob swarmJob() {
      return swarmJob;
   }

   protected DeploymentContainerCfg containerCfg() {
      return containerCfg;
   }

   private String getServiceId(int port) {
      return containerCfg().getConsulServiceName() + ":127.0.0.1:" + port;
   }

}
