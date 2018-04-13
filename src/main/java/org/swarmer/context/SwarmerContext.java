package org.swarmer.context;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class SwarmerContext {
   private static final Logger         LOG                           = LogManager.getLogger(SwarmerContext.class);
   private static final String         SETTING_JAVA_PATH             = "java.path";
   private static final String         SETTING_LOCK_WAIT_TIMEOUT     = "lock.wait.timeout";
   private static final String         SETTING_SWARM_PORT_LOWER      = "swarm.port.lower";
   private static final String         SETTING_SWARM_PORT_UPPER      = "swarm.port.upper";
   private static final String         SETTING_SWARM_STARTUP_TIMEOUT = "swarm.default.startup.time";
   private static       SwarmerContext ctxInstance                   = null;

   protected Ini.Section               defaultSection;
   protected List<DeploymentContainer> deploymentContainers;
   protected WatchService              watchService;

   public static SwarmerContext instance() {
      return ctxInstance;
   }

   static Builder newBuilder() {
      return new Builder();
   }

   static void reset(SwarmerContext ctxInstance) {
      SwarmerContext.ctxInstance = ctxInstance;
   }

   private SwarmerContext() {
      this.deploymentContainers = new ArrayList<DeploymentContainer>();
   }

   private SwarmerContext(Builder builder) {
      this.deploymentContainers = builder.deploymentContainers;
      this.defaultSection = builder.defaultSection;
      this.watchService = builder.watchService;
   }

   public boolean addDeploymentContainer(DeploymentContainer deploymentContainer) throws IOException {
      Path     srcPath = deploymentContainer.getSourcePath().toPath();
      WatchKey key     = srcPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
      deploymentContainer.setWatchKey(key);
      return deploymentContainers.add(deploymentContainer);
   }

   public boolean clearFileSuccessfullyLocked(WatchKey watchKey) {
      boolean                       fileSuccessfullyLockedCleared = false;
      Optional<DeploymentContainer> result                        = searchDeploymentContainer(watchKey);
      if (result.isPresent()) {
         fileSuccessfullyLockedCleared = true;
         result.get().setFileSuccessfullyLocked(false);
      }
      return fileSuccessfullyLockedCleared;
   }

   private Optional<DeploymentContainer> searchDeploymentContainer(WatchKey watchKey) {
      return deploymentContainers.stream().filter(container -> container.containsKey(watchKey)).findFirst();
   }

   public DeploymentContainer getDeploymentContainer(WatchKey watchKey) {
      DeploymentContainer           resultContainer = null;
      Optional<DeploymentContainer> result          = searchDeploymentContainer(watchKey);
      if (result.isPresent()) {
         resultContainer = result.get();
      }
      return resultContainer;
   }

   public DeploymentContainer[] getDeploymentContainers() {
      DeploymentContainer[] resultArray = new DeploymentContainer[deploymentContainers.size()];
      return deploymentContainers.toArray(resultArray);
   }

   public String getJavaPath() {
      String javaPathValue = defaultSection.get(SETTING_JAVA_PATH);
      return javaPathValue;
   }

   public int getLockWaitTimeout() {
      int    lockWaitTimeout      = 3000;
      String lockWaitTimeoutValue = defaultSection.get(SETTING_LOCK_WAIT_TIMEOUT);
      if (lockWaitTimeoutValue != null) {
         lockWaitTimeout = Integer.valueOf(lockWaitTimeoutValue);
      }

      return lockWaitTimeout;
   }

   public IntRange getPortRange() {
      int defaultLowerPort = 8000;
      int defaultUpperPort = 10000;

      defaultLowerPort = Integer.parseInt(defaultSection.get(SETTING_SWARM_PORT_LOWER, "8000"));
      defaultUpperPort = Integer.parseInt(defaultSection.get(SETTING_SWARM_PORT_UPPER, "10000"));
      return new IntRange(defaultLowerPort, defaultUpperPort);
   }

   public SwarmConfig getSwarmConfig(WatchKey watchKey) {
      SwarmConfig                   resultConfig = null;
      Optional<DeploymentContainer> result       = searchDeploymentContainer(watchKey);
      if (result.isPresent()) {
         resultConfig = result.get().getSwarmConfig();
      }
      return resultConfig;
   }

   public int getSwarmStartupTimeout() {
      return Integer.parseInt(defaultSection.get(SETTING_SWARM_STARTUP_TIMEOUT, "300"));
   }

   public WatchService getWatchService() {
      return watchService;
   }

   public boolean isFileSuccessfullyLocked(WatchKey watchKey) {
      boolean                       fileSuccessfullyLocked = false;
      Optional<DeploymentContainer> result                 = searchDeploymentContainer(watchKey);
      if (result.isPresent()) {
         fileSuccessfullyLocked = result.get().isFileSuccessfullyLocked();
      }
      return fileSuccessfullyLocked;
   }

   public boolean setFileSuccessfullyLocked(WatchKey watchKey) {
      boolean                       fileSuccessfullyLockedSet = false;
      Optional<DeploymentContainer> result                    = searchDeploymentContainer(watchKey);
      if (result.isPresent()) {
         fileSuccessfullyLockedSet = true;
         result.get().setFileSuccessfullyLocked(true);
      }
      return fileSuccessfullyLockedSet;
   }

   public static class Builder {

      private Ini.Section               defaultSection;
      private List<DeploymentContainer> deploymentContainers;
      private WatchService              watchService;

      private Builder() {
         deploymentContainers = new ArrayList<>();
      }

      public Builder addDeploymentContainer(DeploymentContainer deploymentContainer) {
         deploymentContainers.add(deploymentContainer);
         return this;
      }

      public SwarmerContext build() throws IOException {
         watchService = FileSystems.getDefault().newWatchService();
         for (DeploymentContainer deploymentContainer : deploymentContainers) {
            Path     srcPath = deploymentContainer.getSourcePath().toPath();
            WatchKey key     = srcPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            deploymentContainer.setWatchKey(key);
         }
         return new SwarmerContext(this);
      }

      public Builder setDefaultSection(Ini.Section defaultSection) {
         this.defaultSection = defaultSection;
         return this;
      }

   }
}
