package org.swarmer.context;

public enum State {
   WAITING("WAITING"), RUNNING("RUNNING"), ERROR("ERROR"), FINISHED("FINISHED");

   private final String value;

   State(String value) {
      this.value = value;
   }

   public String value() {
      return value;
   }
}