package org.swarmer.context;

import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmDeploymentCfg;

import java.io.Closeable;
import java.io.File;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;

public class DeploymentContainer implements Closeable {

   // Locks
   private final Object DEPLOYMENT_IN_PROGRESS_LOCK = new Object();

   // Local variables
   private DeploymentContainerCfg                deploymentContainerCfg;
   private boolean                               deploymentInProgress;
   private boolean                               fileSuccessfullyLocked;
   private Map<DeploymentColor, SwarmDeployment> swarmDeployments;
   private WatchKey                              watchKey;

   DeploymentContainer(DeploymentContainerCfg deploymentContainerCfg) {
      this.deploymentContainerCfg = deploymentContainerCfg;
      this.fileSuccessfullyLocked = false;
      this.deploymentInProgress = false;
      initSwarmDeployment();
   }

   private void initSwarmDeployment() {
      this.swarmDeployments = new HashMap<DeploymentColor, SwarmDeployment>() {{
         put(DeploymentColor.BLUE, null);
         put(DeploymentColor.GREEN, null);
      }};
      SwarmDeploymentCfg swarmDeploymentCfg = deploymentContainerCfg.getSwarmDeploymentCfg(0);
      if (swarmDeploymentCfg != null) {
         if (swarmDeploymentCfg.isGreenDeployment()) {
            setDeployment(DeploymentColor.GREEN, SwarmDeployment.builder(swarmDeploymentCfg).build());
         } else if (swarmDeploymentCfg.isBlueDeployment()) {
            setDeployment(DeploymentColor.BLUE, SwarmDeployment.builder(swarmDeploymentCfg).build());
         }
      }
   }

   void setDeployment(DeploymentColor color, SwarmDeployment swarmDeployment) {
      swarmDeployments.put(color, swarmDeployment);
   }

   @Override
   public void close() {
      deploymentContainerCfg = null;
      watchKey.cancel();
      swarmDeployments.clear();
   }

   void clearDeployment(DeploymentColor color) {
      swarmDeployments.put(color, null);
   }

   void clearDeploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         this.deploymentInProgress = false;
      }
   }

   boolean containsKey(WatchKey watchKey) {
      return this.watchKey.equals(watchKey);
   }

   void deploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         this.deploymentInProgress = true;
      }
   }

   DeploymentContainerCfg getDeploymentContainerCfg() throws CloneNotSupportedException {
      DeploymentContainerCfg resultContainerCfg = (DeploymentContainerCfg) deploymentContainerCfg.clone();
      resultContainerCfg.clearSwarmDeploymentList();
      SwarmDeployment swarmDeployment = getRunningSwarmDeployment();
      if (swarmDeployment != null) {
         resultContainerCfg.addSwarmDeploymentCfg(swarmDeployment.getSwarmerDeploymentCfg());
      }

      return resultContainerCfg;
   }

   File getDestFolder() {
      return deploymentContainerCfg.getDestFolder();
   }

   String getFilePattern() {
      return deploymentContainerCfg.getFilePattern();
   }

   String getName() {
      return deploymentContainerCfg.getName();
   }

   boolean isDeploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         return deploymentInProgress;
      }
   }

   boolean isFileSuccessfullyLocked() {
      return fileSuccessfullyLocked;
   }

   void setFileSuccessfullyLocked(boolean fileSuccessfullyLocked) {
      this.fileSuccessfullyLocked = fileSuccessfullyLocked;
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

   File getSourcePath() { return deploymentContainerCfg.getSrcFolder(); }

   private File getTargetPath() { return deploymentContainerCfg.getDestFolder(); }

   void setWatchKey(WatchKey watchKey) {
      this.watchKey = watchKey;
   }

   private boolean areAllDeploymentColorsFree() {
      boolean result = false;
      if (isDeploymentFree(DeploymentColor.BLUE) && isDeploymentFree(DeploymentColor.GREEN)) {
         result = true;
      }
      return result;
   }

   private DeploymentColor currentDeploymentColor() {
      DeploymentColor resultColor = null;

      if (areAllDeploymentColorsFree()) {
         return null;
      } else if (!isDeploymentFree(DeploymentColor.BLUE)) {
         resultColor = DeploymentColor.BLUE;
      } else if (!isDeploymentFree(DeploymentColor.GREEN)) {
         resultColor = DeploymentColor.GREEN;
      }

      return resultColor;
   }

   private SwarmDeployment getDeployment(DeploymentColor color) {
      return swarmDeployments.get(color);
   }

   private SwarmDeployment getRunningSwarmDeployment() {
      SwarmDeployment resultSwarmDeployment = null;
      DeploymentColor deploymentColor       = currentDeploymentColor();
      if (deploymentColor != null) {
         resultSwarmDeployment = getDeployment(deploymentColor);
      }

      return resultSwarmDeployment;
   }

   private boolean isDeploymentFree(DeploymentColor color) {
      return getDeployment(color) == null;
   }
}
