package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.util.SwarmExecutor;

public class SwarmDeployment {
   private static final Logger    LOG = LogManager.getLogger(SwarmDeployment.class);
   private              int       port;
   private              long      processTimeStart;
   private              String[]  swarmCommand;
   private              SwarmFile swarmFile;
   private              String    windowTitle;


   public SwarmDeployment(SwarmFile swarmFile, int port) {
      this.swarmFile = swarmFile;
      this.processTimeStart = System.currentTimeMillis();
      this.port = port;
   }

   public int getPort() { return port; }

   public long getProcessTimeStart() {
      return processTimeStart;
   }

   public String getLogFilename() {
      return SwarmExecutor.getLogFilename(swarmCommand);
   }

   public String[] getSwarmCommand() {
      return swarmCommand;
   }

   public SwarmFile getSwarmFile() {
      return swarmFile;
   }

   public void setSwarmCommand(String[] swarmCommand) {
      this.swarmCommand = swarmCommand;
   }

   public String getWindowTitle() {
      return windowTitle;
   }

   public void setWindowTitle(String windowTitle) {
      this.windowTitle = windowTitle;
   }

   public void hardKillSwarm() {
      SwarmExecutor.killSwarmWindow(windowTitle);
   }

   public void setSwarmState(SwarmFile.State state, Exception e) {
      swarmFile.setState(state, e);
   }

   public void sigIntProces() {
      int pid = getPid();
      if (pid != -1) {
         SwarmExecutor.sigIntSwarm(pid);
      }
   }

   public int getPid() {
      return SwarmExecutor.getSwarmPID(swarmFile.getFilename(), processTimeStart);
   }

   public boolean waitForSwarmToShutdown() {
      boolean swarmExited = SwarmExecutor.waitUntilSwarmProcExits(swarmFile.getFilename(), processTimeStart, 300, 1000);
      return swarmExited;
   }
}
