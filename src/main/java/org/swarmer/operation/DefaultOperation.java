package org.swarmer.operation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.State;

public abstract class DefaultOperation<CTX> extends SwarmerOperation<CTX> {
   private static final Logger LOG = LogManager.getLogger(DefaultOperation.class);

   public DefaultOperation(String name, CTX context) {
      super(name, context);
   }

   @Override
   public void execute() {
      setState(State.RUNNING);
      try {
         LOG.info("Starting {}", name());
         executionBock();
         setState(State.FINISHED);
      } catch (Exception e) {
         setState(State.ERROR);
         handleError(e);
      }
   }

   protected abstract void executionBock() throws Exception;

   protected abstract void handleError(Exception e);
}
