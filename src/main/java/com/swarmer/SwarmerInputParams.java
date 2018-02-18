package com.swarmer;

public class SwarmerInputParams {

   public static String iniFilePath() {
      return System.getProperty("swarm.starter.configuration.filepath", "D:\\programming\\java\\swarm-starter\\src\\test\\resources\\swarm_starter.ini");
   }

   private SwarmerInputParams() {
   }
}
