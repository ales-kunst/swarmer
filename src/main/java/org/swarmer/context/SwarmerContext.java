package org.swarmer.context;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import java.util.ArrayList;
import java.util.List;


public class SwarmerContext {
   private static final Logger         LOG                       = LogManager.getLogger(SwarmerContext.class);
   private static final String         SETTING_JAVA_PATH         = "java.path";
   private static final String         SETTING_LOCK_WAIT_TIMEOUT = "lock.wait.timeout";
   private static final String         SETTING_SWARM_PORT_LOWER  = "swarm.port.lower";
   private static final String         SETTING_SWARM_PORT_UPPER  = "swarm.port.upper";
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

   protected Ini.Section               defaultSection;
   protected List<DeploymentContainer> deploymentContainers;

   private SwarmerContext() {
      this.deploymentContainers = new ArrayList<DeploymentContainer>();
   }

   private SwarmerContext(Builder builder) {
      this.deploymentContainers = builder.deploymentContainers;
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

   public IntRange getPortRange() {
      int defaultLowerPort = 8000;
      int defaultUpperPort = 10000;
      return new IntRange(defaultLowerPort, defaultUpperPort);
   }

   public DeploymentContainer[] getDeploymentContainers() {
      DeploymentContainer[] resultArray = new DeploymentContainer[deploymentContainers.size()];
      return deploymentContainers.toArray(resultArray);
   }

   public static class Builder extends SwarmerContext {

      private Builder() {
         super();
      }

      public Builder addDeploymentContainer(DeploymentContainer deploymentContainer) {
         super.deploymentContainers.add(deploymentContainer);
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
