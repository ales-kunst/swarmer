package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class SwarmDeploymentSetting {
   public Integer processTimeStart;
   public String  swarmFile;

   @Override
   public String toString() {
      return "ClassPojo [processTimeStart = " + processTimeStart + ", swarmFile = " + swarmFile + "]";
   }
}
