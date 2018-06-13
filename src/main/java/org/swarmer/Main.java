package org.swarmer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerContext;
import org.swarmer.context.SwarmerContextRetriever;
import org.swarmer.util.FileUtil;

import java.io.File;


public class Main {
   private static final int    ERROR_STATUS = 8;
   private static final Logger LOG          = LogManager.getLogger(Main.class);

   public static void main(String[] args) {
      try {
         LOG.info("Program started!!!");

         new File(FileUtil.KILL_APP_PATH).delete();
         new File(FileUtil.WIN_TEE_APP_PATH).delete();
         FileUtil.copyWindowsKillAppToTmp();
         FileUtil.copyWinTeeAppToTmp();

         SwarmerContext ctx = SwarmerContextRetriever.retrieve(SwarmerInputParams.jsonFilePath());
         MainExecutor.cliArgs(args)
                     .startOperations(ctx)
                     .startRestServer(ctx)
                     .waitToEnd();
         if (MainExecutor.finishedWithErrors()) {
            System.exit(ERROR_STATUS);
         }
      } catch (Exception e) {
         LOG.error("Swarmer ended with error:\n {}", ExceptionUtils.getStackTrace(e));
         System.exit(ERROR_STATUS);
      }
   }
}
