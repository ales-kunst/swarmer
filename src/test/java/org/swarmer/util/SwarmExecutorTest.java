package org.swarmer.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class SwarmExecutorTest {

   private static String BIND_ADDRESS_PARAM_02 = "-Dswarm.bind.address=127.0.0.1";
   private static String FILE_ENC_PARAM_01     = "-Dfile.encoding=UTF-8";
   private static String JAR_FILE_PARAM_04     = "-jar";
   private static String JAR_FILE_PARAM_05     = "D:\\programming\\java\\test-swarm-app\\target\\demo-swarm.jar";
   private static String JAVA_EXE              = "C:\\winapp\\Java\\1.8.0_102\\X64\\JDK\\bin\\java.exe";

   @Test
   public void testCreateSwarmCliArguments() {
      String jarPath = ".\\src\\scrapbook\\test-swarm-app\\target\\demo-swarm.jar";
      String[] commandLine = SwarmExecutor.createSwarmCliArguments("Blue Instance",
                                                                   "8080", "", new File(jarPath));
      // start "Blue Instance" /D D:\Path\To\Some\Swarm\Instance java -Dswarm.bind.address=127.0.0.1 -Dfile.encoding=UTF-8 -Dswarm.http.port=8085 -jar demo-swarm.jar
      Assert.assertEquals("start", commandLine[0]);
      Assert.assertEquals("Blue Instance", commandLine[1]);
      Assert.assertEquals("/D", commandLine[2]);
      Assert.assertEquals("D:\\Path\\To\\Some\\Swarm\\Instance", commandLine[3]);
      Assert.assertEquals("java", commandLine[3]);
      Assert.assertEquals("-Dswarm.http.port=8080", commandLine[3]);
      Assert.assertEquals("-Dswarm.http.port=8080", commandLine[3]);
      Assert.assertEquals("-jar", commandLine[3]);
      Assert.assertEquals("-jar", commandLine[3]);
      File f = new File("");
      System.out.println(f.getAbsolutePath());
   }

   @Test
   public void testExecuteCommand() {
      /*
      try {

         Process process = SwarmExecutor.executeLongLivingCommand(JAVA_EXE, FILE_ENC_PARAM_01, BIND_ADDRESS_PARAM_02,
                                                                  JAR_FILE_PARAM_04, JAR_FILE_PARAM_05);

         SwarmExecutor.destroy(process);

      } catch (IOException e) {
         e.printStackTrace();
      }
      */

   }

   @Test
   public void testGetJavaFolder() {
      File javaFolder = SwarmExecutor.getJavaFolder();
      Assert.assertTrue((javaFolder != null) && javaFolder.exists());
   }
}