import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

class ProcessExec {

   // C:\\winapp\\Java\\1.8.0_102\\X64\\JDK\\bin\\java.exe -Dfile.encoding=UTF-8 -Dswarm.bind.address=0.0.0.0 -jar D:\\programming\\java\\test-rest-app\\target\\demo-rest.jar
   private static String JAVA_EXE             = "C:\\winapp\\Java\\1.8.0_102\\X64\\JDK\\bin\\java.exe";
   private static String FILE_ENC_PARAM_01    = "-Dfile.encoding=UTF-8";
   private static String BINDADDRESS_PARAM_02 = "-Dswarm.bind.address=0.0.0.0";
   private static String JAR_FILE_PARAM_03    = "-jar D:\\programming\\java\\test-rest-app\\target\\demo-rest.jar";
   private static String SWARM_STARTED_TXT    = "WildFly Swarm is Ready";

   public static void main(String[] args) {
      String      line;
      InputStream stderr       = null;
      InputStream stdout       = null;
      boolean     swarmStarted = false;

      // launch EXE and grab stdin/stdout and stderr
      // new ProcessBuilder(JAVA_EXE, FILE_ENC_PARAM_01, BINDADDRESS_PARAM_02, JAR_FILE_PARAM_03).start();
      String cmdLine = JAVA_EXE + " " + FILE_ENC_PARAM_01 + " " + BINDADDRESS_PARAM_02 + " " + JAR_FILE_PARAM_03;
      // Process process = Runtime.getRuntime().exec(cmdLine);
      ProcessBuilder processBuilder = new ProcessBuilder(JAVA_EXE, FILE_ENC_PARAM_01, BINDADDRESS_PARAM_02,
                                                         JAR_FILE_PARAM_03);
      Process        process        = processBuilder.start();
      stderr = process.getErrorStream();
      stdout = process.getInputStream();

      // clean up if any output in stdout
      BufferedReader brCleanUp = new BufferedReader(new InputStreamReader(stdout));
      while ((line = brCleanUp.readLine()) != null) {
         System.out.println("[Stdout] " + line);
         if (line.contains(SWARM_STARTED_TXT)) {
            swarmStarted = true;
            break;
         }
      }
      brCleanUp.close();

      if (!swarmStarted) {
         // clean up if any output in stderr
         brCleanUp = new BufferedReader(new InputStreamReader(stderr));
         while ((line = brCleanUp.readLine()) != null) {
            System.out.println("[Stderr] " + line);
         }
         brCleanUp.close();
      }

      process.destroyForcibly();
      process.waitFor();
   }
}