package org.swarmer;

public class SwarmerInputParams {

   public static String jsonFilePath() {
      return System.getProperty("swarmer.configuration.file",
                                "D:\\projects\\java\\swarmer\\src\\test\\resources\\swarmer_config.json");
   }

   private SwarmerInputParams() {
   }
}
