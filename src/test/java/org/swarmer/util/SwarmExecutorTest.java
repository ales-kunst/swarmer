package org.swarmer.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class SwarmExecutorTest {

   private static int DEFAULT_SWARM_STARTUP_TIME = 300;

   @Test
   public void testGetSwarmPID() {
      long uid = startSwarm();
      int  pid = SwarmExecutor.getSwarmPID("demo-swarm.jar", uid);
      Assert.assertTrue(pid != -1);
      FileUtil.copyWindowsKillAppToTmp();
      SwarmExecutor.sigIntSwarm(pid);
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
      Assert.assertEquals("java", commandLine[6]);
      Assert.assertEquals("-Dswarm.http.port=8080", commandLine[7]);
      Assert.assertEquals("-Djava.io.tmpdir=D:\\temp\\some_tmp", commandLine[8]);
      Assert.assertEquals("-jar", commandLine[9]);
      Assert.assertEquals(jarFile.getName(), commandLine[10]);
      Assert.assertEquals("-S", commandLine[11]);
      Assert.assertEquals("appArg", commandLine[12]);
   }

   @Test
   public void testGetJavaFolder() {
      File javaFolder = SwarmExecutor.getJavaFolder();
      Assert.assertTrue((javaFolder != null) && javaFolder.exists());
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

   @Test
   public void testJavaProcessStatusToolExists() {
      Assert.assertTrue("jps.exe does not exis!", SwarmExecutor.javaProcessStatusToolExists());
   }
}