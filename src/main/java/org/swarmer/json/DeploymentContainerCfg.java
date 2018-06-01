package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentContainerCfg {
   private final String                   consulServiceHealthUrl;
   private final String                   consulUrl;
   private final String                   destFolder;
   private final String                   filePattern;
   private final String                   jvmParams;
   private final String                   name;
   private final String                   srcFolder;
   private final List<SwarmDeploymentCfg> swarmDeploymentCfgs;

   public DeploymentContainerCfg(@JsonProperty("name") String name,
                                 @JsonProperty("src_folder") String srcFolder,
                                 @JsonProperty("dest_folder") String destFolder,
                                 @JsonProperty("file_pattern") String filePattern,
                                 @JsonProperty("jvm_params") String jvmParams,
                                 @JsonProperty("consul_url") String consulUrl,
                                 @JsonProperty("consul_service_health_url") String consulServiceHealthUrl,
                                 @JsonProperty("swarm_deployment_list") List<SwarmDeploymentCfg> swarmDeploymentCfgs) {
      this.consulServiceHealthUrl = consulServiceHealthUrl;
      this.consulUrl = consulUrl;
      this.destFolder = destFolder;
      this.filePattern = filePattern;
      this.jvmParams = jvmParams;
      this.name = name;
      this.srcFolder = srcFolder;
      this.swarmDeploymentCfgs = swarmDeploymentCfgs != null ? swarmDeploymentCfgs : new ArrayList<>();
   }

   public String getConsulServiceHealthUrl() {
      return consulServiceHealthUrl;
   }

   public String getConsulUrl() {
      return consulUrl;
   }

   public String getDestFolder() {
      return destFolder;
   }

   public String getFilePattern() {
      return filePattern;
   }

   public String getJvmParams() {
      return jvmParams;
   }

   public String getName() {
      return name;
   }

   public String getSrcFolder() {
      return srcFolder;
   }

   public SwarmDeploymentCfg getSwarmDeploymentCfg(int index) {
      return index < swarmDeploymentCfgs.size() ? swarmDeploymentCfgs.get(index) : null;
   }

   public int swarmDeploymentCfgsSize() {
      return swarmDeploymentCfgs.size();
   }

   @Override
   public String toString() {
      return "DeploymentContainerCfg{" +
             "consulServiceHealthUrl='" + consulServiceHealthUrl + '\'' +
             ", consulUrl='" + consulUrl + '\'' +
             ", destFolder='" + destFolder + '\'' +
             ", filePattern='" + filePattern + '\'' +
             ", jvmParams='" + jvmParams + '\'' +
             ", name='" + name + '\'' +
             ", srcFolder='" + srcFolder + '\'' +
             ", swarmDeploymentCfgs=" + swarmDeploymentCfgs +
             '}';
   }
}