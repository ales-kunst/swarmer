package org.swarmer.operation.executor;

import org.apache.commons.lang.math.IntRange;
import org.javatuples.Pair;
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
   private static final Logger LOG = LoggerFactory.getLogger(
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

   protected void shutdownSwarmInstance(String consulUrl, String serviceName, int pid, String windowTitle) {
      boolean swarmExited     = false;
      boolean processSigInted = SwarmUtil.pidExists(pid) && SwarmUtil.sigIntSwarm(pid);

      if (processSigInted) {
         int shutdownTimeout = getCtx().getGeneralCfgData().getLockWaitTimeout();
         swarmExited = SwarmUtil.waitUntilSwarmProcExits(pid, shutdownTimeout);
      }
      if (!swarmExited) {
         String warnMsg = "Process with PID [{}] could not be stopped."
                          + " Hard killing window [{}].";
         LOG.warn(warnMsg, pid, windowTitle);
         SwarmUtil.killSwarmWindow(windowTitle);
         SwarmUtil.waitForCriticalServicesDeregistration(consulUrl, serviceName, 10);
      } else {
         LOG.info("SIGINT successfully sent to process with PID [{}].", pid);
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

      long timeStarted = System.currentTimeMillis();
      String[] swarmCommand = SwarmUtil.createSwarmCliArguments(windowTitle,
                                                                Integer.toString(port),
                                                                jvmArgs,
                                                                timeStarted,
                                                                appArgs,
                                                                copiedJarFile);
      Pair<Process, String> runSwarmInstanceResult = SwarmUtil.startSwarmInstance(swarmCommand);
      String                serviceId              = getServiceId(port);
      int                   startupTimeout         = getCtx().getGeneralCfgData().getSwarmDefaultStartupTime();
      boolean registeredSuccessful = SwarmUtil.waitForServiceRegistration(containerCfg().getConsulUrl(),
                                                                          containerCfg().getConsulServiceName(),
                                                                          serviceId, startupTimeout);
      SwarmDeployment resultDeployment = null;
      if (registeredSuccessful) {
         int pid = SwarmUtil.getSwarmPid(copiedJarFile.getName(), timeStarted);
         LOG.info("Swarm started [PID: {}; WindowTitle: {}].", pid, windowTitle);
         resultDeployment = SwarmDeployment.builder()
                                           .deploymentColor(colorToDeploy)
                                           .file(copiedJarFile)
                                           .pid(pid)
                                           .windowTitle(windowTitle)
                                           .build();
      } else {
         String teeLogFile = runSwarmInstanceResult.getValue1();
         LOG.warn("Swarm could not be started! See Swarm log file [{}]!", teeLogFile);
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
