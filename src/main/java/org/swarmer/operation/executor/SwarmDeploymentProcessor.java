package org.swarmer.operation.executor;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.DeploymentColor;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.util.SwarmUtil;

import java.io.File;
import java.io.IOException;

public abstract class SwarmDeploymentProcessor extends SwarmJobProcessor {
   protected static final int    DEFAULT_LOOP_WAIT_IN_MILLIS = 1000;
   private static final   Logger LOG                         = LogManager.getLogger(SwarmDeploymentProcessor.class);

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

   protected IntRange portRange() {
      return portRange;
   }

   protected SwarmDeployment startSwarmJar(int port, DeploymentColor colorToDeploy) throws IOException {
      File copiedJarFile = swarmJob().getSwarmJarFile();
      LOG.info("Starting rest: [{}]", copiedJarFile.getAbsolutePath());
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
