package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SwarmDeploymentCfg {
   private final Integer processTimeStart;
   private final String  swarmFilePath;

   @JsonCreator
   public SwarmDeploymentCfg(@JsonProperty("swarmFile") String swarmFilePath,
                             @JsonProperty("processTimeStart") Integer processTimeStart) {
      this.swarmFilePath = swarmFilePath;
      this.processTimeStart = processTimeStart;
   }

   public Integer getProcessTimeStart() {
      return processTimeStart;
   }

   public String getSwarmFilePath() {
      return swarmFilePath;
   }

   @Override
   public String toString() {
      return "SwarmDeploymentCfg{" +
             "processTimeStart=" + processTimeStart +
             ", swarmFilePath='" + swarmFilePath + '\'' +
             '}';
   }
}
