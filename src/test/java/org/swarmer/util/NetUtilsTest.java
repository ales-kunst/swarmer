package org.swarmer.util;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.util.List;

public class NetUtilsTest {

   @Test
   public void getLocalIpAddress() {
      List<InetAddress> ipAddresses = NetUtils.getLocalIpAddress();
      Assert.assertTrue(ipAddresses.size() >= 1);
   }
}