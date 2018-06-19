package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SwarmerCfg implements Cloneable {

   private List<DeploymentContainerCfg> deploymentContainerCfgList;
   private GeneralData                  generalData;

   @JsonCreator
   public SwarmerCfg(@JsonProperty("general_data") GeneralData generalData,
                     @JsonProperty(
                             "deployment_container_list") List<DeploymentContainerCfg> deploymentContainerCfgList) {
      this.generalData = generalData;
      this.deploymentContainerCfgList =
              deploymentContainerCfgList != null ? deploymentContainerCfgList : new ArrayList<>();
   }

   public Object clone() throws CloneNotSupportedException {
      return super.clone();
   }

   public boolean containerExists(String containerName) {
      if (containerName == null || containerName.isEmpty()) {
         return false;
      }
      return searchDeploymentContainer(containerName).isPresent();
   }

   private Optional<DeploymentContainerCfg> searchDeploymentContainer(String containerName) {
      return containerCfgs().stream()
                            .filter(container -> container.getName().equalsIgnoreCase(containerName))
                            .findFirst();
   }

   @JsonGetter("deployment_container_list")
   public List<DeploymentContainerCfg> containerCfgs() {
      return deploymentContainerCfgList;
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

   public boolean hasContainerDeployments(String containerName) {
      Optional<DeploymentContainerCfg> deployment = searchDeploymentContainer(containerName);
      return deployment.isPresent() && !deployment.get().swarmDeploymentCfgs().isEmpty();
   }

   @JsonIgnoreProperties(ignoreUnknown = true)
   public static class GeneralData implements Cloneable {

      private final String  javaPath;
      private final Integer lockWaitTimeout;
      private final Integer serverPort;
      private final Integer swarmDefaultStartupTime;
      private final Integer swarmPortLower;
      private final Integer swarmPortUpper;


      @JsonCreator
      public GeneralData(@JsonProperty("java_path") String javaPath,
                         @JsonProperty("server_port") Integer serverPort,
                         @JsonProperty("lock_wait_timeout") Integer lockWaitTimeout,
                         @JsonProperty("swarm_port_lower") Integer swarmPortLower,
                         @JsonProperty("swarm_port_upper") Integer swarmPortUpper,
                         @JsonProperty("swarm_default_startup_time") Integer swarmDefaultStartupTime) {
         this.javaPath = javaPath;
         this.lockWaitTimeout = lockWaitTimeout;
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

      public Object clone() throws CloneNotSupportedException {
         return super.clone();
      }

      @Override
      public String toString() {
         return "GeneralData{" +
                "javaPath='" + javaPath + '\'' +
                ", lockWaitTimeout=" + lockWaitTimeout +
                ", serverPort=" + serverPort +
                ", swarmDefaultStartupTime=" + swarmDefaultStartupTime +
                ", swarmPortLower=" + swarmPortLower +
                ", swarmPortUpper=" + swarmPortUpper +
                '}';
      }
   }

   @Override
   public String toString() {
      return "SwarmerCfg{" +
             "deploymentContainerCfgList=" + deploymentContainerCfgList +
             ", generalData=" + generalData +
             '}';
   }


}