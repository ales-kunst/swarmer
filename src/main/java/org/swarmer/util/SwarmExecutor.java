package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class SwarmExecutor {
   private static final String CMD_COMMAND        = "cmd.exe";
   private static final Logger LOG                = LogManager.getLogger(SwarmExecutor.class);
   private static final String RUN_COMMAND_OPTION = "/c";
   private static final String START_COMMAND      = "start";
   private static final String START_PATH_OPTION  = "/D";
   private static final String SWARM_STARTED_TXT  = "WildFly Swarm is Ready";


   public static String[] createSwarmCliArguments(String windowTitle, String startPath, String port, String jvmArgs) {
      List<String> cliArgs = new ArrayList<>();
      cliArgs.add(CMD_COMMAND);
      cliArgs.add(RUN_COMMAND_OPTION);
      cliArgs.add(START_COMMAND);
      cliArgs.add(windowTitle);
      cliArgs.add(START_PATH_OPTION);
      cliArgs.add(startPath);
      cliArgs.addAll(parseJvmArgs(jvmArgs));
      return cliArgs.toArray(new String[cliArgs.size()]);
   }

   public static ProcessResult executeCommand(String... args) {
      try {
         /*
         Future<ProcessResult> future = new ProcessExecutor().command().readOutput(true).start()
                                                             .getFuture();
                                                           */
      } catch (Exception e) {

      }
      return null;
   }

   public static void destroy(Process process) {
      process.destroy();
   }

   public static File getJavaFolder() {
      File file = null;
      try {
         Future<ProcessResult> future = new ProcessExecutor().command("where", "javac.exe").readOutput(true).start()
                                                             .getFuture();
         ProcessResult pr        = future.get(60, TimeUnit.SECONDS);
         String        output    = pr.outputUTF8();
         Scanner       sc        = new Scanner(output);
         String        firstLine = sc.nextLine();
         // Path to JDK folder
         file = new File(firstLine).getParentFile().getParentFile();
      } catch (Exception e) {
         LOG.error("Error in getJavaFolder: {}", e);
      }

      return file;
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

   public static List<String> parseJvmArgs(String jvmArgs) {
      final String JVM_ARG_DELIM = "-";
      List<String> resultJvmArgs = new ArrayList<>();
      Scanner      sc            = new Scanner(jvmArgs);

      while (sc.hasNext(JVM_ARG_DELIM)) {
         String jvmArg = sc.next(JVM_ARG_DELIM);
         resultJvmArgs.add(JVM_ARG_DELIM + jvmArg);
      }
      return resultJvmArgs;
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
