package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
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

   public static int getSwarmPID(String swarmJar, long uid) {
      int                   pid    = -1;
      Future<ProcessResult> future = null;
      try {
         future = new ProcessExecutor().command("jps.exe", "-mlv").readOutput(true)
                                       .start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         pid = parsePID(processResult.outputUTF8(), swarmJar, uid);
      } catch (Exception e) {
         LOG.error("Error executing getSwarmPID: {}", e);
      }
      return pid;
   }

   private static int parsePID(String content, String swarmJar, long uid) {
      int     pid  = -1;
      Scanner sc   = new Scanner(content);
      String  line = null;
      while (sc.hasNextLine() || (line == null)) {
         line = sc.nextLine();
         if (line.contains(swarmJar) && line.contains("-Duid=" + uid)) {
            Scanner sc_pid = new Scanner(line);
            pid = sc_pid.nextInt();
            break;
         }
      }
      return pid;
   }

   public static boolean javaProcessStatusToolExists() {
      boolean               success = true;
      Future<ProcessResult> future  = null;
      try {
         LOG.info("Looking up jps.exe");
         future = new ProcessExecutor().command("where", "jps.exe").readOutput(true)
                                       .start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         if (processResult.getExitValue() != 0) {
            success = false;
         }
      } catch (Exception e) {
         LOG.error("Error executing javaProcessStatusToolExists: {}", e);
         success = false;
      }
      if (success) {
         LOG.info("jps.exe found");
      } else {
         LOG.info("jps.exe not found");
      }
      return success;
   }

   public static boolean sigIntSwarm(int pid) {
      boolean               success = false;
      Future<ProcessResult> future  = null;
      try {
         future = new ProcessExecutor().command(FileUtil.KILL_APP_PATH, "-SIGINT", Integer.toString(pid)).readOutput(
                 true)
                                       .start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         if (processResult.getExitValue() == 0) {
            success = true;
         }
      } catch (Exception e) {
         LOG.error("Error executing getSwarmPID: {}", e);
      }
      return success;
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
