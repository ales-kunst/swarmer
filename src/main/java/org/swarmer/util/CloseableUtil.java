package org.swarmer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class CloseableUtil {
   private static final Logger LOG = LoggerFactory.getLogger(CloseableUtil.class);

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

   private CloseableUtil() {}
}
