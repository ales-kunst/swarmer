package org.swarmer.operation;

public abstract class DefaultOperation<CTX> extends SwarmerOperation<CTX> {

   public DefaultOperation(String name, CTX context) {
      super(name, context);
   }

   @Override
   public void execute() {
      setState(SwarmerOperation.State.RUNNING);
      try {
         executionBock();
         setState(State.FINISHED);
      } catch (Exception e) {
         setState(SwarmerOperation.State.ERROR);
         handleError(e);
      }
   }

   protected abstract void executionBock() throws Exception;

   protected abstract void handleError(Exception e);
}
