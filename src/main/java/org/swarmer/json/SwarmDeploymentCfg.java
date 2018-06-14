package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SwarmDeploymentCfg {
   private String  deploymentColor;
   private Integer pid;
   private String  swarmFilePath;
   private String  windowTitle;

   @JsonCreator
   public SwarmDeploymentCfg(@JsonProperty("deployment_color") String deploymentColor,
                             @JsonProperty("swarm_file_path") String swarmFilePath,
                             @JsonProperty("pid") Integer pid,
                             @JsonProperty("window_title") String windowTitle) {
      this.deploymentColor = deploymentColor;
      this.swarmFilePath = swarmFilePath;
      this.pid = pid;
      this.windowTitle = windowTitle;
   }

   @JsonGetter("deployment_color")
   public String getDeploymentColor() {
      return deploymentColor;
   }

   @JsonGetter("pid")
   public Integer getPid() {
      return pid;
   }

   @JsonGetter("swarm_file_path")
   public String getSwarmFilePath() {
      return swarmFilePath;
   }

   @JsonGetter("window_title")
   public String getWindowTitle() {
      return windowTitle;
   }

   public boolean isBlueDeployment() {
      return deploymentColor.equalsIgnoreCase("blue");
   }

   public boolean isGreenDeployment() {
      return deploymentColor.equalsIgnoreCase("green");
   }

   @Override
   public String toString() {
      return "SwarmDeploymentCfg{" +
             "deploymentColor='" + deploymentColor + '\'' +
             ", swarmFilePath='" + swarmFilePath + '\'' +
             ", pid=" + pid +
             ", windowTitle='" + windowTitle + '\'' +
             '}';
   }
}
