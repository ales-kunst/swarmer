package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SwarmExecutor {
   private static final Logger LOG               = LogManager.getLogger(SwarmExecutor.class);
   private static final String SWARM_STARTED_TXT = "WildFly Swarm is Ready";

   public static void destroy(Process process) {
      process.destroy();
   }

   public static Process executeCommand(String... command) throws IOException {
      Process process = null;

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      process = processBuilder.start();


      String line;

      if (!isProcessRunning(process)) {
         InputStream    stdErr          = process.getErrorStream();
         BufferedReader errStreamReader = new BufferedReader(new InputStreamReader(stdErr));
         StringBuffer   errorContent    = new StringBuffer();
         while ((line = errStreamReader.readLine()) != null) {
            errorContent.append(line);
         }
         LOG.error("Error stream in executeCommand:\n{}\n", errorContent.toString());
         CloseableUtil.close(errStreamReader);
      }

      InputStream    stdOut          = process.getInputStream();
      BufferedReader stdStreamReader = new BufferedReader(new InputStreamReader(stdOut));
      StringBuffer   stdOutContent   = new StringBuffer();
      while ((line = stdStreamReader.readLine()) != null) {
         stdOutContent.append(line + "\n");
         if (line.contains(SWARM_STARTED_TXT)) {
            break;
         }
      }
      LOG.debug("Std stream in executeCommand:\n{}\n", stdOutContent.toString());
      CloseableUtil.close(stdStreamReader);

      return process;
   }

   public static boolean isProcessRunning(Process process) {
      try {
         process.exitValue();
         return false;
      } catch (Exception e) {}
      return true;
   }

   private SwarmExecutor() {}
}
