package org.swarmer.context;

import java.io.File;

public class SwarmJob implements Cloneable {
   private static final Object LOCK_STATE = new Object();
   private final        Action action;
   private final        String containerName;
   private final        int    instances;
   private final        int    pid;
   private final        String windowTitle;
   private              State  state;
   private              File   swarmJarFile;

   public static Builder builder() {
      return new Builder();
   }

   private SwarmJob(Builder builder) {
      containerName = builder.containerName;
      action = builder.action;
      instances = builder.instances;
      pid = builder.pid;
      swarmJarFile = builder.swarmJarFile;
      windowTitle = builder.windowTitle;
      state = State.WAITING;
   }

   public Action getAction() {
      return action;
   }

   public String getContainerName() {
      return containerName;
   }

   public int getInstances() {
      return instances;
   }

   public int getPid() {
      return pid;
   }

   public State getState() {
      return state;
   }

   public void setState(State state) {
      synchronized (LOCK_STATE) {
         this.state = state;
         LOCK_STATE.notifyAll();
      }
   }

   public File getSwarmJarFile() {
      return swarmJarFile;
   }

   public void setSwarmJarFile(File swarmJarFile) {
      this.swarmJarFile = swarmJarFile;
   }

   public String getWindowTitle() {
      return windowTitle;
   }

   public boolean isRunAppend() {
      return action == Action.RUN_APPEND;
   }

   public boolean isRunNew() {
      return action == Action.RUN_NEW;
   }

   public boolean isFinished() {
      return (state != State.FINISHED) && (state != State.ERROR);
   }

   public enum Action {
      RUN_NEW, RUN_APPEND, KILL_LAST_INSTANCE
   }

   public static class Builder {
      private Action action;
      private String containerName;
      private int    instances;
      private int    pid;
      private File   swarmJarFile;
      private String windowTitle;

      private Builder() {
         instances = 0;
         pid = -1;
         swarmJarFile = null;
         windowTitle = null;
         containerName = null;
      }

      public Builder action(Action action) {
         this.action = action;
         return this;
      }

      public SwarmJob build() {
         return new SwarmJob(this);
      }

      public Builder containerName(String containerName) {
         this.containerName = containerName;
         return this;
      }

      public Builder instances(int instances) {
         this.instances = instances;
         return this;
      }

      public Builder pid(int pid) {
         this.pid = pid;
         return this;
      }

      public Builder swarmJarFile(File swarmJarFile) {
         this.swarmJarFile = swarmJarFile;
         return this;
      }

      public Builder windowTitle(String windowTitle) {
         this.windowTitle = windowTitle;
         return this;
      }
   }
}
