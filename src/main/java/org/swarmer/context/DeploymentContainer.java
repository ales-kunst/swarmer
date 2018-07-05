package org.swarmer.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.util.SwarmUtil;

import java.io.File;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentContainer implements Destroyable, CtxVisitableElement {
   private static final Logger LOG = LoggerFactory.getLogger(DeploymentContainer.class);

   // Locks
   private final Object DEPLOYMENT_IN_PROGRESS_LOCK = new Object();

   // Local variables
   private DeploymentContainerCfg                      deploymentContainerCfg;
   private boolean                                     deploymentInProgress;
   private Map<DeploymentColor, List<SwarmDeployment>> swarmDeployments;
   private WatchKey                                    watchKey;

   DeploymentContainer(DeploymentContainerCfg deploymentContainerCfg) {
      this.deploymentContainerCfg = deploymentContainerCfg;
      this.deploymentInProgress = false;
      initSwarmDeployment();
   }

   private void initSwarmDeployment() {
      this.swarmDeployments = new HashMap<>();
      swarmDeployments.put(DeploymentColor.BLUE, new ArrayList<>());
      swarmDeployments.put(DeploymentColor.GREEN, new ArrayList<>());
      for (int index = 0; index < deploymentContainerCfg.swarmDeploymentCfgsSize(); index++) {
         SwarmDeploymentCfg deploymentCfg = deploymentContainerCfg.getSwarmDeploymentCfg(index);
         int                pid           = deploymentCfg.getPid();
         if ((pid >= -1) && SwarmUtil.pidExists(pid)) {
            LOG.debug("Swarm deployment with PID {} was added to container {}", pid, deploymentContainerCfg.getName());
            addDeployment(deploymentCfg.getDeploymentColorEnum(), SwarmDeployment.builder(deploymentCfg).build());
         } else {
            LOG.warn("Swarm deployment with PID {} was NOT added to container {}", pid,
                     deploymentContainerCfg.getName());
         }
      }
   }

   void addDeployment(DeploymentColor color, SwarmDeployment swarmDeployment) {
      swarmDeployments.get(color).add(swarmDeployment);
   }

   @Override
   public void destroy() {
      deploymentContainerCfg = null;
      watchKey.cancel();
      for (Map.Entry<DeploymentColor, List<SwarmDeployment>> entry : swarmDeployments.entrySet()) {
         entry.getValue().clear();
      }
      swarmDeployments.clear();
   }

   @Override
   public void visit(CtxElementVisitor visitor) throws Exception {
      visitor.visit(this);
      for (Map.Entry<DeploymentColor, List<SwarmDeployment>> entry : swarmDeployments.entrySet()) {
         List<SwarmDeployment> deployments = entry.getValue();
         for (SwarmDeployment deployment : deployments) {
            deployment.visit(visitor);
         }
      }
   }

   void clearDeployment(DeploymentColor color) {
      swarmDeployments.get(color).clear();
   }

   void clearDeploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         this.deploymentInProgress = false;
      }
   }

   DeploymentColor currentDeploymentColor() {
      DeploymentColor color = null;
      if (!swarmDeployments.get(DeploymentColor.BLUE).isEmpty()) {
         color = DeploymentColor.BLUE;
      } else if (!swarmDeployments.get(DeploymentColor.GREEN).isEmpty()) {
         color = DeploymentColor.GREEN;
      }
      return color;
   }

   DeploymentContainerCfg deploymentContainerCfg() {
      return deploymentContainerCfg;
   }

   void deploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         this.deploymentInProgress = true;
      }
   }

   DeploymentContainerCfg getDeploymentContainerCfg() throws CloneNotSupportedException {
      DeploymentContainerCfg resultContainerCfg = (DeploymentContainerCfg) deploymentContainerCfg.clone();
      resultContainerCfg.clearSwarmDeploymentList();

      for (Map.Entry<DeploymentColor, List<SwarmDeployment>> entry : swarmDeployments.entrySet()) {
         List<SwarmDeployment> deployments = entry.getValue();
         for (SwarmDeployment deployment : deployments) {
            resultContainerCfg.addSwarmDeploymentCfg(deployment.getSwarmerDeploymentCfg());
         }
      }

      return resultContainerCfg;
   }

   boolean isDeploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         return deploymentInProgress;
      }
   }

   void isValid() throws ValidationException {
      StringBuilder errMsgs = new StringBuilder();
      if (getSourcePath().getAbsolutePath().equalsIgnoreCase(getTargetPath().getAbsolutePath())) {
         errMsgs.append(String.format("Source and target folder are same [%s = %s]", getSourcePath(), getTargetPath()));
      }

      // Only throw error if there are no text messages in the string builder.
      if (!errMsgs.toString().isEmpty()) {
         ExceptionThrower.throwValidationException(errMsgs.toString());
      }
   }

   File getSourcePath() {
      return deploymentContainerCfg.getSrcFolder();
   }

   private File getTargetPath() {
      return deploymentContainerCfg.getDestFolder();
   }

   boolean removeSwarmDeployment(DeploymentColor color, int pid) {
      boolean               resultSuccess = false;
      List<SwarmDeployment> deployments   = swarmDeployments.get(color);
      for (SwarmDeployment deployment : deployments) {
         if (deployment.pid() == pid) {
            deployments.remove(deployment);
            resultSuccess = true;
            break;
         }
      }
      return resultSuccess;
   }

   void setWatchKey(WatchKey watchKey) {
      this.watchKey = watchKey;
   }

   String watchKeyHash() {
      return Integer.toString(watchKey.hashCode());
   }
}
