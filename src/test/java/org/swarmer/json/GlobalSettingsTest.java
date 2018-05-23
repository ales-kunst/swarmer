package org.swarmer.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class GlobalSettingsTest {

   @Test
   public void testCreateGlobalSeetingsFromJson() {
      URL          url          = getClass().getResource("/swarmer.json");
      File         jsonFile     = new File(url.getFile());
      ObjectMapper objectMapper = new ObjectMapper();
      try {
         GlobalSettings gs = objectMapper
                 .readerFor(GlobalSettings.class)
                 .readValue(jsonFile);

         Assert.assertEquals(gs.javaPath, "C:\\winapp\\Java\\1.8.0_102\\X64\\JDK");
         Assert.assertEquals(gs.serverAddress, "192.0.2.62");
         Assert.assertEquals(gs.serverPort.intValue(), 221);
         Assert.assertEquals(gs.lockWaitTimeout.intValue(), 5000);
         Assert.assertEquals(gs.swarmPortLower.intValue(), 8000);
         Assert.assertEquals(gs.swarmPortUpper.intValue(), 10000);
         Assert.assertEquals(gs.swarmDefaultStartupTime.intValue(), 90);

         Assert.assertEquals(gs.getDeploymentSetting(0).name, "partner");
         Assert.assertEquals(gs.getDeploymentSetting(0).srcFolder, "D:\\temp");
         Assert.assertEquals(gs.getDeploymentSetting(0).destFolder, "D:\\temp\\tmp");
         Assert.assertEquals(gs.getDeploymentSetting(0).filePattern, "demo-swarm.*\\.jar");
         Assert.assertEquals(gs.getDeploymentSetting(0).jvmParams,
                             "-Dfile.encoding=UTF-8 -Dswarm.bind.address=127.0.0.1 -Djava.io.tmpdir=D:\\swarm_temp");
         Assert.assertEquals(gs.getDeploymentSetting(0).consulUrl, "http://127.0.0.1:8500");
         Assert.assertEquals(gs.getDeploymentSetting(0).consulServiceHealthUrl,
                             "http://127.0.0.1:8500/v1/health/service/QnstMS");

         Assert.assertEquals(gs.getSwarmDeploymentSetting(0, 0).swarmFile, "D:\\temp\\tmp");
         Assert.assertEquals(gs.getSwarmDeploymentSetting(0, 0).processTimeStart.intValue(), 1234567890);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}