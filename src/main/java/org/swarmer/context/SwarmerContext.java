package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import java.util.ArrayList;
import java.util.List;

/**
 * SwarmContext is singleton in the system and holds list of swarm instances which were
 * started by our Swarmer.
 *
 * @author aq
 */
public class SwarmerContext {
   private static final Logger LOG = LogManager.getLogger(SwarmerContext.class);

   /**
    * Instance of SwarmerContext.
    */
   private static SwarmerContext ctxInstance = null;

   /**
    * Create SwarmContext builder.
    *
    * @return SwarmerContextBuilder object.
    */
   static Builder newBuilder() {
      return new Builder();
   }

   /**
    * @return
    */
   public static SwarmerContext instance() {
      return ctxInstance;
   }

   static void reset(SwarmerContext ctxInstance) {
      SwarmerContext.ctxInstance = ctxInstance;
   }

   /**
    * List of SwarmInstanceData objects.
    */
   protected List<SwarmInstanceData> swarmInstances;
   protected Ini.Section             defaultSection;

   private SwarmerContext() {
      this.swarmInstances = new ArrayList<SwarmInstanceData>();
   }

   private SwarmerContext(Builder builder) {
      this.swarmInstances = builder.swarmInstances;
      this.defaultSection = builder.defaultSection;
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

   /**
    * Help for building SwarmerContext in more concise
    * way.
    *
    * @author kun01826
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
}
