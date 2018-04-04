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
   private static final String CMD_COMMAND           = "cmd.exe";
   private static final String JAR_OPTION            = "-jar";
   private static final String JAVA_COMMAND          = "java";
   private static final String JVM_SWARM_PORT_OPTION = "-Dswarm.http.port=";
   private static final Logger LOG                   = LogManager.getLogger(SwarmExecutor.class);
   private static final String RUN_COMMAND_OPTION    = "/c";
   private static final String START_COMMAND         = "start";
   private static final String START_PATH_OPTION     = "/D";
   private static final String SWARM_STARTED_TXT     = "WildFly Swarm is Ready";


   public static String[] createSwarmCliArguments(String windowTitle, String port, String jvmArgs, String appArgs,
                                                  File swarmJar) {
      final String JVM_ARG_DELIMETER = "-";
      final String APP_ARG_DELIMETER = " ";
      List<String> cliArgs           = new ArrayList<>();
      cliArgs.add(CMD_COMMAND);
      cliArgs.add(RUN_COMMAND_OPTION);
      cliArgs.add(START_COMMAND);
      cliArgs.add(windowTitle);
      if (swarmJar.getParent() != null) {
         cliArgs.add(START_PATH_OPTION);
         cliArgs.add(swarmJar.getParent());
      }
      cliArgs.add(JAVA_COMMAND);
      cliArgs.add(JVM_SWARM_PORT_OPTION + port);
      cliArgs.addAll(parseArgs(jvmArgs, JVM_ARG_DELIMETER));
      cliArgs.add(JAR_OPTION);
      cliArgs.add(swarmJar.getName());
      cliArgs.addAll(parseArgs(appArgs, APP_ARG_DELIMETER));
      return cliArgs.toArray(new String[cliArgs.size()]);
   }

   private static List<String> parseArgs(String args, String delimiter) {
      List<String> resultArgs = new ArrayList<>();
      Scanner      sc         = new Scanner(args).useDelimiter(delimiter);
      String       arg;

      while (sc.hasNext()) {
         arg = delimiter + sc.next();
         resultArgs.add(arg.trim());
      }
      CloseableUtil.close(sc);
      return resultArgs;
   }

   public static void destroy(Process process) {
      process.destroy();
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
         LOG.error("Error executing executeLongLivingCommand:\n{}\n", errorContent.toString());
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

   public static boolean isProcessRunning(Process process) {
      try {
         process.exitValue();
         return false;
      } catch (Exception e) {}
      return true;
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

   public static boolean killSwarmWindow(String windowName) {
      boolean       success       = true;
      ProcessResult processResult = null;
      try {
         String filter = "\"WindowTitle eq " + windowName + "*\"";
         // taskkill /FI "WindowTitle eq Administrator:  CALCULATOR GREEN*"
         Future<ProcessResult> future = new ProcessExecutor().command("taskkill", "/FI", filter).readOutput(true)
                                                             .start().getFuture();
         processResult = future.get(60, TimeUnit.SECONDS);
         LOG.info(processResult.outputUTF8());
      } catch (Exception e) {
         LOG.error("Error executing killSwarmWindow: {}", e);
      }
      if (processResult == null || processResult.getExitValue() != 0) {
         success = false;
      }
      return success;
   }

   public static Process startSwarmInstance(String... command) {
      Process process = null;
      try {
         process = new ProcessExecutor().command(command).readOutput(true).start().getProcess();
      } catch (Exception e) {
         LOG.error("Error executing startSwarmInstance: {}", e);
      }
      return process;
   }

   public static boolean waitFor(long millis) {
      boolean success = true;
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
         success = false;
      }

      return success;
   }

   private SwarmExecutor() {}
}
