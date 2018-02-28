package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

   public SwarmConfig getSwarmConfig() {
      return swarmConfig;
   }

   public final String getName() {
      return getSwarmConfig().getName();
   }

   public final File getSourcePath() { return getSwarmConfig().getSourcePath(); }

   public final File getTargetPath() {
      return getSwarmConfig().getTargetPath();
   }

   public final boolean matchesFilePattern(String fileName) { return getSwarmConfig().matchesFilePattern(fileName); }

}
