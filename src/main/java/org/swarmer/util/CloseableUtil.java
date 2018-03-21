package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;

public class CloseableUtil {
   private static final Logger LOG = LogManager.getLogger(CloseableUtil.class);

   public static boolean close(Closeable closable) {
      boolean isSuccessfull = false;
      if (closable != null) {
         try {
            closable.close();
            isSuccessfull = true;
         } catch (IOException e) {
            LOG.error("Error when closing [{}]: {}", closable.getClass().getName(), e);
         }
      }
      return isSuccessfull;
   }
}
