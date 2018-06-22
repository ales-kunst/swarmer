package org.swarmer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmerCtxManager;
import org.swarmer.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;


public class Main {
   private static final int    ERROR_STATUS = 8;
   private static final Logger LOG          = LogManager.getLogger(Main.class);

   public static void main(String[] args) {
      try {
         System.out.println("\n" + getInfoTxt());

         new File(FileUtil.KILL_APP_PATH).delete();
         new File(FileUtil.WIN_TEE_APP_PATH).delete();
         FileUtil.copyWindowsKillAppToTmp();
         FileUtil.copyWinTeeAppToTmp();

         SwarmerCtxManager.on(SwarmerInputParams.jsonAbsoluteFilePath());
         MainExecutor.cliArgs(args)
                     .startOperations()
                     .startRestServer()
                     .waitToEnd();
         if (MainExecutor.finishedWithErrors()) {
            System.exit(ERROR_STATUS);
         }
      } catch (Exception e) {
         LOG.error("Swarmer ended with error:\n {}", ExceptionUtils.getStackTrace(e));
         System.exit(ERROR_STATUS);
      }
   }

   private static String getInfoTxt() {
      String      resultText = "";
      InputStream inStream   = Main.class.getResourceAsStream("/info.txt");
      try {
         StringWriter writer = new StringWriter();
         IOUtils.copy(inStream, writer, StandardCharsets.UTF_8);
         resultText = writer.toString();
      } catch (IOException e) {}
      return resultText;
   }
}
