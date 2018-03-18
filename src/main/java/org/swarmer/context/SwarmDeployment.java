package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.util.ExceptionThrower;
import org.swarmer.util.ValidationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SwarmDeployment {

   private static final Logger LOG = LogManager.getLogger(SwarmDeployment.class);

   private final SwarmConfig     swarmConfig;
   private       List<SwarmFile> swarmFilesQueue;

   public SwarmDeployment(SwarmConfig swarmConfig) {
      this.swarmConfig = swarmConfig;
      this.swarmFilesQueue = new ArrayList<>();
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

   public SwarmFile addSwarmFile(File file, SwarmFile.State fileState, long fileSize) {
      SwarmFile swarmFile = new SwarmFile(file, fileState, fileSize);
      swarmFilesQueue.add(swarmFile);
      return swarmFile;
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
