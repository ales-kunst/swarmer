package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.util.ValidationException;

import java.io.File;

public class SwarmInstanceData {

   private static final Logger LOG = LogManager.getLogger(SwarmInstanceData.class);

   private final SwarmConfig swarmConfig;
   private       long        numOfErrors;
   private       long        numOfWarnings;

   public SwarmInstanceData(SwarmConfig swarmConfig) {
      this.swarmConfig = swarmConfig;
      numOfErrors = 0;
      numOfWarnings = 0;
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

   public void isValid() throws ValidationException {
      StringBuilder errMsgs = new StringBuilder();
      if (getSourcePath().getAbsolutePath().equalsIgnoreCase(getTargetPath().getAbsolutePath())) {
         errMsgs.append(String.format("Source and target folder are same [%s = %s]", getSourcePath(), getTargetPath()));
      }
      ;

      // Only throw error if there are no text messages in the string builder.
      if (!errMsgs.toString().isEmpty()) {
         throw new ValidationException(errMsgs.toString());
      }
   }

   public final boolean matchesFilePattern(String fileName) { return getSwarmConfig().matchesFilePattern(fileName); }

}
