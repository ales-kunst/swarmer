package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SwarmExecutor {
   private static final Logger LOG               = LogManager.getLogger(SwarmExecutor.class);
   private static final String SWARM_STARTED_TXT = "WildFly Swarm is Ready";

   public static void destroy(Process process) {
      process.destroy();
   }

   public static Process executeLongLivingCommand(String... command) throws IOException {
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
         LOG.error("Error stream in executeLongLivingCommand:\n{}\n", errorContent.toString());
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
      LOG.debug("Std stream in executeLongLivingCommand:\n{}\n", stdOutContent.toString());
      CloseableUtil.close(stdStreamReader);

      return process;
   }

   public static File getJavaFolder() {
      File file = null;
      try {
         Future<ProcessResult> future    = new ProcessExecutor().command("where", "javac.exe").readOutput(true).start()
                                                                .getFuture();
         ProcessResult         pr        = future.get(60, TimeUnit.SECONDS);
         String                output    = pr.outputUTF8();
         Scanner               sc        = new Scanner(output);
         String                firstLine = sc.nextLine();
         // Path to JDK folder
         file = new File(firstLine).getParentFile().getParentFile();
      } catch (Exception e) {
         LOG.error("Error in getJavaFolder: {}", e);
      }

      return file;
   }

   public static boolean isProcessRunning(Process process) {
      try {
         process.exitValue();
         return false;
      } catch (Exception e) {}
      return true;
   }

   public static void startNewSwarmInstance(String... command) {
      // new ProcessExecutor().command()
   }

   private SwarmExecutor() {}
}
