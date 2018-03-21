package org.swarmer.util;

import org.junit.Test;

import java.io.IOException;

public class SwarmExecutorTest {

   private static String BIND_ADDRESS_PARAM_02 = "-Dswarm.bind.address=127.0.0.1";
   private static String FILE_ENC_PARAM_01     = "-Dfile.encoding=UTF-8";
   private static String JAR_FILE_PARAM_04     = "-jar";
   private static String JAR_FILE_PARAM_05     = "D:\\programming\\java\\test-swarm-app\\target\\demo-swarm.jar";
   private static String JAVA_EXE              = "C:\\winapp\\Java\\1.8.0_102\\X64\\JDK\\bin\\java.exe";

   @Test
   public void executeCommand() {
      try {
         Process process = SwarmExecutor
                 .executeCommand(JAVA_EXE, FILE_ENC_PARAM_01, BIND_ADDRESS_PARAM_02, JAR_FILE_PARAM_04,
                                 JAR_FILE_PARAM_05);

         SwarmExecutor.destroy(process);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}