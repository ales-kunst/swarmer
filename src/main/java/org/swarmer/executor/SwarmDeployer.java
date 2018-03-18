package org.swarmer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.Main;
import org.swarmer.context.SwarmerContext;

public class SwarmDeployer {
   private static final Logger LOG = LogManager.getLogger(Main.class);

   SwarmerContext swarmerCtx;

   public SwarmDeployer() {
      swarmerCtx = SwarmerContext.instance();
   }

   private void sleep(long millis) {
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
         LOG.warn("Sleep error: []", e);
      }
   }

   public void start() {
      while (true) {
         sleep(1000);
      }
   }

}
