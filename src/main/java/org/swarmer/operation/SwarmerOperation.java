package org.swarmer.operation;

import org.swarmer.context.State;

import java.util.HashMap;
import java.util.Map;

public abstract class SwarmerOperation<CTX> {
   public static final Map<String, State> operationsStates = new HashMap<>();
   protected final     CTX                context;
   private             String             name;

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

   SwarmerOperation(String name, CTX context) {
      this.name = name;
      this.context = context;
      setState(State.WAITING);
   }

   public abstract void cleanUp() throws Exception;

   public abstract void execute();

   protected CTX getContext() {
      return context;
   }

   State getState() {
      synchronized (operationsStates) {
         return operationsStates.get(name());
      }
   }

   void setState(State state) {
      synchronized (operationsStates) {
         operationsStates.put(name, state);
         operationsStates.notifyAll();
      }
   }

   public String name() {
      return name;
   }
}
