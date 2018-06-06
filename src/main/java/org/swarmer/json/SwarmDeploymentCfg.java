package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SwarmDeploymentCfg {
   private final Integer processTimeStart;
   private final String  swarmFilePath;

   @JsonCreator
   public SwarmDeploymentCfg(@JsonProperty("swarm_file_path") String swarmFilePath,
                             @JsonProperty("process_time_start") Integer processTimeStart) {
      this.swarmFilePath = swarmFilePath;
      this.processTimeStart = processTimeStart;
   }

   @JsonGetter("process_time_start")
   public Integer getProcessTimeStart() {
      return processTimeStart;
   }

   @JsonGetter("swarm_file_path")
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
