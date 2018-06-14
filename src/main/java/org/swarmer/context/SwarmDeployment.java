package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.util.SwarmUtil;

import java.io.File;

public class SwarmDeployment {
   private static final Logger    LOG = LogManager.getLogger(SwarmDeployment.class);
   private              int       pid;
   private              long      processTimeStart;
   private              SwarmFile swarmFile;
   private              String    windowTitle;

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(SwarmDeploymentCfg swarmDeploymentCfg) {
      return new Builder(swarmDeploymentCfg);
   }

   private SwarmDeployment(Builder builder) {
      this.pid = builder.pid;
      this.processTimeStart = builder.timeStart;
      this.swarmFile = builder.swarmFile;
      this.windowTitle = builder.windowTitle;
      swarmFile.setState(SwarmFile.State.SWARM_STARTED, null);
   }

   public String getWindowTitle() {
      return windowTitle;
   }

   public void hardKillSwarm() {
      SwarmUtil.killSwarmWindow(windowTitle);
   }

   public boolean sigIntProces() {
      int pid = getPid();
      return (pid != -1) && SwarmUtil.sigIntSwarm(pid);
   }

   public int getPid() {
      return pid;
   }

   public boolean waitForSwarmToShutdown() {
      boolean swarmExited = SwarmUtil.waitUntilSwarmProcExits(swarmFile.getFilename(), processTimeStart, 300, 1000);
      return swarmExited;
   }

   public static class Builder {
      private int       pid;
      private SwarmFile swarmFile;
      private long      timeStart;
      private String    windowTitle;

      private Builder() {
         pid = -1;
         swarmFile = null;
         timeStart = -1;
         windowTitle = null;
      }

      private Builder(SwarmDeploymentCfg swarmDeploymentCfg) {
         pid = swarmDeploymentCfg.getPid() != null ? swarmDeploymentCfg.getPid().intValue() : -1;
         File file = new File(swarmDeploymentCfg.getSwarmFilePath());
         swarmFile = new SwarmFile(file, SwarmFile.State.SWARM_STARTED, -1);
         timeStart = -1;
         windowTitle = swarmDeploymentCfg.getWindowTitle();
      }

      public SwarmDeployment build() {
         SwarmDeployment result = new SwarmDeployment(this);
         return result;
      }

      public Builder file(SwarmFile swarmFile) {
         this.swarmFile = swarmFile;
         return this;
      }

      public Builder pid(int pid) {
         this.pid = pid;
         return this;
      }

      public Builder timeStart(long timeStart) {
         this.timeStart = timeStart;
         return this;
      }

      public Builder windowTitle(String windowTitle) {
         this.windowTitle = windowTitle;
         return this;
      }
   }
}
