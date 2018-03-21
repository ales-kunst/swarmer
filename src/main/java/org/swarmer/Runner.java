package org.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerContext;
import org.swarmer.exception.ExceptionThrower;

public abstract class Runner implements Runnable {
   private static final Logger LOG = LogManager.getLogger(Runner.class);
   protected SwarmerContext swarmerCtx;
   private   Thread         thread;

   Runner() {
      swarmerCtx = SwarmerContext.instance();
   }

   protected abstract String getName();

   public Thread getThread() {
      if (thread == null) {
         thread = new Thread(this);
         thread.setName(getName());
      }
      return thread;
   }

   @Override
   public void run() {
      try {
         runLocal();
      } catch (Exception e) {
         LOG.error("Error in run method: {}", e);
         ExceptionThrower.throwRuntimeError(e);
      }

   }

   protected abstract void runLocal() throws Exception;
}
