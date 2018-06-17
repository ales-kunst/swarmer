package org.swarmer.context;

import java.io.File;

public class SwarmJob implements Cloneable {

   private final Action action;
   private final String containerName;
   private final int    instances;
   private final int    pid;
   private final File   swarmJarFile;
   private final String windowTitle;

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
   }

   public Object clone() throws CloneNotSupportedException {
      return super.clone();
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

   public File getSwarmJarFile() {
      return swarmJarFile;
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

   Action getAction() {
      return action;
   }

   public enum Action {
      RUN_NEW, RUN_APPEND, KILL_INSTANCE, KILL_ALL
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
