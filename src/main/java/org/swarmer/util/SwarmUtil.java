package org.swarmer.util;

import com.ecwid.consul.v1.health.model.Check;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


public class SwarmUtil {
   public static final  String UID_JVM_ARG           = "-Duid=";
   private static final String APP_ARG_DELIMETER     = " ";
   private static final String CMD_COMMAND           = "cmd.exe";
   private static final String JAR_OPTION            = "-jar";
   private static final String JAVA_COMMAND          = "java";
   private static final String JVM_ARG_DELIMETER     = "-D";
   private static final String JVM_SWARM_PORT_OPTION = "-Dswarm.http.port=";
   private static final Logger LOG                   = LogManager.getLogger(SwarmUtil.class);
   private static final String RUN_COMMAND_OPTION    = "/c";
   private static final String START_COMMAND         = "start";
   private static final String START_PATH_OPTION     = "/D";

   public static String[] createSwarmCliArguments(String windowTitle, String port, String jvmArgs, long uid,
                                                  String appArgs,
                                                  File swarmJar) {

      List<String> cliArgs = new ArrayList<>();
      cliArgs.add(CMD_COMMAND);
      cliArgs.add(RUN_COMMAND_OPTION);
      cliArgs.add(START_COMMAND);
      cliArgs.add(windowTitle);
      if (swarmJar.getParent() != null) {
         cliArgs.add(START_PATH_OPTION);
         cliArgs.add(swarmJar.getParent());
      }
      cliArgs.add(FileUtil.WIN_TEE_APP_PATH);
      cliArgs.add(JAVA_COMMAND);
      cliArgs.add(UID_JVM_ARG + uid);
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

   public static int getSwarmPID(String swarmJar, long uid) {
      int                   pid = -1;
      Future<ProcessResult> future;
      try {
         future = new ProcessExecutor().command("jps.exe", "-mlv").readOutput(true)
                                       .start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         pid = parsePID(processResult.outputUTF8(), swarmJar, uid);
      } catch (Exception e) {
         LOG.error("Error executing getSwarmPID: {}", ExceptionUtils.getStackTrace(e));
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
         LOG.error("Error executing javaProcessStatusToolExists: {}", ExceptionUtils.getStackTrace(e));
         success = false;
      }
      if (success) {
         LOG.info("jps.exe found");
      } else {
         LOG.info("jps.exe not found");
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
         LOG.error("Error executing killSwarmWindow: {}", ExceptionUtils.getStackTrace(e));
      }
      if (processResult == null || processResult.getExitValue() != 0) {
         success = false;
      }
      return success;
   }

   public static boolean sigIntSwarm(int pid) {
      boolean               success = false;
      Future<ProcessResult> future;
      try {
         future = new ProcessExecutor().command(FileUtil.KILL_APP_PATH, "-SIGINT", Integer.toString(pid))
                                       .readOutput(true).start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         if (processResult.getExitValue() == 0) {
            success = true;
         }
      } catch (Exception e) {
         LOG.error("Error executing getSwarmPID: {}", ExceptionUtils.getStackTrace(e));
      }
      return success;
   }

   private static String getLogFilename(String[] command) {
      String logFilename;
      String jarArg = null;
      String uidArg = null;
      for (String cliArg : command) {
         if (cliArg.toLowerCase().contains(".jar")) {
            int endIndex = cliArg.indexOf(".jar");
            jarArg = cliArg.substring(0, endIndex) + "_";
         } else if (cliArg.toLowerCase().contains(UID_JVM_ARG)) {
            int startIndex = cliArg.indexOf("=");
            uidArg = cliArg.substring(startIndex + 1, cliArg.length());
         }
      }
      logFilename = jarArg != null ? jarArg : "swarm_unknown_jar_";
      logFilename = logFilename + (uidArg != null ? uidArg : System.currentTimeMillis());

      return logFilename + ".log";
   }

   public static boolean waitForServiceRegistration(String consulUrl, String serviceName, String serviceId,
                                                    int appWaitTimeoutSeconds,
                                                    long loopWaitMillis) throws IOException {
      ConsulQuery consulQuery = ConsulQuery.url(consulUrl);
      Predicate<Integer> waitForServiceRegistrationPred = (Integer time) -> {
         boolean     success            = false;
         final Check swarmInstanceCheck = consulQuery.getSwarmInstance(serviceName, serviceId);
         if ((swarmInstanceCheck != null) && swarmInstanceCheck.getStatus().equals(Check.CheckStatus.PASSING)) {
            success = true;
         }

         return success;
      };

      return waitLoop(waitForServiceRegistrationPred, appWaitTimeoutSeconds, loopWaitMillis);
   }

   private static boolean waitLoop(Predicate<Integer> predicate, int waitTimeout,
                                   long loopWaitMillis) {
      boolean successfulRun = false;
      int     timeWaited    = 0;
      while (true) {
         waitFor(loopWaitMillis);
         if (predicate.test(timeWaited)) {
            successfulRun = true;
            break;
         } else if (timeWaited > waitTimeout) {
            break;
         }
         timeWaited++;
      }
      return successfulRun;
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


   public static boolean waitUntilSwarmProcExits(int pid, int shutdownTimeoutSeconds,
                                                 long loopWaitMillis) {
      // TODO Refactor method waitUntilSwarmProcExits
      boolean processExited = false;
      int     timeWaited    = 0;
      while (true) {
         waitFor(loopWaitMillis);
         if (!pidExists(pid)) {
            processExited = true;
            break;
         } else if (timeWaited > shutdownTimeoutSeconds) {
            break;
         }
         timeWaited++;
      }
      return processExited;
   }

   public static Process startSwarmInstance(String... command) {
      Process process = null;
      try {
         String teeLogFilename = getLogFilename(command);
         process = new ProcessExecutor().command(command).environment("LOGFILE", teeLogFilename).readOutput(true)
                                        .start().getProcess();
      } catch (Exception e) {
         LOG.error("Error executing startSwarmInstance: {}", ExceptionUtils.getStackTrace(e));
      }
      return process;
   }

   private static boolean pidExists(int pid) {
      boolean               resultPidExists = false;
      Future<ProcessResult> future;
      try {
         future = new ProcessExecutor().command("jps.exe", "-mlv").readOutput(true)
                                       .start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         resultPidExists = parsePID(processResult.outputUTF8(), pid);
      } catch (Exception e) {
         LOG.error("Error executing getSwarmPID: {}", ExceptionUtils.getStackTrace(e));
      }
      return resultPidExists;
   }

   private static boolean parsePID(String content, int pid) {
      boolean resultPIDExists = false;
      if (content.isEmpty()) {
         return resultPIDExists;
      }

      Scanner sc   = new Scanner(content);
      String  line = null;
      while (sc.hasNextLine() || (line == null)) {
         line = sc.nextLine();
         Scanner sc_pid = new Scanner(line);
         if (pid == sc_pid.nextInt()) {
            resultPIDExists = true;
            break;
         }
      }

      return resultPIDExists;
   }

   private static int parsePID(String content, String swarmJar, long uid) {
      int pid = -1;

      if (content.isEmpty()) {
         return pid;
      }

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

   private SwarmUtil() {}
}
