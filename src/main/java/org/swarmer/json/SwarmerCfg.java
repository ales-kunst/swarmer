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

      private final Integer deregisterConsulServiceTimeout;
      private final Integer serverPort;
      private final Integer shutdownSwarmTimeout;
      private final Integer swarmDefaultStartupTime;
      private final Integer swarmPortLower;
      private final Integer swarmPortUpper;


      @JsonCreator
      public GeneralData(@JsonProperty("deregister_consul_service_timeout") Integer deregisterConsulServiceTimeout,
                         @JsonProperty("server_port") Integer serverPort,
                         @JsonProperty("shutdown_swarm_timeout") Integer shutdownSwarmTimeout,
                         @JsonProperty("swarm_port_lower") Integer swarmPortLower,
                         @JsonProperty("swarm_port_upper") Integer swarmPortUpper,
                         @JsonProperty("swarm_default_startup_time") Integer swarmDefaultStartupTime) {
         this.deregisterConsulServiceTimeout = deregisterConsulServiceTimeout;
         this.shutdownSwarmTimeout = shutdownSwarmTimeout;
         this.serverPort = serverPort;
         this.swarmDefaultStartupTime = swarmDefaultStartupTime;
         this.swarmPortLower = swarmPortLower;
         this.swarmPortUpper = swarmPortUpper;
      }

      @JsonGetter("deregister_consul_service_timeout")
      public Integer getDeregisterConsulServiceTimeout() {
         return deregisterConsulServiceTimeout;
      }

      @JsonGetter("server_port")
      public Integer getServerPort() {
         return serverPort;
      }

      @JsonGetter("shutdown_swarm_timeout")
      public Integer getShutdownSwarmTimeout() {
         return shutdownSwarmTimeout;
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
         return "GeneralData [" +
                "deregisterConsulServiceTimeout=" + deregisterConsulServiceTimeout +
                ", serverPort=" + serverPort +
                ", shutdownSwarmTimeout=" + shutdownSwarmTimeout +
                ", swarmDefaultStartupTime=" + swarmDefaultStartupTime +
                ", swarmPortLower=" + swarmPortLower +
                ", swarmPortUpper=" + swarmPortUpper +
                ']';
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