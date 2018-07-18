package org.swarmer.util;

import com.ecwid.consul.v1.health.model.Check;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class SwarmUtil {
   public static final  String       JPS_EXE               = "jps.exe";
   public static final  String       UID_JVM_ARG           = "-Duid=";
   private static final String       APP_ARG_DELIMETER     = " ";
   private static final String       CMD_COMMAND           = "cmd.exe";
   private static final String       JAR_OPTION            = "-jar";
   private static final String       JAVA_COMMAND          = "java";
   public static final  ObjectMapper JSON_MAPPER           = new ObjectMapper();
   private static final String       JVM_ARG_DELIMETER     = "-D";
   private static final String       JVM_SWARM_PORT_OPTION = "-Dswarm.http.port=";
   private static final Logger       LOG                   = LoggerFactory.getLogger(SwarmUtil.class);
   private static final String       RUN_COMMAND_OPTION    = "/c";
   private static final String       START_COMMAND         = "start";
   private static final String       START_PATH_OPTION     = "/D";

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

   public static int getSwarmPid(String swarmJar, long uid) {
      int                   pid = -1;
      Future<ProcessResult> future;
      try {
         future = new ProcessExecutor().command(JPS_EXE, "-mlv").readOutput(true)
                                       .start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         pid = parsePid(processResult.outputUTF8(), swarmJar, uid);
      } catch (Exception e) {
         LOG.error("Error executing getSwarmPid: {}", ExceptionUtils.getStackTrace(e));
      }
      return pid;
   }

   private static int parsePid(String content, String swarmJar, long uid) {
      int pid = -1;

      if (content.isEmpty()) {
         return pid;
      }

      LOG.debug("Searching Swarm PID with UID [{}] in content:\n {}", uid, content);
      try (Scanner sc = new Scanner(content)) {
         String line = null;
         while (sc.hasNextLine() || (line == null)) {
            line = sc.nextLine();
            if (line.contains(swarmJar) && line.contains(UID_JVM_ARG + uid)) {
               try (Scanner scPid = new Scanner(line)) {
                  pid = scPid.nextInt();
               }
               break;
            }
         }
         LOG.debug("Found PID [{}].", pid);
      }

      return pid;
   }

   public static boolean isJarFileValid(File jarFile) {
      boolean resultJarFileValid = true;
      try (ZipFile file = new ZipFile(jarFile)) {
         Enumeration<? extends ZipEntry> entries = file.entries();
         while (entries.hasMoreElements()) {
            entries.nextElement();
         }
      } catch (Throwable ex) {
         resultJarFileValid = false;
      }
      return resultJarFileValid;
   }

   public static boolean javaProcessStatusToolExists() {
      boolean               success = true;
      Future<ProcessResult> future  = null;
      try {
         LOG.info("Looking up jps.exe");
         future = new ProcessExecutor().command("where", JPS_EXE).readOutput(true)
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
         LOG.info("Sending SIGKILL to process with WindowTitle [{}]", windowName);
         String filter = "\"WindowTitle eq " + windowName + "*\"";
         // taskkill /FI "WindowTitle eq Administrator:  CALCULATOR GREEN*"
         Future<ProcessResult> future = new ProcessExecutor().command("taskkill", "/FI", filter).readOutput(true)
                                                             .start().getFuture();
         processResult = future.get(60, TimeUnit.SECONDS);
         logExtApp("taskkill.exe", processResult.outputUTF8(), processResult.getExitValue());
      } catch (Exception e) {
         LOG.error("Error executing killSwarmWindow:\n{}", ExceptionUtils.getStackTrace(e));
      }
      if (processResult == null || processResult.getExitValue() != 0) {
         success = false;
      }
      return success;
   }

   public static boolean pidExists(int pid) {
      boolean               resultPidExists = false;
      Future<ProcessResult> future;
      try {
         LOG.debug("Checking existence of PID [{}]", pid);
         future = new ProcessExecutor().command(JPS_EXE, "-mlv").readOutput(true)
                                       .start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         resultPidExists = parsePid(processResult.outputUTF8(), pid);
         LOG.debug("PID [{}] exists: {}", pid, resultPidExists);
      } catch (Exception e) {
         LOG.error("Error executing pidExists: {}", ExceptionUtils.getStackTrace(e));
      }
      return resultPidExists;
   }

   public static boolean sigIntSwarm(int pid) {
      final String          SIGINT_SUCCESS_STDOUT_TEXT = "Signal sent successfuly.";
      boolean               success                    = false;
      Future<ProcessResult> future;
      try {
         LOG.info("Sending SIGINT to process with PID [{}]", pid);
         future = new ProcessExecutor().command(FileUtil.KILL_APP_PATH, "-SIGINT", Integer.toString(pid))
                                       .readOutput(true).start().getFuture();
         ProcessResult processResult = future.get(60, TimeUnit.SECONDS);
         String        stdOut        = processResult.outputUTF8();
         boolean       sigIntSuccess = stdOut.contains(SIGINT_SUCCESS_STDOUT_TEXT);
         // Success is 0 and Error is 8
         int exitValue = sigIntSuccess ? 0 : 8;

         logExtApp("windows-kill.exe", stdOut, exitValue);
         success = exitValue == 0;

      } catch (Exception e) {
         LOG.warn("Error executing sigIntSwarm:\n{}", ExceptionUtils.getStackTrace(e));
      }
      return success;
   }

   public static Pair<Process, String> startSwarmInstance(String... command) {
      Process process        = null;
      String  teeLogFilename = null;
      try {
         teeLogFilename = getLogFilename(command);
         process = new ProcessExecutor().command(command).environment("LOGFILE", teeLogFilename).readOutput(true)
                                        .start().getProcess();
      } catch (Exception e) {
         LOG.error("Error executing startSwarmInstance: {}", ExceptionUtils.getStackTrace(e));
      }

      return new Pair<>(process, teeLogFilename);
   }

   private static boolean parsePid(String content, int pid) {
      boolean resultPIDExists = false;
      if (content.isEmpty()) {
         return resultPIDExists;
      }

      LOG.debug("Searching for PID [{}] in content:\n {}", pid, content);
      try (Scanner sc = new Scanner(content)) {
         String line = null;
         while (sc.hasNextLine() || (line == null)) {
            line = sc.nextLine();
            try (Scanner scPid = new Scanner(line)) {
               if (pid == scPid.nextInt()) {
                  resultPIDExists = true;
                  break;
               }
            }
         }
      }

      return resultPIDExists;
   }

   public static void waitForCriticalServicesDeregistration(String consulUrl, String serviceName,
                                                            long appWaitTimeoutSeconds) {
      try {
         final ConsulQuery consulQuery = ConsulQuery.url(consulUrl);
         LOG.debug(
                 "|---> Starting Wait for Deregistering of Critical Services on Consul [Servicename: {}; Timeout: {}]",
                 serviceName, appWaitTimeoutSeconds);
         boolean anyServiceUnregistered = waitLoop((Long time) -> consulQuery.deregisterCriticalServices(serviceName),
                                                   appWaitTimeoutSeconds);
         LOG.debug("--->| End Wait for Deregistering of Critical Services on Consul [Servicename: {}; Success: {}]",
                   serviceName, anyServiceUnregistered);
      } catch (IOException ioe) {
         LOG.warn("There was an error in ConsulClient:\n {}", ExceptionUtils.getStackTrace(ioe));
      }
   }

   private static boolean waitLoop(Predicate<Long> predicate, long waitTimeoutInSec) {
      final int defaultExecWait = 1000;
      return waitLoop(predicate, waitTimeoutInSec, defaultExecWait);
   }

   private static boolean waitLoop(Predicate<Long> predicate, long waitTimeoutInSec, long nextExecInMillis) {
      boolean successfulRun       = false;
      long    startTime           = System.currentTimeMillis();
      long    timeElapsed         = 0;
      long    waitTimeoutInMillis = waitTimeoutInSec * 1000;
      while (true) {
         if (predicate.test(timeElapsed)) {
            successfulRun = true;
            break;
         }
         timeElapsed = System.currentTimeMillis() - startTime;
         if (timeElapsed > waitTimeoutInMillis) {
            break;
         }
         waitFor(nextExecInMillis);
      }
      LOG.debug("Wait loop info [success: {}; elapsed_time: {} ].", successfulRun, timeElapsed);
      return successfulRun;
   }

   public static boolean waitFor(long millis) {
      boolean success = true;
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
         success = false;
         Thread.currentThread().interrupt();
      }

      return success;
   }

   public static boolean waitForInSecs(long secs) {
      return waitFor(secs * 1000);
   }

   public static boolean waitForServiceRegistration(String consulUrl, String serviceName, String serviceId,
                                                    long appWaitTimeoutSeconds) throws IOException {
      ConsulQuery consulQuery = ConsulQuery.url(consulUrl);
      LOG.debug("|---> Starting Wait for Service Registering on Consul [Servicename: {}; ServiceId: {}; Timeout: {}]",
                serviceName, serviceId, appWaitTimeoutSeconds);
      Predicate<Long> waitForServiceRegistrationPred = (Long time) -> {
         boolean     success            = false;
         final Check swarmInstanceCheck = consulQuery.getServiceCheck(serviceName, serviceId);
         if ((swarmInstanceCheck != null) && swarmInstanceCheck.getStatus().equals(Check.CheckStatus.PASSING)) {
            success = true;
         }

         return success;
      };
      boolean resultRegistered = waitLoop(waitForServiceRegistrationPred, appWaitTimeoutSeconds);
      LOG.debug("--->| End Wait for Service Registering on Consul [Servicename: {}; ServiceId: {}; Success: {}]",
                serviceName, serviceId, resultRegistered);

      return resultRegistered;
   }

   public static boolean waitForValidJar(File jarFile, long waitTimeoutInSec) {
      boolean isJarValid;
      LOG.debug("|---> Starting Wait for valid Jar [file: {}; Timeout: {}]", jarFile.getAbsolutePath(),
                waitTimeoutInSec);
      isJarValid = waitLoop((Long timeElapsed) -> isJarFileValid(jarFile), waitTimeoutInSec, 3000);
      LOG.debug("--->| End Wait for valid Jar [file: {}; Success: {}]", jarFile.getAbsolutePath(), isJarValid);
      return isJarValid;
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
            int startIndex = cliArg.indexOf('=');
            uidArg = cliArg.substring(startIndex + 1, cliArg.length());
         }
      }
      logFilename = jarArg != null ? jarArg : "swarm_unknown_jar_";
      logFilename = logFilename + (uidArg != null ? uidArg : System.currentTimeMillis());

      return logFilename + ".log";
   }

   public static boolean waitUntilSwarmProcExits(int pid, long shutdownTimeoutInSec) {
      boolean processExited;
      LOG.debug("|---> Starting Wait for Swarm process to exit [PID: {}; Start: {}]", pid, System.currentTimeMillis());
      processExited = waitLoop((Long time) -> !pidExists(pid), shutdownTimeoutInSec);
      LOG.debug("--->| End Wait for Swarm process to exit [PID: {}; ProcessExited: {}, End: {}]", pid, processExited,
                System.currentTimeMillis());
      return processExited;
   }

   private static void logExtApp(String appName, String stdOut, int exitValue) {
      StringBuilder sb = new StringBuilder();
      sb.append("Ext App [{}] Info:\n");
      sb.append("--------------------------------------------------\n");
      sb.append("[StdOut]\n{}\n[/StdOut]\n");
      sb.append("Rc: {}\n");
      sb.append("--------------------------------------------------");
      String content = sb.toString();
      LOG.debug(content, appName, stdOut, exitValue);
   }

   private static List<String> parseArgs(String args, String delimiter) {
      List<String> resultArgs = new ArrayList<>();
      try (Scanner sc = new Scanner(args).useDelimiter(delimiter)) {
         String arg;

         while (sc.hasNext()) {
            arg = delimiter + sc.next();
            resultArgs.add(arg.trim());
         }
      }
      return resultArgs;
   }

   private SwarmUtil() {}
}
