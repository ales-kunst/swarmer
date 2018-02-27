package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

   public String getName() {
      return swarmConfig.getName();
   }

}
