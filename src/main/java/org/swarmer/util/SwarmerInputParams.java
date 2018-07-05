package org.swarmer.util;

import java.io.File;

public class SwarmerInputParams {

   public static final String HSQLD_FILE_KEY               = "swarmer.hsqldb.path";
   public static final String LAST_STATE_CFG_JSON_FILENAME = "swarmer_cfg_last_state.json";
   public static final String SWARMER_CONFIG_FILE_KEY      = "swarmer.configuration.file";

   public static File getConfigFile(String file) {
      File cfgDir = SwarmerInputParams.getJsonFileFolder();
      return new File(cfgDir, file);
   }

   public static File getJsonFileFolder() {
      File file = new File(jsonAbsoluteFilePath());
      return file.getParentFile();
   }

   public static String getHsqldbPath() {
      return System.getProperty(HSQLD_FILE_KEY, "./logs/log_db");
   }

   public static String jsonAbsoluteFilePath() {
      String defaultFilepath = "./conf/" + LAST_STATE_CFG_JSON_FILENAME;
      File   cfgFile         = new File(System.getProperty(SWARMER_CONFIG_FILE_KEY, defaultFilepath));
      return cfgFile.getAbsolutePath();
   }

   public static void resetJsonAbsolutePath(String path) {
      System.setProperty(SWARMER_CONFIG_FILE_KEY, path);
   }

   private SwarmerInputParams() {
   }
}
