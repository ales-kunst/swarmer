package org.swarmer.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.State;

public abstract class DefaultOperation<CTX, RT> extends SwarmerOperation<CTX, RT> {
   private static final Logger LOG = LoggerFactory.getLogger(DefaultOperation.class);

   public DefaultOperation(String name, CTX context) {
      super(name, context);
   }

   @Override
   public RT execute() {
      setState(State.RUNNING);
      RT returnValue = null;
      try {
         String operationName = name();
         LOG.info("Starting Operation [{}]", operationName);
         returnValue = executionBock();
         setState(State.FINISHED);
      } catch (Exception e) {
         setState(State.ERROR);
         handleError(e);
      }
      return returnValue;
   }

   protected abstract RT executionBock() throws Exception;

   protected abstract void handleError(Exception e);
}
