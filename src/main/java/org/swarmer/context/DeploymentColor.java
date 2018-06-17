package org.swarmer.context;

public enum DeploymentColor {
   BLUE("blue"), GREEN("green");

   private String value;

   public static DeploymentColor value(String value) {
      DeploymentColor resultColor = null;
      if ("green".equalsIgnoreCase(value)) {
         resultColor = GREEN;
      } else if ("blue".equalsIgnoreCase(value)) {
         resultColor = BLUE;
      }
      return resultColor;
   }

   DeploymentColor(String value) {
      this.value = value;
   }

   public String value() {
      return value;
   }
}
