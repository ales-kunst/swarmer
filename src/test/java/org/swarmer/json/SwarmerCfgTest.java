package org.swarmer.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SwarmerCfgTest {

   @Test
   public void testCreateSwarmerConfigurationFromJson() {
      URL          url          = getClass().getResource("/swarmer_config.json");
      File         jsonFile     = new File(url.getFile());
      ObjectMapper objectMapper = new ObjectMapper();
      try {
         SwarmerCfg cfg = objectMapper
                 .readerFor(SwarmerCfg.class)
                 .readValue(jsonFile);

         System.out.println("Cfg" + cfg);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }

}