package org.swarmer;

public class SwarmerInputParams {

   public static String iniFilePath() {
      return System.getProperty("swarm.starter.configuration.filepath",
                                "D:\\programming\\java\\swarmer\\src\\test\\resources\\swarmer.ini");
   }

   private SwarmerInputParams() {
   }
}
