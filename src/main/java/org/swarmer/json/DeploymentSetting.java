package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class DeploymentSetting {
   public String                       consulServiceHealthUrl;
   public String                       consulUrl;
   public String                       destFolder;
   public String                       filePattern;
   public String                       jvmParams;
   public String                       name;
   public String                       srcFolder;
   public List<SwarmDeploymentSetting> swarmDeploymentSettings = new ArrayList<>();

   @Override
   public String toString() {
      return "ClassPojo [filePattern = " + filePattern + ", srcFolder = " + srcFolder + ", consulUrl = " + consulUrl +
             ", destFolder = " + destFolder + ", name = " + name + ", consulServiceHealthUrl = " +
             consulServiceHealthUrl + ", jvmParams = " + jvmParams + ", swarmDeploymentSettings = " +
             swarmDeploymentSettings + "]";
   }
}
