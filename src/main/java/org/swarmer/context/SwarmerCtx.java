package org.swarmer.context;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmerCfg;
import org.swarmer.util.CloseableUtil;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class SwarmerCtx implements Destroyable, CtxVisitableElement {
   private static final Logger LOG = LogManager.getLogger(SwarmerCtx.class);

   // Locking objects
   private final Object                    DEPLOYMENT_CONTAINER_LOCK = new Object();
   private final Object                    SWARM_JOBS_LOCK           = new Object();
   // Local vatiables
   private       List<DeploymentContainer> deploymentContainers;
   private       List<SwarmJob>            swarmJobs;
   private       SwarmerCfg.GeneralData    swarmerCfgGeneralData;
   private       WatchService              watchService;

   static Builder newBuilder(SwarmerCfg swarmerCfg) {
      return new Builder(swarmerCfg);
   }

   private SwarmerCtx(Builder builder) {
      this.deploymentContainers = builder.deploymentContainers;
      this.swarmerCfgGeneralData = builder.swarmerCfg.getGeneralData();
      this.watchService = builder.watchService;
      this.swarmJobs = new ArrayList<>();
   }

   public void addDeployment(String containerName, SwarmDeployment swarmDeployment) {
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> result = searchDeploymentContainer(containerName);
         if (result.isPresent()) {
            result.get().addDeployment(swarmDeployment.deploymentColor(), swarmDeployment);
         } else {
            ExceptionThrower.throwIllegalArgumentException("Container " + containerName + " does not exist!");
         }
      }
   }

   public void addSwarmJob(SwarmJob swarrmJob) {
      if ((swarrmJob == null) || (swarrmJob.getAction() == null)) {
         ExceptionThrower.throwIllegalArgumentException("Action can not be null!");
      }
      synchronized (SWARM_JOBS_LOCK) {
         swarmJobs.add(swarrmJob);
      }
   }

   public boolean removeDeployment(String containerName, int pid) {
      boolean                       resultSuccess = false;
      Optional<DeploymentContainer> container     = searchDeploymentContainer(containerName);
      if (container.isPresent()) {
         DeploymentColor color = container.get().currentDeploymentColor();
         if (color != null) {
            resultSuccess = container.get().removeSwarmDeployment(color, pid);
         }
      }
      return resultSuccess;
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
      return deploymentContainers.stream()
                                 .filter(container ->
                                                 container.deploymentContainerCfg().getName().equalsIgnoreCase(name))
                                 .findFirst();
   }

   public void clearDeploymentInProgress(String containerName) {
      Optional<DeploymentContainer> container = searchDeploymentContainer(containerName);
      container.ifPresent(DeploymentContainer::clearDeploymentInProgress);
   }

   public void deploymentInProgress(String containerName) {
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> container = searchDeploymentContainer(containerName);
         container.ifPresent(DeploymentContainer::deploymentInProgress);
      }
   }

   @Override
   public void destroy() {
      for (DeploymentContainer container : deploymentContainers) {
         container.destroy();
      }
      CloseableUtil.close(watchService);
   }

   public SwarmerCfg getCfg() {
      CfgCreator cfgCreator = new CfgCreator();
      try {
         visit(cfgCreator);
      } catch (Exception e) {
         ExceptionThrower.throwRuntimeError(e);
      }
      return cfgCreator.getData();
   }

   @Override
   public void visit(CtxElementVisitor visitor) throws Exception {
      visitor.visit(this);
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         for (DeploymentContainer container : deploymentContainers) {
            container.visit(visitor);
         }
      }
   }

   public DeploymentContainerCfg getDeploymentContainerCfg(String containerName) {
      DeploymentContainerCfg resultContainerCfg = null;
      synchronized (DEPLOYMENT_CONTAINER_LOCK) {
         Optional<DeploymentContainer> container = searchDeploymentContainer(containerName);
         if (container.isPresent()) {
            try {
               resultContainerCfg = container.get().getDeploymentContainerCfg();
            } catch (Exception e) {
               ExceptionThrower.throwRuntimeError(e);
            }
         }
      }
      return resultContainerCfg;
   }

   public int getLockWaitTimeout() {
      return swarmerCfgGeneralData.getLockWaitTimeout() != null ? swarmerCfgGeneralData.getLockWaitTimeout() : 3000;
   }

   public int getPort() {
      return swarmerCfgGeneralData.getServerPort() != null ? swarmerCfgGeneralData.getServerPort() : 10080;
   }

   public IntRange getPortRange() {
      int defaultLowerPort =
              swarmerCfgGeneralData.getSwarmPortLower() != null ? swarmerCfgGeneralData.getSwarmPortLower() : 8000;
      int defaultUpperPort =
              swarmerCfgGeneralData.getSwarmPortUpper() != null ? swarmerCfgGeneralData.getSwarmPortUpper() : 10000;

      return new IntRange(defaultLowerPort, defaultUpperPort);
   }

   public SwarmerCfg.GeneralData getGeneralCfgData() {
      try {
         SwarmerCfg.GeneralData generalData = (SwarmerCfg.GeneralData) swarmerCfgGeneralData.clone();
         return generalData;
      } catch (Exception e) {
         LOG.error("Error in getGeneralCfgData: {}", ExceptionUtils.getFullStackTrace(e));
      }
      return null;
   }

   public WatchService getWatchService() {
      return watchService;
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

   public SwarmJob popSwarmJob() {
      synchronized (SWARM_JOBS_LOCK) {
         if (swarmJobs.isEmpty()) {
            return null;
         }
         SwarmJob resultFirstElem = swarmJobs.get(0);
         swarmJobs.remove(resultFirstElem);
         return resultFirstElem;
      }
   }

   SwarmerCfg.GeneralData swarmerCfgGeneralData() {
      return swarmerCfgGeneralData;
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
