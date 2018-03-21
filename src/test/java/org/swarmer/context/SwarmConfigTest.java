package org.swarmer.context;

import org.junit.Assert;

public class SwarmConfigTest {

   @org.junit.Test
   public void getJavaParamValue() {
      SwarmConfig sc        = new SwarmConfig();
      String      params_01 = "-Dfile.encoding=UTF-8 -Dswarm.bind.address=127.0.0.1 -Dswarm.http.port=8085 -Dswarm.management.http.port=9991";
      String      params_02 = "-Dfile.encoding=UTF-8 -Dswarm.bind.address=127.0.0.1 -Dswarm.http.port = 8085 -Dswarm.management.http.port=9991";
      String      value_01  = sc.getJavaParamValue(params_01, SwarmConfig.SWARM_HTTP_PORT_REGEX);
      String      value_02  = sc.getJavaParamValue(params_02, SwarmConfig.SWARM_HTTP_PORT_REGEX);

      Assert.assertEquals("8085", value_01);
      Assert.assertEquals(null, value_02);
   }
}