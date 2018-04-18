package org.swarmer.util;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ConsulUtilTest {

   private String consulJsonText;

   @Before
   public void setUp() {
      InputStream inStream = this.getClass().getResourceAsStream("/test_json.json");
      try {
         consulJsonText = IOUtils.toString(inStream, StandardCharsets.UTF_8.name());
         consulJsonText = "{\"Consul\":" + consulJsonText + "}";
      } catch (IOException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      } finally {
         CloseableUtil.close(inStream);
      }
   }

   @Test
   public void testGetServiceId() {
      String serviceId_8085 = ConsulUtil.getServiceId(consulJsonText, "127.0.0.1", 8085);

      Assert.assertEquals("QnstMS:127.0.0.1:8085", serviceId_8085);
   }

}