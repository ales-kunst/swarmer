package org.swarmer.context;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmerCfg;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class SwarmerContext {
   private static final Object         DEPLOYMENT_CONTAINER_LOCK     = new Object();
   private static final Logger         LOG                           = LogManager.getLogger(SwarmerContext.class);

   protected List<DeploymentContainer> deploymentContainers;
   protected WatchService              watchService;
   private   SwarmerCfg.GeneralData    swarmerCfgGeneralData;

   static Builder newBuilder(SwarmerCfg swarmerCfg) {
      return new Builder(swarmerCfg);
   }

   private SwarmerContext(Builder builder) {
      this.deploymentContainers = builder.deploymentContainers;
      this.swarmerCfgGeneralData = builder.swarmerCfg.getGeneralData();
      this.watchService = builder.watchService;
   }

   public boolean addDeploymentContainer(DeploymentContainer deploymentContainer) throws IOException {
      boolean added = false;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Path     srcPath = deploymentContainer.getSourcePath().toPath();
         WatchKey key     = srcPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
         deploymentContainer.setWatchKey(key);
         String filenamePattern = deploymentContainer.getFilenamePattern();
         LOG.info("Added deployment container [name: {}, pattern: {}, source path: {}, target path: {}]",
                  deploymentContainer.getName(), filenamePattern, deploymentContainer.getSourcePath(),
                  deploymentContainer.getTargetPath());
         added = deploymentContainers.add(deploymentContainer);
      }

      return added;
   }

   public boolean clearFileSuccessfullyLocked(WatchKey watchKey) {
      boolean fileSuccessfullyLockedCleared = false;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            fileSuccessfullyLockedCleared = true;
            result.get().setFileSuccessfullyLocked(false);
         }
      }

      return fileSuccessfullyLockedCleared;
   }

   private Optional<DeploymentContainer> searchDeploymentContainer(WatchKey watchKey) {
      return deploymentContainers.stream().filter(container -> container.containsKey(watchKey)).findFirst();
   }

   public DeploymentContainer getDeploymentContainer(WatchKey watchKey) {
      DeploymentContainer resultContainer = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            resultContainer = result.get();
         }
      }

      return resultContainer;
   }

   public DeploymentContainer[] getDeploymentContainers() {
      DeploymentContainer[] resultArray = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         resultArray = new DeploymentContainer[deploymentContainers.size()];
         deploymentContainers.toArray(resultArray);
      }

      return resultArray;
   }

   public File getDestFolder(WatchKey watchKey) {
      File destFolder = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            destFolder = result.get().getDestFolder();
         }
      }
      return destFolder;
   }

   public String getFilePattern(WatchKey watchKey) {
      String filePattern = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            filePattern = result.get().getFilePattern();
         }
      }
      return filePattern;
   }

   /*
   public SwarmConfig getSwarmConfig(WatchKey watchKey) {
      SwarmConfig resultConfig = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            resultConfig = result.get().getSwarmConfig();
         }
      }

      return resultConfig;
   }
   */

   public int getLockWaitTimeout() {
      int     lockWaitTimeout      = 3000;
      Integer lockWaitTimeoutValue = swarmerCfgGeneralData.getLockWaitTimeout();
      if (lockWaitTimeoutValue != null) {
         lockWaitTimeout = lockWaitTimeoutValue.intValue();
      }

      return lockWaitTimeout;
   }

   public IntRange getPortRange() {
      int defaultLowerPort =
              swarmerCfgGeneralData.getSwarmPortLower() != null ? swarmerCfgGeneralData.getSwarmPortLower() : 8000;
      int defaultUpperPort =
              swarmerCfgGeneralData.getSwarmPortUpper() != null ? swarmerCfgGeneralData.getSwarmPortUpper() : 10000;

      return new IntRange(defaultLowerPort, defaultUpperPort);
   }

   public int getSwarmStartupTimeout() {
      return swarmerCfgGeneralData.getSwarmDefaultStartupTime() != null ? swarmerCfgGeneralData
              .getSwarmDefaultStartupTime() : 300;
   }

   public WatchService getWatchService() {
      return watchService;
   }

   public boolean isFileSuccessfullyLocked(WatchKey watchKey) {
      boolean fileSuccessfullyLocked = false;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            fileSuccessfullyLocked = result.get().isFileSuccessfullyLocked();
         }
      }

      return fileSuccessfullyLocked;
   }

   public boolean setFileSuccessfullyLocked(WatchKey watchKey) {
      boolean fileSuccessfullyLockedSet = false;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            fileSuccessfullyLockedSet = true;
            result.get().setFileSuccessfullyLocked(true);
         }
      }

      return fileSuccessfullyLockedSet;
   }

   public static class Builder {
      private List<DeploymentContainer> deploymentContainers;
      private SwarmerCfg                swarmerCfg;
      private WatchService              watchService;

      private Builder(SwarmerCfg swarmerCfg) {
         this();
         this.swarmerCfg = swarmerCfg;
      }

      private Builder() {
         deploymentContainers = new ArrayList<>();
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

      public SwarmerContext buildFromCfg() throws IOException, ValidationException {
         watchService = FileSystems.getDefault().newWatchService();
         int elem_num = swarmerCfg.deploymentContainerCfgsSize();
         for (int index = 0; index < elem_num; index++) {
            DeploymentContainerCfg deploymentContainerCfg = swarmerCfg.getDeploymentContainerCfg(index);
            DeploymentContainer    deploymentContainer    = new DeploymentContainer(deploymentContainerCfg);
            deploymentContainer.isValid();
            Path     srcPath = deploymentContainer.getSourcePath().toPath();
            WatchKey key     = srcPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            deploymentContainer.setWatchKey(key);
            addDeploymentContainer(deploymentContainer);
         }

         return new SwarmerContext(this);
      }

      public Builder addDeploymentContainer(DeploymentContainer deploymentContainer) {
         deploymentContainers.add(deploymentContainer);
         return this;
      }
   }
}
