package org.swarmer.util;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.SwarmerCtx;

import java.io.File;
import java.io.IOException;

public class SwarmUtilTest {
   private static final int    DEFAULT_SWARM_STARTUP_TIME = 300;
   private static final Logger LOG                        = LoggerFactory.getLogger(SwarmerCtx.class);
   private static final String TMP_FOLDER                 = System.getProperty("java.io.tmpdir");
   private static final String CONSUL_EXEC_PATH           = TMP_FOLDER + "\\consul.exe";

   public static Process consulProcess = null;

   @BeforeClass
   public static void setUpBeforeClass() {
      try {
         FileUtil.copyWinTeeAppToTmp();
         FileUtil.copyWindowsKillAppToTmp();
         TestUtil.copyConsulExecToTmp();
         consulProcess = TestUtil.startConsul();
         TestUtil.waitForConsulToStart();
      } catch (Exception e) {
         LOG.error("Error when trying to start Consul: {}", e);
         throw new RuntimeException(e);
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
      String     jarPath = ".\\src\\scrapbook\\test-rest-app\\target\\demo-rest.jar";
      File       jarFile = new File(jarPath);
      final long uid     = System.currentTimeMillis();
      String[] commandLine = SwarmUtil.createSwarmCliArguments("Blue Instance",
                                                               "8080", "-Djava.io.tmpdir=D:\\temp\\some_tmp", uid,
                                                               "-S appArg", jarFile);

      Assert.assertEquals("cmd.exe", commandLine[0]);
      Assert.assertEquals("/c", commandLine[1]);
      Assert.assertEquals("start", commandLine[2]);
      Assert.assertEquals("Blue Instance", commandLine[3]);
      Assert.assertEquals("/D", commandLine[4]);
      Assert.assertEquals(".\\src\\scrapbook\\test-rest-app\\target", commandLine[5]);
      Assert.assertEquals(FileUtil.WIN_TEE_APP_PATH, commandLine[6]);
      Assert.assertEquals("java", commandLine[7]);
      Assert.assertEquals((SwarmUtil.UID_JVM_ARG + uid), commandLine[8]);
      Assert.assertEquals("-Dswarm.http.port=8080", commandLine[9]);
      Assert.assertEquals("-Djava.io.tmpdir=D:\\temp\\some_tmp", commandLine[10]);
      Assert.assertEquals("-jar", commandLine[11]);
      Assert.assertEquals(jarFile.getName(), commandLine[12]);
      Assert.assertEquals("-S", commandLine[13]);
      Assert.assertEquals("appArg", commandLine[14]);
   }

   @Test
   public void testGetSwarmPID() throws IOException {
      long uid = startSwarm();
      int  pid = SwarmUtil.getSwarmPid("demo-rest.jar", uid);
      Assert.assertTrue(pid != -1);
      FileUtil.copyWindowsKillAppToTmp();
      SwarmUtil.sigIntSwarm(pid);
      waitSwarmToShutDown("demo-rest.jar", uid);
   }

   private long startSwarm() throws IOException {
      final String WINDOW_NAME = "Blue Instance";
      String       jarPath     = ".\\src\\scrapbook\\test-rest-app\\target\\demo-rest.jar";
      File         jarFile     = new File(jarPath);
      final long   uid         = System.currentTimeMillis();
      final String jvmArgs     = "-Duid=" + uid + " -Djava.io.tmpdir=D:\\temp\\some_tmp";
      String[] swarmArgs = SwarmUtil.createSwarmCliArguments(WINDOW_NAME,
                                                             "8080", jvmArgs, uid, "",
                                                             jarFile);
      new File(FileUtil.WIN_TEE_APP_PATH).delete();
      FileUtil.copyWinTeeAppToTmp();
      SwarmUtil.startSwarmInstance(swarmArgs);
      int timeWaited = 0;
      while (SwarmUtil.waitFor(1000)) {
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
      while (SwarmUtil.waitFor(1000)) {
         int     pid                 = SwarmUtil.getSwarmPid(swarmJar, uid);
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
   public void testHealthStatusOfSwarm() throws IOException {
      final String WINDOW_NAME = "Blue Instance";
      String       jarPath     = ".\\src\\scrapbook\\test-rest-app\\target\\demo-rest.jar";
      File         jarFile     = new File(jarPath);
      final long   uid         = System.currentTimeMillis();
      final String jvmArgs     = "-Duid=" + uid + " -Djava.io.tmpdir=D:\\temp\\some_tmp";
      String[] swarmArgs = SwarmUtil.createSwarmCliArguments(WINDOW_NAME,
                                                             "8080", jvmArgs, uid, "",
                                                             jarFile);
      Process process    = SwarmUtil.startSwarmInstance(swarmArgs);
      int     timeWaited = 0;
      while (SwarmUtil.waitFor(1000)) {
         StringBuffer urlContents = NetUtils.getUrlContent("http://localhost:8500/v1/health/service/QnstMS");
         if (!urlContents.toString().equalsIgnoreCase("[]")) {
            System.out.println("URL Contents: " + urlContents.toString());
            Assert.assertTrue(SwarmUtil.killSwarmWindow(WINDOW_NAME));
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
      Assert.assertTrue("jps.exe does not exis!", SwarmUtil.javaProcessStatusToolExists());
   }

   @Test
   public void testStartSwarmInstance() {
      final String WINDOW_NAME = "Blue Instance";
      String       jarPath     = ".\\src\\scrapbook\\test-rest-app\\target\\demo-rest.jar";
      File         jarFile     = new File(jarPath);
      final long   uid         = System.currentTimeMillis();
      String[] swarmArgs = SwarmUtil.createSwarmCliArguments(WINDOW_NAME,
                                                             "8080", "-Djava.io.tmpdir=D:\\temp\\some_tmp", uid, "",
                                                             jarFile);
      Process process    = SwarmUtil.startSwarmInstance(swarmArgs);
      int     timeWaited = 0;
      while (SwarmUtil.waitFor(1000)) {
         if (!NetUtils.isPortAvailable(8080)) {
            Assert.assertTrue(true);
            break;
         }
         if (timeWaited > DEFAULT_SWARM_STARTUP_TIME) {
            Assert.assertTrue("Swarm did not START", false);
         }
         timeWaited++;
      }
      Assert.assertTrue(SwarmUtil.killSwarmWindow(WINDOW_NAME));
   }
}