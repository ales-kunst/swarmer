package org.swarmer.context;

import org.apache.commons.lang.math.IntRange;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmerCfg;
import org.swarmer.util.CloseableUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class SwarmerCtx implements Closeable {
   // Locking objects
   private final Object CFG_DATA_LOCK             = new Object();
   private final Object DEPLOYMENT_CONTAINER_LOCK = new Object();
   private final Object SWARM_JOBS_LOCK           = new Object();

   // Local vatiables
   private List<DeploymentContainer> deploymentContainers;
   private List<SwarmJob>            swarmJobs;
   private SwarmerCfg.GeneralData    swarmerCfgGeneralData;
   private WatchService              watchService;

   static Builder newBuilder(SwarmerCfg swarmerCfg) {
      return new Builder(swarmerCfg);
   }

   private SwarmerCtx(Builder builder) {
      this.deploymentContainers = builder.deploymentContainers;
      this.swarmerCfgGeneralData = builder.swarmerCfg.getGeneralData();
      this.watchService = builder.watchService;
      this.swarmJobs = new ArrayList<>();
   }

   public void addSwarmJob(SwarmJob swarrmJob) {
      if ((swarrmJob == null) || (swarrmJob.getAction() == null)) {
         ExceptionThrower.throwIllegalArgumentException("Action can not be null!");
      }
      synchronized (SWARM_JOBS_LOCK) {
         swarmJobs.add(swarrmJob);
      }
   }

   public void clearDeployment(String containerName, DeploymentColor color) {
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(containerName);
         if (result.isPresent()) {
            result.get().clearDeployment(color);
         } else {
            ExceptionThrower.throwIllegalArgumentException("Container " + containerName + " does not exist!");
         }
      }
   }

   private Optional<DeploymentContainer> searchDeploymentContainer(String name) {
      return deploymentContainers.stream().filter(container -> container.getName().equalsIgnoreCase(name)).findFirst();
   }

   public void clearDeploymentInProgress(String containerName) {
      Optional<DeploymentContainer> container = searchDeploymentContainer(containerName);
      container.ifPresent(DeploymentContainer::clearDeploymentInProgress);
   }

   public void clearFileSuccessfullyLocked(WatchKey watchKey) {
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         result.ifPresent(deploymentContainer -> deploymentContainer.setFileSuccessfullyLocked(false));
      }

   }

   private Optional<DeploymentContainer> searchDeploymentContainer(WatchKey watchKey) {
      return deploymentContainers.stream().filter(container -> container.containsKey(watchKey)).findFirst();
   }

   @Override
   public void close() {
      for (DeploymentContainer container : deploymentContainers) {
         CloseableUtil.close(container);
      }
      CloseableUtil.close(watchService);
   }

   public void deploymentInProgress(String containerName) {
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> container = searchDeploymentContainer(containerName);
         container.ifPresent(DeploymentContainer::deploymentInProgress);
      }
   }

   public String getContainerName(WatchKey watchKey) {
      String containerName = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         if (result.isPresent()) {
            containerName = result.get().getName();
         }
      }
      return containerName;
   }

   public DeploymentContainerCfg getDeploymentContainerCfg(String containerName) throws CloneNotSupportedException {
      DeploymentContainerCfg resultContainerCfg = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> container = searchDeploymentContainer(containerName);
         if (container.isPresent()) {
            resultContainerCfg = container.get().getDeploymentContainerCfg();
         }
      }
      return resultContainerCfg;
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

   public int getLockWaitTimeout() {
      synchronized (CFG_DATA_LOCK) {
         return swarmerCfgGeneralData.getLockWaitTimeout() != null ? swarmerCfgGeneralData.getLockWaitTimeout() : 3000;
      }
   }

   public int getPort() {
      synchronized (CFG_DATA_LOCK) {
         return swarmerCfgGeneralData.getServerPort() != null ? swarmerCfgGeneralData.getServerPort() : 10080;
      }
   }

   public IntRange getPortRange() {
      synchronized (CFG_DATA_LOCK) {
         int defaultLowerPort =
                 swarmerCfgGeneralData.getSwarmPortLower() != null ? swarmerCfgGeneralData.getSwarmPortLower() : 8000;
         int defaultUpperPort =
                 swarmerCfgGeneralData.getSwarmPortUpper() != null ? swarmerCfgGeneralData.getSwarmPortUpper() : 10000;

         return new IntRange(defaultLowerPort, defaultUpperPort);
      }
   }

   public boolean isDeploymentInProgress(String containerName) {
      boolean result = false;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> container = searchDeploymentContainer(containerName);
         if (container.isPresent()) {
            result = container.get().isDeploymentInProgress();
         }
      }
      return result;
   }

   public SwarmJob popSwarmJob(Set<SwarmJob.Action> validActions) {
      if (validActions == null || validActions.isEmpty()) {
         return null;
      }
      synchronized (SWARM_JOBS_LOCK) {
         SwarmJob resultSwarmJob = null;
         Optional<SwarmJob> firstElem = swarmJobs.stream().filter(sj -> validActions.contains(sj.getAction()))
                                                 .findFirst();
         if (firstElem.isPresent()) {
            resultSwarmJob = firstElem.get();
            swarmJobs.remove(resultSwarmJob);
         }
         return resultSwarmJob;
      }
   }

   public WatchService getWatchService() {
      return watchService;
   }

   public void setDeployment(String containerName, DeploymentColor color, SwarmDeployment swarmDeployment) {
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(containerName);
         if (result.isPresent()) {
            result.get().setDeployment(color, swarmDeployment);
         } else {
            ExceptionThrower.throwIllegalArgumentException("Container " + containerName + " does not exist!");
         }
      }
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

   public void setFileSuccessfullyLocked(WatchKey watchKey) {
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(watchKey);
         result.ifPresent(deploymentContainer -> deploymentContainer.setFileSuccessfullyLocked(true));
      }

   }

   SwarmerCfg getSwarmerCfg() throws CloneNotSupportedException {
      SwarmerCfg resultCfg;
      synchronized (CFG_DATA_LOCK) {
         SwarmerCfg.GeneralData       generalData              = (SwarmerCfg.GeneralData) swarmerCfgGeneralData.clone();
         List<DeploymentContainerCfg> deploymentContainersCfgs = getDeploymentContainersCfgs();
         resultCfg = new SwarmerCfg(generalData, deploymentContainersCfgs);
      }
      return resultCfg;
   }

   private List<DeploymentContainerCfg> getDeploymentContainersCfgs() throws CloneNotSupportedException {
      List<DeploymentContainerCfg> resultList = new ArrayList<>();
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         for (DeploymentContainer container : deploymentContainers) {
            resultList.add(container.getDeploymentContainerCfg());
         }
      }

      return resultList;
   }

   public static class Builder {
      private List<DeploymentContainer> deploymentContainers;
      private SwarmerCfg                swarmerCfg;
      private WatchService              watchService;

      private Builder(SwarmerCfg swarmerCfg) {
         deploymentContainers = new ArrayList<>();
         this.swarmerCfg = swarmerCfg;
      }

      public SwarmerCtx build() throws IOException, ValidationException {
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

         return new SwarmerCtx(this);
      }

      private void addDeploymentContainer(DeploymentContainer deploymentContainer) {
         deploymentContainers.add(deploymentContainer);
      }
   }
}
