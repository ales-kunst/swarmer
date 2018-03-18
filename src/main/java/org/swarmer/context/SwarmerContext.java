package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import java.util.ArrayList;
import java.util.List;


public class SwarmerContext {
   private static final Logger         LOG                       = LogManager.getLogger(SwarmerContext.class);
   private static final String         SETTING_JAVA_PATH         = "java.path";
   private static final String         SETTING_LOCK_WAIT_TIMEOUT = "lock.wait.timeout";
   private static       SwarmerContext ctxInstance               = null;

   public static SwarmerContext instance() {
      return ctxInstance;
   }

   static Builder newBuilder() {
      return new Builder();
   }

   static void reset(SwarmerContext ctxInstance) {
      SwarmerContext.ctxInstance = ctxInstance;
   }

   protected Ini.Section           defaultSection;
   protected List<SwarmDeployment> swarmDeployments;

   private SwarmerContext() {
      this.swarmDeployments = new ArrayList<SwarmDeployment>();
   }

   private SwarmerContext(Builder builder) {
      this.swarmDeployments = builder.swarmDeployments;
      this.defaultSection = builder.defaultSection;
   }

   public String getJavaPath() {
      String javaPathValue = defaultSection.get(SETTING_JAVA_PATH);
      return javaPathValue;
   }

   public int getLockWaitTimeout() {
      int    lockWaitTimeout      = 3000;
      String lockWaitTimeoutValue = defaultSection.get(SETTING_LOCK_WAIT_TIMEOUT);
      if (lockWaitTimeoutValue != null) {
         lockWaitTimeout = Integer.valueOf(lockWaitTimeoutValue);
      }

      return lockWaitTimeout;
   }

   public SwarmDeployment[] getSwarmDeployments() {
      SwarmDeployment[] resultArray = new SwarmDeployment[swarmDeployments.size()];
      return swarmDeployments.toArray(resultArray);
   }

   public static class Builder extends SwarmerContext {

      private Builder() {
         super();
      }

      public Builder addSwarmDeployment(SwarmDeployment swarmDeployment) {
         super.swarmDeployments.add(swarmDeployment);
         return this;
      }

      public SwarmerContext build() {
         return new SwarmerContext(this);
      }

      public Builder setDefaultSection(Ini.Section defaultSection) {
         super.defaultSection = defaultSection;
         return this;
      }

   }
}
