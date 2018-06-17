package org.swarmer.operation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.exception.ExceptionThrower;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class InfiniteLoopOperation extends SwarmerOperation<SwarmerCtx> {

   private static final Logger        LOG = LogManager.getLogger(InfiniteLoopOperation.class);
   private final        AtomicBoolean running;
   private              long          errorCount;
   private              Thread        thread;

   public InfiniteLoopOperation(String name, SwarmerCtx context) {
      super(name, context);
      this.errorCount = 0;
      running = new AtomicBoolean(true);
   }

   public void cleanUp() { }

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
         waitUntilRunning();
         LOG.info("{} stopped", name());
      }
   }

   private Thread getThread() {
      return thread;
   }

   private void stopRunning() {
      running.set(false);
   }

   private void waitUntilRunning() throws InterruptedException {
      synchronized (operationsStates) {
         while (getState() == State.RUNNING) {
            operationsStates.wait();
         }
      }
   }

   private void executeLoop() throws Exception {
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

   protected abstract void operationInitialize() throws Exception;

   private boolean running() {
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
         try {
            operation.executeLoop();
         } catch (Exception e) {
            ExceptionThrower.throwRuntimeError(e);
         }
      }
   }


}
