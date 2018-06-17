package org.swarmer.json;

import com.fasterxml.jackson.annotation.*;
import org.swarmer.context.DeploymentColor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SwarmDeploymentCfg implements Cloneable {
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

   public Object clone() throws CloneNotSupportedException {
      return super.clone();
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

   @JsonGetter("deployment_color")
   public String getDeploymentColor() {
      return deploymentColor;
   }

   @JsonIgnore
   public DeploymentColor getDeploymentColorEnum() {
      return DeploymentColor.value(deploymentColor);
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

   @JsonIgnore
   public boolean isBlueDeployment() {
      return deploymentColor.equalsIgnoreCase("blue");
   }

   @JsonIgnore
   public boolean isGreenDeployment() {
      return deploymentColor.equalsIgnoreCase("green");
   }
}
