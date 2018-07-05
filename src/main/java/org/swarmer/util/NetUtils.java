package org.swarmer.util;

import org.apache.commons.lang.math.IntRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.exception.ExceptionThrower;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {
   public static final  int    MAX_PORT_NUMBER       = 65535;
   public static final  int    MIN_PORT_NUMBER       = 1100;
   private static final String IP_VALIDATION_PATTERN = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
   private static final Logger LOG                   = LoggerFactory.getLogger(NetUtils.class);

   public static int getFirstAvailablePort(IntRange range) {
      int freePort = -1;
      LOG.debug("Obtain first available port [{}, {}].", range.getMinimumInteger(), range.getMaximumInteger());
      for (int port = range.getMinimumInteger(); port <= range.getMaximumInteger(); port++) {
         if (isPortAvailable(port)) {
            freePort = port;
            LOG.debug("Found available port [{}].", freePort);
            break;
         }
      }
      return freePort;
   }

   public static boolean isPortAvailable(int port) {
      if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
         String errMsg = String.format("Invalid start port: %d", port);
         ExceptionThrower.throwIllegalArgumentException(errMsg);
      }

      ServerSocket   serverSocket   = null;
      DatagramSocket datagramSocket = null;
      Socket         socket         = null;
      try {
         serverSocket = new ServerSocket(port);
         serverSocket.setReuseAddress(true);
         socket = new Socket("127.0.0.1", port);
         socket.setReuseAddress(true);
         datagramSocket = new DatagramSocket(port);
         datagramSocket.setReuseAddress(true);

         return true;
      } catch (IOException e) {
         // If IOEXception happens then we know that the port is already taken
      } finally {
         CloseableUtil.close(datagramSocket);
         CloseableUtil.close(socket);
         CloseableUtil.close(serverSocket);
      }

      return false;
   }

   public static List<InetAddress> getLocalIpAddresses() {
      List<InetAddress> ipAddresses = new ArrayList<>();
      final Pattern     pattern     = Pattern.compile(IP_VALIDATION_PATTERN, Pattern.MULTILINE);

      try {
         Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
         while (netInterfaces.hasMoreElements()) {
            NetworkInterface netInterface  = (NetworkInterface) netInterfaces.nextElement();
            Enumeration      inetAddresses = netInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
               InetAddress   inetAddress = (InetAddress) inetAddresses.nextElement();
               final Matcher matcher     = pattern.matcher(inetAddress.getHostAddress());
               if (matcher.matches()) {
                  ipAddresses.add(inetAddress);
               }
            }
         }
      } catch (SocketException e1) {
      }

      return ipAddresses;
   }

   public static StringBuffer getUrlContent(String urlAddress) throws IOException {
      URL            url;
      StringBuffer   resultContent;
      BufferedReader contentReader = null;
      try {
         // get Url connection
         url = new URL(urlAddress);
         URLConnection conn = url.openConnection();

         // open the stream and put it into BufferedReader
         contentReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
         String inputLine;
         resultContent = new StringBuffer();

         // Get Url content
         while ((inputLine = contentReader.readLine()) != null) {
            resultContent.append(inputLine);
         }

      } finally {
         CloseableUtil.close(contentReader);
      }
      return resultContent;
   }
}
