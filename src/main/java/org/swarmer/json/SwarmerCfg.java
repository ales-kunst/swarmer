package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SwarmerCfg {

   private final List<DeploymentContainerCfg> deploymentContainerCfgList;
   private final GeneralData                  generalData;

   @JsonCreator
   public SwarmerCfg(@JsonProperty("general_data") GeneralData generalData,
                     @JsonProperty(
                             "deployment_container_list") List<DeploymentContainerCfg> deploymentContainerCfgList) {
      this.generalData = generalData;
      this.deploymentContainerCfgList =
              deploymentContainerCfgList != null ? deploymentContainerCfgList : new ArrayList<>();
   }

   public int deploymentContainerCfgsSize() {
      return deploymentContainerCfgList.size();
   }

   public DeploymentContainerCfg getDeploymentContainerCfg(int index) {
      return index < deploymentContainerCfgList.size() ? deploymentContainerCfgList.get(index) : null;
   }

   @JsonGetter("general_data")
   public GeneralData getGeneralData() {
      return generalData;
   }

   @Override
   public String toString() {
      return "SwarmerCfg{" +
             "deploymentContainerCfgList=" + deploymentContainerCfgList +
             ", generalData=" + generalData +
             '}';
   }

   @JsonGetter("deployment_container_list")
   protected List<DeploymentContainerCfg> getDeploymentContainerCfgList() {
      return deploymentContainerCfgList;
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

      @JsonGetter("java_path")
      public String getJavaPath() {
         return javaPath;
      }

      @JsonGetter("lock_wait_timeout")
      public Integer getLockWaitTimeout() {
         return lockWaitTimeout;
      }

      @JsonGetter("server_address")
      public String getServerAddress() {
         return serverAddress;
      }

      @JsonGetter("server_port")
      public Integer getServerPort() {
         return serverPort;
      }

      @JsonGetter("swarm_default_startup_time")
      public Integer getSwarmDefaultStartupTime() {
         return swarmDefaultStartupTime;
      }

      @JsonGetter("swarm_port_lower")
      public Integer getSwarmPortLower() {
         return swarmPortLower;
      }

      @JsonGetter("swarm_port_upper")
      public Integer getSwarmPortUpper() {
         return swarmPortUpper;
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