package org.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class InfiniteThreadOperation<CTX> {

   private static final Logger LOG = LogManager.getLogger(InfiniteThreadOperation.class);
   private final        CTX    context;
   private              long   errorCount;
   private              State  state;
   private              Thread thread;

   public InfiniteThreadOperation(CTX context) {
      this.context = context;
      this.errorCount = 0;
      this.state = State.WAITING;
   }

   public State getState() {
      return state;
   }

   public Thread getThread() {
      return thread;
   }

   public void join() {
      if (thread != null) {
         try {
            thread.join();
         } catch (InterruptedException e) {
            LOG.error("Error in InfiniteThreadOperation: {}", e.getMessage());
         }
      }
   }

   public boolean start() {
      boolean started = false;
      if (thread == null) {
         thread = new Thread(new MyRunnable(this), threadName());
         thread.start();
         started = true;
      }
      return started;
   }

   protected abstract String threadName();

   protected CTX getContext() {
      return context;
   }

   protected long getErrorCount() {
      return errorCount;
   }

   private void executeLoop() {
      state = State.RUNNING;
      try {
         operationInitialize();
         while (!shouldStop()) {
            try {
               loopBlock();
            } catch (Exception e) {
               errorCount++;
               handleError(e);
            }
         }
      } finally {
         state = State.STOPPED;
         operationFinalize();
      }
   }

   protected abstract void operationInitialize();

   protected abstract boolean shouldStop();

   protected abstract void loopBlock() throws Exception;

   protected abstract void handleError(Exception exception);

   protected abstract void operationFinalize();

   public enum State {
      WAITING, RUNNING, STOPPED
   }

   private static class MyRunnable implements Runnable {

      private final InfiniteThreadOperation operation;

      MyRunnable(InfiniteThreadOperation operation) {
         this.operation = operation;
      }

      @Override
      public void run() {
         operation.executeLoop();
      }
   }
}
