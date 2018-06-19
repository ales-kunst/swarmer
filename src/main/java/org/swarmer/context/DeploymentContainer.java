package org.swarmer.context;

import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmDeploymentCfg;

import java.io.File;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentContainer implements Destroyable, CtxVisitableElement {

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

   DeploymentContainerCfg deploymentContainerCfg() {
      return deploymentContainerCfg;
   }

   String watchKeyHash() {
      return Integer.toString(watchKey.hashCode());
   }

   private void initSwarmDeployment() {
      this.swarmDeployments = new HashMap<>();
      swarmDeployments.put(DeploymentColor.BLUE, new ArrayList<>());
      swarmDeployments.put(DeploymentColor.GREEN, new ArrayList<>());
      for (int index = 0; index < deploymentContainerCfg.swarmDeploymentCfgsSize(); index++) {
         SwarmDeploymentCfg deploymentCfg = deploymentContainerCfg.getSwarmDeploymentCfg(index);
         addDeployment(deploymentCfg.getDeploymentColorEnum(), SwarmDeployment.builder(deploymentCfg).build());
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

   void clearDeployment(DeploymentColor color) {
      swarmDeployments.get(color).clear();
   }

   void clearDeploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         this.deploymentInProgress = false;
      }
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

   void setWatchKey(WatchKey watchKey) {
      this.watchKey = watchKey;
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
}
