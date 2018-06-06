package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.DeploymentContainerCfg;

import java.io.File;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentContainer {

   private static final Logger                 LOG                         = LogManager.getLogger(
           DeploymentContainer.class);
   private final        Object                 DEPLOYMENT_IN_PROGRESS_LOCK = new Object();
   private final        Object                 FILES_QUEUE_LOCK            = new Object();
   private final        DeploymentContainerCfg deploymentContainerCfg;

   private boolean                               deploymentInProgress;
   private boolean                               fileSuccessfullyLocked;
   private Map<DeploymentColor, SwarmDeployment> swarmDeployments;
   private List<SwarmFile>                       swarmFilesQueue;
   private WatchKey                              watchKey;

   public DeploymentContainer(DeploymentContainerCfg deploymentContainerCfg) {
      this.fileSuccessfullyLocked = false;
      this.swarmFilesQueue = new ArrayList<>();
      this.deploymentInProgress = false;
      this.swarmDeployments = new HashMap<DeploymentColor, SwarmDeployment>() {{
         put(DeploymentColor.BLUE, null);
         put(DeploymentColor.GREEN, null);
      }};
      this.deploymentContainerCfg = deploymentContainerCfg;
   }

   public SwarmFile addSwarmFile(File file, SwarmFile.State fileState, long fileSize) {
      SwarmFile swarmFile = null;
      synchronized (FILES_QUEUE_LOCK) {
         swarmFile = new SwarmFile(file, fileState, fileSize);
         swarmFilesQueue.add(swarmFile);
      }

      return swarmFile;
   }

   public boolean containsKey(WatchKey watchKey) {
      return this.watchKey.equals(watchKey);
   }

   public SwarmFile findFirstSwarmFileWithState(SwarmFile.State fileState) {
      SwarmFile resultSwarmFile = null;
      synchronized (FILES_QUEUE_LOCK) {
         for (SwarmFile swarmFile : swarmFilesQueue) {
            if (swarmFile.getState().equals(fileState)) {
               resultSwarmFile = swarmFile;
            }
         }
      }

      return resultSwarmFile;
   }

   public String getConsulHealthServiceUrl() {
      return deploymentContainerCfg.getConsulServiceHealthUrl();
   }

   public File getDestFolder() {
      return deploymentContainerCfg.getDestFolder();
   }

   public String getFilePattern() {
      return deploymentContainerCfg.getFilePattern();
   }

   public String getFilenamePattern() {
      return deploymentContainerCfg.getFilePattern();
   }

   public String getJvmParams() {
      return deploymentContainerCfg.getJvmParams();
   }

   public String getName() {
      return deploymentContainerCfg.getName();
   }

   public void setWatchKey(WatchKey watchKey) {
      this.watchKey = watchKey;
   }

   public boolean isDeploymentInProgress() {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         return deploymentInProgress;
      }
   }

   public void setDeploymentInProgress(boolean deploymentInProgress) {
      synchronized (DEPLOYMENT_IN_PROGRESS_LOCK) {
         this.deploymentInProgress = deploymentInProgress;
      }
   }

   public boolean isFileSuccessfullyLocked() {
      return fileSuccessfullyLocked;
   }

   public void setFileSuccessfullyLocked(boolean fileSuccessfullyLocked) {
      this.fileSuccessfullyLocked = fileSuccessfullyLocked;
   }

   public void isValid() throws ValidationException {
      StringBuilder errMsgs = new StringBuilder();
      if (getSourcePath().getAbsolutePath().equalsIgnoreCase(getTargetPath().getAbsolutePath())) {
         errMsgs.append(String.format("Source and target folder are same [%s = %s]", getSourcePath(), getTargetPath()));
      }

      // Only throw error if there are no text messages in the string builder.
      if (!errMsgs.toString().isEmpty()) {
         ExceptionThrower.throwValidationException(errMsgs.toString());
      }
   }

   public File getSourcePath() { return deploymentContainerCfg.getSrcFolder(); }

   public File getTargetPath() { return deploymentContainerCfg.getDestFolder(); }

   public DeploymentColor nextDeploymentColor() {
      DeploymentColor freeDeploymentColor = freeDeploymentColor();
      if (freeDeploymentColor != null) {
         return freeDeploymentColor;
      }
      // Take the deployment with older timestamp
      long blueProcessStart  = getDeployment(DeploymentColor.BLUE).getProcessTimeStart();
      long greenProcessStart = getDeployment(DeploymentColor.GREEN).getProcessTimeStart();
      return blueProcessStart > greenProcessStart ? DeploymentColor.GREEN : DeploymentColor.BLUE;
   }

   public DeploymentColor freeDeploymentColor() {
      if (swarmDeployments.get(DeploymentColor.BLUE) == null) {
         return DeploymentColor.BLUE;
      } else if (swarmDeployments.get(DeploymentColor.GREEN) == null) {
         return DeploymentColor.GREEN;
      }
      return null;
   }

   public SwarmDeployment getDeployment(DeploymentColor color) {
      return swarmDeployments.get(color);
   }

   public boolean removeSwarmFile(SwarmFile swarmFileToBeRemoved) {
      boolean removed;
      synchronized (FILES_QUEUE_LOCK) {
         removed = swarmFilesQueue.remove(swarmFileToBeRemoved);
      }

      return removed;
   }

   public void setDeployment(DeploymentColor color, SwarmDeployment swarmDeployment) {
      swarmDeployments.put(color, swarmDeployment);
   }
}
