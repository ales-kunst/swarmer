package org.swarmer.operation;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.State;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.exception.ExceptionThrower;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class InfiniteLoopOperation extends SwarmerOperation<SwarmerCtx, Boolean> {

   private static final Logger        LOG = LoggerFactory.getLogger(InfiniteLoopOperation.class);
   private final        AtomicBoolean running;
   private              Thread        thread;

   public InfiniteLoopOperation(String name, SwarmerCtx context) {
      super(name, context);
      running = new AtomicBoolean(true);
   }

   public void cleanUp() { }

   public final Boolean execute() {
      if (thread == null) {
         thread = new Thread(new MyRunnable(this), name());
         thread.start();
      }
      return true;
   }

   public final void gracefulStop() throws InterruptedException {
      if (getThread() != null) {
         LOG.info("Stopping Operation [{}]", name());
         stopRunning();
         waitUntilRunning();
         LOG.info("Operation [{}] stopped", name());
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
      setState(State.RUNNING);
      try {
         LOG.info("Starting Operation [{}]", name());
         operationInitialize();
         while (running()) {
            try {
               loopBlock();
            } catch (Exception e) {
               try {
                  handleError(e);
               } catch (Exception otherExc) {
                  setState(State.ERROR);
                  stopRunning();
                  LOG.error("Error In Operation [{}]: {}", name(), ExceptionUtils.getStackTrace(e));
               } catch (Throwable t) {
                  setState(State.ERROR);
                  stopRunning();
                  LOG.error("Fastal Error In Operation [{}]: {}", name(), ExceptionUtils.getStackTrace(t));
               }
            }
         }
      } finally {
         if (getState() != State.ERROR) {
            setState(State.FINISHED);
         }
         operationFinalize();
      }
   }

   protected abstract void operationInitialize() throws Exception;

   private boolean running() {
      return running.get();
   }

   protected abstract void loopBlock() throws Exception;

   protected abstract void handleError(Throwable exception);

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
