package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.SwarmerException;

public class SwarmDeployment {
   private static final Logger    LOG = LogManager.getLogger(SwarmDeployment.class);
   private              int       port;
   private              Process   process;
   private              long      processTimeStart;
   private              SwarmFile swarmFile;


   public SwarmDeployment(SwarmFile swarmFile, int port) {
      this.swarmFile = swarmFile;
      this.process = null;
      this.processTimeStart = -1;
      this.port = port;
   }

   public int getPort() { return port; }

   public long getProcessTimeStart() {
      return processTimeStart;
   }

   public SwarmFile getSwarmFile() {
      return swarmFile;
   }

   public void setProcess(Process process) throws SwarmerException {
      if (process != null) {
         String msg = String.format("Process already exists %s", process);
         LOG.error(msg);
         ExceptionThrower.throwSwarmerException(msg);
      }
      this.processTimeStart = System.currentTimeMillis();
      this.process = process;
   }

   public void setSwarmState(SwarmFile.State state, Exception e) {
      swarmFile.setState(state, e);
   }
}
