package org.swarmer.operation;

import java.util.HashMap;
import java.util.Map;

public abstract class SwarmerOperation<CTX> {
   public static   Map<String, State> operationsStates = new HashMap<>();
   protected final CTX                context;
   private         String             name;

   public static boolean finishedWithError() {
      synchronized (operationsStates) {
         for (State state : operationsStates.values()) {
            if (state == State.ERROR) {
               return true;
            }
         }
      }
      return false;
   }

   public SwarmerOperation(String name, CTX context) {
      this.name = name;
      this.context = context;
      setState(State.WAITING);
   }

   public abstract void cleanUp();

   public abstract void execute();

   public State getState() {
      synchronized (operationsStates) {
         return operationsStates.get(name());
      }
   }

   protected void setState(State state) {
      synchronized (operationsStates) {
         operationsStates.put(name, state);
         operationsStates.notify();
      }
   }

   public String name() {
      return name;
   }

   protected CTX getContext() {
      return context;
   }

   public enum State {
      WAITING, RUNNING, ERROR, FINISHED
   }
}
