package org.swarmer.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SwarmerCfgTest {

   private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

   @Test
   public void testCreateJsonFromSwarmerCfg() throws IOException {
      SwarmerCfg cfgObj = getDefaultSwarmerCfg();
      String     json   = JSON_MAPPER.writeValueAsString(cfgObj);
      Assert.assertEquals(530, json.length());
   }

   private SwarmerCfg getDefaultSwarmerCfg() throws IOException {
      URL  url      = getClass().getResource("/swarmer_config.json");
      File jsonFile = new File(url.getFile());
      return JSON_MAPPER
              .readerFor(SwarmerCfg.class)
              .readValue(jsonFile);
   }

   @Test
   public void testCreateSwarmerCfgFromJson() throws IOException {
      SwarmerCfg cfgObj = getDefaultSwarmerCfg();

      Assert.assertEquals("C:\\winapp\\Java\\1.8.0_102\\X64\\JDK", cfgObj.getGeneralData().getJavaPath());
      Assert.assertEquals(90, cfgObj.getGeneralData().getSwarmDefaultStartupTime().intValue());
      Assert.assertEquals(1, cfgObj.deploymentContainerCfgsSize());
      Assert.assertEquals(0, cfgObj.getDeploymentContainerCfg(0).swarmDeploymentCfgsSize());

   }

}