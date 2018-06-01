package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SwarmerCfg {

   private final List<DeploymentContainerCfg> deploymentContainerCfgs;
   private final GeneralData                  generalData;

   @JsonCreator
   public SwarmerCfg(@JsonProperty("general_data") GeneralData generalData,
                     @JsonProperty("deployment_container_list") List<DeploymentContainerCfg> deploymentContainerCfgs) {
      this.generalData = generalData;
      this.deploymentContainerCfgs = deploymentContainerCfgs != null ? deploymentContainerCfgs : new ArrayList<>();
   }

   public int deploymentContainerCfgsSize() {
      return deploymentContainerCfgs.size();
   }

   public DeploymentContainerCfg getDeploymentContainerCfg(int index) {
      return index < deploymentContainerCfgs.size() ? deploymentContainerCfgs.get(index) : null;
   }

   public String getJavaPath() {
      return generalData.javaPath;
   }

   public Integer getLockWaitTimeout() {
      return generalData.lockWaitTimeout;
   }

   public String getServerAddress() {
      return generalData.serverAddress;
   }

   public Integer getServerPort() {
      return generalData.serverPort;
   }

   public Integer getSwarmDefaultStartupTime() {
      return generalData.swarmDefaultStartupTime;
   }

   public Integer getSwarmPortLower() {
      return generalData.swarmPortLower;
   }

   public Integer getSwarmPortUpper() {
      return generalData.swarmPortUpper;
   }

   @Override
   public String toString() {
      return "SwarmerCfg{" +
             "deploymentContainerCfgs=" + deploymentContainerCfgs +
             ", generalData=" + generalData +
             '}';
   }

   @JsonIgnoreProperties(ignoreUnknown = true)
   public static class GeneralData {
      private final String  javaPath;
      private final Integer lockWaitTimeout;
      private final String  serverAddress;
      private final Integer serverPort;
      private final Integer swarmDefaultStartupTime;
      private final Integer swarmPortLower;
      private final Integer swarmPortUpper;


      @JsonCreator
      public GeneralData(@JsonProperty("java_path") String javaPath,
                         @JsonProperty("server_address") String serverAddress,
                         @JsonProperty("server_port") Integer serverPort,
                         @JsonProperty("lock_wait_timeout") Integer lockWaitTimeout,
                         @JsonProperty("swarm_port_lower") Integer swarmPortLower,
                         @JsonProperty("swarm_port_upper") Integer swarmPortUpper,
                         @JsonProperty("swarm_default_startup_time") Integer swarmDefaultStartupTime) {
         this.javaPath = javaPath;
         this.lockWaitTimeout = lockWaitTimeout;
         this.serverAddress = serverAddress;
         this.serverPort = serverPort;
         this.swarmDefaultStartupTime = swarmDefaultStartupTime;
         this.swarmPortLower = swarmPortLower;
         this.swarmPortUpper = swarmPortUpper;
      }

      @Override
      public String toString() {
         return "GeneralData{" +
                "javaPath='" + javaPath + '\'' +
                ", lockWaitTimeout=" + lockWaitTimeout +
                ", serverAddress='" + serverAddress + '\'' +
                ", serverPort=" + serverPort +
                ", swarmDefaultStartupTime=" + swarmDefaultStartupTime +
                ", swarmPortLower=" + swarmPortLower +
                ", swarmPortUpper=" + swarmPortUpper +
                '}';
      }
   }
}