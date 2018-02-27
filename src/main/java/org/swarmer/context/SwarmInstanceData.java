package org.swarmer.context;

public class SwarmInstanceData {

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
