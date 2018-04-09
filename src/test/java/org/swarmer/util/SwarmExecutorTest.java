package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.swarmer.context.SwarmerContext;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SwarmExecutorTest {
   private static final int    DEFAULT_SWARM_STARTUP_TIME = 300;
   private static final Logger LOG                        = LogManager.getLogger(SwarmerContext.class);
   private static final String TMP_FOLDER                 = System.getProperty("java.io.tmpdir");
   private static final String CONSUL_EXEC_PATH           = TMP_FOLDER + "\\consul.exe";

   public static Process consulProcess = null;

   @BeforeClass
   public static void setUpBeforeClass() {
      try {
         FileUtil.copyWinTeeAppToTmp();
         FileUtil.copyWindowsKillAppToTmp();
         copyConsulExecToTmp();
         consulProcess = startConsul();
         waitForConsulToStart();
      } catch (Exception e) {
         LOG.error("Error when trying to start Consul: {}", e);
         throw new RuntimeException(e);
      }
   }

   public static void copyConsulExecToTmp() throws Exception {

      InputStream  inStream  = null;
      OutputStream outStream = null;
      try {
         inStream = SwarmExecutor.class.getResourceAsStream("/consul/consul.exe");
         File consulFile = new File(CONSUL_EXEC_PATH);
         consulFile.delete();
         outStream = new FileOutputStream(CONSUL_EXEC_PATH);
         byte[] buffer = new byte[1024];
         int    length = 0;
         while ((length = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, length);
         }
      } finally {
         CloseableUtil.close(inStream);
         CloseableUtil.close(outStream);
      }
   }

   public static Process startConsul() throws Exception {
      LOG.info("Starting consul.");
      return new ProcessExecutor().command(CONSUL_EXEC_PATH, "agent", "-dev").redirectOutput(System.out)
                                  .redirectError(System.out).start().getProcess();
   }

   private static void waitForConsulToStart() {
      int timeWaited = 0;
      while (SwarmExecutor.waitFor(1000)) {
         StringBuffer urlContents = NetUtils.getUrlContent("http://localhost:8500", false);
         if (!urlContents.toString().isEmpty()) {
            break;
         }
         if (timeWaited > DEFAULT_SWARM_STARTUP_TIME) {
            throw new RuntimeException("Swarm could not be started!");
         }
         timeWaited++;
      }
   }

   @AfterClass
   public static void tearDownAfterClass() {
      new File(FileUtil.KILL_APP_PATH).delete();
      new File(FileUtil.WIN_TEE_APP_PATH).delete();
      if (consulProcess != null) {
         LOG.info("Stopping Consul forcibly.");
         consulProcess.destroyForcibly();
      }

   }

   @Test
   public void testCreateSwarmCliArguments() {
      String jarPath = ".\\src\\scrapbook\\test-swarm-app\\target\\demo-swarm.jar";
      File   jarFile = new File(jarPath);
      String[] commandLine = SwarmExecutor.createSwarmCliArguments("Blue Instance",
                                                                   "8080", "-Djava.io.tmpdir=D:\\temp\\some_tmp",
                                                                   "-S appArg", jarFile);

      Assert.assertEquals("cmd.exe", commandLine[0]);
      Assert.assertEquals("/c", commandLine[1]);
      Assert.assertEquals("start", commandLine[2]);
      Assert.assertEquals("Blue Instance", commandLine[3]);
      Assert.assertEquals("/D", commandLine[4]);
      Assert.assertEquals(".\\src\\scrapbook\\test-swarm-app\\target", commandLine[5]);
      Assert.assertEquals(FileUtil.WIN_TEE_APP_PATH, commandLine[6]);
      Assert.assertEquals("java", commandLine[7]);
      Assert.assertEquals("-Dswarm.http.port=8080", commandLine[8]);
      Assert.assertEquals("-Djava.io.tmpdir=D:\\temp\\some_tmp", commandLine[9]);
      Assert.assertEquals("-jar", commandLine[10]);
      Assert.assertEquals(jarFile.getName(), commandLine[11]);
      Assert.assertEquals("-S", commandLine[12]);
      Assert.assertEquals("appArg", commandLine[13]);
   }

   @Test
   public void testGetSwarmPID() {
      long uid = startSwarm();
      int  pid = SwarmExecutor.getSwarmPID("demo-swarm.jar", uid);
      Assert.assertTrue(pid != -1);
      FileUtil.copyWindowsKillAppToTmp();
      SwarmExecutor.sigIntSwarm(pid);
      waitSwarmToShutDown("demo-swarm.jar", uid);
   }

   private long startSwarm() {
      final String WINDOW_NAME = "Blue Instance";
      String       jarPath     = ".\\src\\scrapbook\\test-swarm-app\\target\\demo-swarm.jar";
      File         jarFile     = new File(jarPath);
      final long   uid         = System.currentTimeMillis();
      final String jvmArgs     = "-Duid=" + uid + " -Djava.io.tmpdir=D:\\temp\\some_tmp";
      String[] swarmArgs = SwarmExecutor.createSwarmCliArguments(WINDOW_NAME,
                                                                 "8080", jvmArgs, "",
                                                                 jarFile);
      new File(FileUtil.WIN_TEE_APP_PATH).delete();
      FileUtil.copyWinTeeAppToTmp();
      SwarmExecutor.startSwarmInstance(swarmArgs);
      int timeWaited = 0;
      while (SwarmExecutor.waitFor(1000)) {
         StringBuffer urlContents = NetUtils.getUrlContent("http://localhost:8500/v1/health/service/QnstMS");
         if (!urlContents.toString().equalsIgnoreCase("[]")) {
            break;
         }
         if (timeWaited > DEFAULT_SWARM_STARTUP_TIME) {
            throw new RuntimeException("Swarm could not be started!");
         }
         timeWaited++;
      }

      return uid;
   }

   private void waitSwarmToShutDown(String swarmJar, long uid) {
      int timeWaited = 0;
      while (SwarmExecutor.waitFor(1000)) {
         int     pid                 = SwarmExecutor.getSwarmPID(swarmJar, uid);
         boolean successfuleShutdown = (pid == -1);
         if (successfuleShutdown) {
            break;
         } else if (timeWaited > DEFAULT_SWARM_STARTUP_TIME) {
            throw new RuntimeException("Swarm could not shutdown!");
         }
         timeWaited++;
      }
   }

   @Test
   public void testHealthStatusOfSwarm() {
      final String WINDOW_NAME = "Blue Instance";
      String       jarPath     = ".\\src\\scrapbook\\test-swarm-app\\target\\demo-swarm.jar";
      File         jarFile     = new File(jarPath);
      final long   uid         = System.currentTimeMillis();
      final String jvmArgs     = "-Duid=" + uid + " -Djava.io.tmpdir=D:\\temp\\some_tmp";
      String[] swarmArgs = SwarmExecutor.createSwarmCliArguments(WINDOW_NAME,
                                                                 "8080", jvmArgs, "",
                                                                 jarFile);
      Process process    = SwarmExecutor.startSwarmInstance(swarmArgs);
      int     timeWaited = 0;
      while (SwarmExecutor.waitFor(1000)) {
         StringBuffer urlContents = NetUtils.getUrlContent("http://localhost:8500/v1/health/service/QnstMS");
         if (!urlContents.toString().equalsIgnoreCase("[]")) {
            System.out.println("URL Contents: " + urlContents.toString());
            Assert.assertTrue(SwarmExecutor.killSwarmWindow(WINDOW_NAME));
            break;
         }
         if (timeWaited > DEFAULT_SWARM_STARTUP_TIME) {
            Assert.assertTrue("Swarm did not START", false);
         }
         timeWaited++;
      }
   }

   @Test
   public void testJavaProcessStatusToolExists() {
      Assert.assertTrue("jps.exe does not exis!", SwarmExecutor.javaProcessStatusToolExists());
   }

   @Test
   public void testStartSwarmInstance() {
      final String WINDOW_NAME = "Blue Instance";
      String       jarPath     = ".\\src\\scrapbook\\test-swarm-app\\target\\demo-swarm.jar";
      File         jarFile     = new File(jarPath);
      String[] swarmArgs = SwarmExecutor.createSwarmCliArguments(WINDOW_NAME,
                                                                 "8080", "-Djava.io.tmpdir=D:\\temp\\some_tmp", "",
                                                                 jarFile);
      Process process    = SwarmExecutor.startSwarmInstance(swarmArgs);
      int     timeWaited = 0;
      while (SwarmExecutor.waitFor(1000)) {
         if (!NetUtils.isPortAvailable(8080)) {
            Assert.assertTrue(true);
            break;
         }
         if (timeWaited > DEFAULT_SWARM_STARTUP_TIME) {
            Assert.assertTrue("Swarm did not START", false);
         }
         timeWaited++;
      }
      Assert.assertTrue(SwarmExecutor.killSwarmWindow(WINDOW_NAME));
   }
}