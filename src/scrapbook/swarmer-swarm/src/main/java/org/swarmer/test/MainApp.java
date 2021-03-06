package org.swarmer.test;

import org.wildfly.swarm.Swarm;

public class MainApp {
   public static void main(String...args) {
      try {
         Swarm swarm = new Swarm(args);
         swarm.start().deploy();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
