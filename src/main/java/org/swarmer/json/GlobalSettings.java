package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class GlobalSettings {
   public List<DeploymentSetting> deploymentSettings = new ArrayList<>();
   public String                  javaPath;
   public Integer                 lockWaitTimeout;
   public String                  serverAddress;
   public Integer                 serverPort;
   public Integer                 swarmDefaultStartupTime;
   public Integer                 swarmPortLower;
   public Integer                 swarmPortUpper;

   public DeploymentSetting getDeploymentSetting(int index) {
      return deploymentSettings.get(index);
   }

   public SwarmDeploymentSetting getSwarmDeploymentSetting(int dsIndex, int sdsIndex) {
      return deploymentSettings.get(dsIndex).swarmDeploymentSettings.get(sdsIndex);
   }

   @Override
   public String toString() {
      return "ClassPojo [swarmPortLower = " + swarmPortLower + ", javaPath = " + javaPath + ", lockWaitTimeout = " +
             lockWaitTimeout + ", serverAddress = " + serverAddress + ", serverPort = " + serverPort +
             ", deploymentSettings = " + deploymentSettings + ", swarmDefaultStartupTime = " + swarmDefaultStartupTime +
             ", swarmPortUpper = " + swarmPortUpper + "]";
   }
}