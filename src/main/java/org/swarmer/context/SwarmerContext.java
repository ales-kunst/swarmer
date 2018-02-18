package org.swarmer.context;

import java.util.ArrayList;
import java.util.List;

import org.ini4j.Ini;

/**
 * SwarmContext is singleton in the system and holds list of swarm instances which were 
 * started by our Swarmer.
 * 
 * @author kun01826
 *
 */
public class SwarmerContext {
   
   /**
    * Instance of SwarmerContext.
    */
   private static SwarmerContext ctxInstance = null;
   
   /**
    * List of SwarmInstanceData objects.
    */
   protected List<SwarmInstanceData> swarmInstances;
   protected Ini.Section defaultSection;
   
   /**
    * Help for building SwarmerContext in more concise 
    * way.
    * 
    * @author kun01826
    *
    */
   public static class Builder extends SwarmerContext {
      
      private Builder() {
         super();
      }
      
      public Builder addSwarmInstanceData(SwarmInstanceData swarmInstanceData) {
         super.swarmInstances.add(swarmInstanceData);
         return this;
      }
      
      public Builder setDefaultSection(Ini.Section defaultSection) {
         super.defaultSection = defaultSection;
         return this;
      }
      
      public SwarmerContext build() {
         return new SwarmerContext(this);
      }
      
   }
   
   /**
    * Create SwarmContext builder.
    * 
    * @return SwarmerContextBuilder object. 
    */
   static Builder newBuilder() {
      return new Builder();
   }
   
   public static SwarmerContext instance() {
      return ctxInstance;
   }
   
   static void reset(SwarmerContext ctxInstance) {
      SwarmerContext.ctxInstance = ctxInstance;
   }
   
   public String[] getSwarmNames() {
      String[] names = new String[swarmInstances.size()];
      for (int index = 0; index < swarmInstances.size(); index++) {
         SwarmInstanceData swarmInstanceData = swarmInstances.get(index);
         names[index] = swarmInstanceData.getName();
      }
      return names;
   }
   
   public SwarmConfig[] getSwarmConfigs() {
      SwarmConfig[] swarmConfigs = new SwarmConfig[swarmInstances.size()];
      for (int index = 0; index < swarmInstances.size(); index++) {
         SwarmInstanceData swarmInstanceData = swarmInstances.get(index);
         swarmConfigs[index] = swarmInstanceData.getSwarmConfig();
      }
      return swarmConfigs;
   }
   
   private SwarmerContext() {
      this.swarmInstances = new ArrayList<SwarmInstanceData>();
   }
      
   private SwarmerContext(Builder builder) {
      this.swarmInstances = builder.swarmInstances;
      this.defaultSection = builder.defaultSection;
   }
}
