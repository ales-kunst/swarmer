package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.exception.ExceptionThrower;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class NetUtils {
   public static final  int    MAX_PORT_NUMBER       = 65535;
   public static final  int    MIN_PORT_NUMBER       = 1100;
   private static final int    ACCEPT_TIMEOUT_MILLIS = 10 * 1000;
   private static final Logger LOG                   = LogManager.getLogger(NetUtils.class);

   public static StringBuffer getUrlContent(String urlAddress) {
      return getUrlContent(urlAddress, true);
   }

   public static StringBuffer getUrlContent(String urlAddress, boolean shouldLog) {
      URL            url           = null;
      StringBuffer   resultContent = null;
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

         CloseableUtil.close(contentReader);

      } catch (IOException e) {
         CloseableUtil.close(contentReader);
         resultContent = null;
         if (shouldLog) {
            LOG.error("Error executing getUrlContent: {}", e);
         }
      }
      return resultContent;
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
         LOG.trace("Error in isPortAvailable: {}", e);
      } finally {
         CloseableUtil.close(datagramSocket);
         CloseableUtil.close(socket);
         CloseableUtil.close(serverSocket);
      }

      return false;
   }
}
