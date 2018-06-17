package org.swarmer;

import java.io.File;

public class SwarmerInputParams {

   public static final String SWARMER_CONFIG_FILE_KEY = "swarmer.configuration.file";

   public static File getJsonFileFolder() {
      File file = new File(jsonAbsoluteFilePath());
      return file.getParentFile();
   }

   public static String jsonAbsoluteFilePath() {
      return System.getProperty(SWARMER_CONFIG_FILE_KEY,
                                "D:\\projects\\java\\swarmer\\src\\test\\resources\\swarmer_config.json");
   }

   public static void resetJsonAbsolutePath(String path) {
      System.setProperty(SWARMER_CONFIG_FILE_KEY, path);
   }

   private SwarmerInputParams() {
   }
}
