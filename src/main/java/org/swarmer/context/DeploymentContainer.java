package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeploymentContainer {

   private static final Object DEPLOYMENT_IN_PROGRESS_LOCK = new Object();
   private static final Logger LOG                         = LogManager.getLogger(DeploymentContainer.class);
   private final SwarmConfig     swarmConfig;
   private       boolean         deploymentInProgress;
   private       List<SwarmFile> swarmFilesQueue;

   public DeploymentContainer(SwarmConfig swarmConfig) {
      this.swarmConfig = swarmConfig;
      this.swarmFilesQueue = new ArrayList<>();
      this.deploymentInProgress = false;
   }

   public SwarmFile addSwarmFile(File file, SwarmFile.State fileState, long fileSize) {
      SwarmFile swarmFile = new SwarmFile(file, fileState, fileSize);
      swarmFilesQueue.add(swarmFile);
      return swarmFile;
   }

   public SwarmFile getLastSwarmFile(SwarmFile.State fileState) {
      SwarmFile resultSwarmFile = null;
      for (SwarmFile swarmFile : swarmFilesQueue) {
         if (swarmFile.getState().equals(fileState)) {
            resultSwarmFile = swarmFile;
         }
      }
      return resultSwarmFile;
   }

   public final String getName() {
      return getSwarmConfig().getName();
   }

   public final File getSourcePath() { return getSwarmConfig().getSourcePath(); }

   public SwarmConfig getSwarmConfig() {
      return swarmConfig;
   }

   public final File getTargetPath() {
      return getSwarmConfig().getTargetPath();
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

   public final boolean matchesFilePattern(String fileName) { return getSwarmConfig().matchesFilePattern(fileName); }

}
