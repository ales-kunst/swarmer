package org.swarmer.operation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class InfiniteLoopOperation<CTX> extends SwarmerOperation<CTX> {

   private static final Logger        LOG = LogManager.getLogger(InfiniteLoopOperation.class);
   private final        AtomicBoolean running;
   protected            Thread        thread;
   private              long          errorCount;

   public InfiniteLoopOperation(String name, CTX context) {
      super(name, context);
      this.errorCount = 0;
      running = new AtomicBoolean(true);
   }

   public final void execute() {
      if (thread == null) {
         thread = new Thread(new MyRunnable(this), name());
         thread.start();
      }
   }

   public final void gracefulStop() throws InterruptedException {
      if (getThread() != null) {
         LOG.info("Stopping {}", name());
         stopRunning();
         waitUntil(State.RUNNING);
         LOG.info("{} stopped", name());
      }
   }

   private Thread getThread() {
      return thread;
   }

   protected final void stopRunning() {
      running.set(false);
   }

   private void waitUntil(State state) throws InterruptedException {
      synchronized (operationsStates) {
         while (getState() == state) {
            operationsStates.wait();
         }
      }
   }

   protected long getErrorCount() {
      return errorCount;
   }

   private void executeLoop() {
      setState(SwarmerOperation.State.RUNNING);
      try {
         LOG.info("Starting {}", name());
         operationInitialize();
         while (running()) {
            try {
               loopBlock();
            } catch (Exception e) {
               errorCount++;
               try {
                  handleError(e);
               } catch (Exception otherExc) {
                  setState(SwarmerOperation.State.ERROR);
                  stopRunning();
               }
            }
         }
      } finally {
         if (getState() != State.ERROR) {
            setState(SwarmerOperation.State.FINISHED);
         }
         operationFinalize();
      }
   }

   protected abstract void operationInitialize();

   protected final boolean running() {
      return running.get();
   }

   protected abstract void loopBlock() throws Exception;

   protected abstract void handleError(Exception exception);

   protected abstract void operationFinalize();

   private static class MyRunnable implements Runnable {
      private final InfiniteLoopOperation operation;

      MyRunnable(InfiniteLoopOperation operation) {
         this.operation = operation;
      }

      @Override
      public void run() {
         operation.executeLoop();
      }
   }


}
