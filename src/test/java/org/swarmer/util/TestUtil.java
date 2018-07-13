package org.swarmer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.*;

public class TestUtil {
   private static final int    DEFAULT_SWARM_STARTUP_TIME = 300;
   private static final Logger LOG                        = LoggerFactory.getLogger(TestUtil.class);
   private static final String TMP_FOLDER                 = System.getProperty("java.io.tmpdir");
   private static final String CONSUL_EXEC_PATH           = TMP_FOLDER + "\\consul.exe";

   public static void copyConsulExecToTmp() throws Exception {

      InputStream  inStream  = null;
      OutputStream outStream = null;
      try {
         inStream = SwarmUtil.class.getResourceAsStream("/consul/consul.exe");
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

   public static void waitForConsulToStart() throws IOException {
      int timeWaited = 0;
      while (SwarmUtil.waitFor(1000)) {
         StringBuilder urlContents = NetUtils.getUrlContent("http://localhost:8500");
         if (!urlContents.toString().isEmpty()) {
            break;
         }
         if (timeWaited > DEFAULT_SWARM_STARTUP_TIME) {
            throw new RuntimeException("Swarm could not be started!");
         }
         timeWaited++;
      }
   }
}
