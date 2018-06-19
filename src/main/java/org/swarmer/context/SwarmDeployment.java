package org.swarmer.context;

import org.swarmer.json.SwarmDeploymentCfg;

import java.io.File;

public class SwarmDeployment implements CtxVisitableElement {
   private final DeploymentColor deploymentColor;
   private final int             pid;
   private final File            swarmFile;
   private final String          windowTitle;

   public static Builder builder() {
      return new Builder();
   }

   static Builder builder(SwarmDeploymentCfg swarmDeploymentCfg) {
      return new Builder(swarmDeploymentCfg);
   }

   private SwarmDeployment(Builder builder) {
      this.deploymentColor = builder.deploymentColor;
      this.pid = builder.pid;
      this.swarmFile = builder.swarmFile;
      this.windowTitle = builder.windowTitle;
   }

   public DeploymentColor deploymentColor() {
      return deploymentColor;
   }

   public int pid() {
      return pid;
   }

   public File swarmFile() {
      return swarmFile;
   }

   @Override
   public void visit(CtxElementVisitor visitor) throws Exception {
      visitor.visit(this);
   }

   public String windowTitle() {
      return windowTitle;
   }

   SwarmDeploymentCfg getSwarmerDeploymentCfg() {
      return new SwarmDeploymentCfg(deploymentColor.value(), swarmFile.getAbsolutePath(), pid, windowTitle);
   }

   public static class Builder {
      private DeploymentColor deploymentColor;
      private int             pid;
      private File            swarmFile;
      private String          windowTitle;

      private Builder() {
         pid = -1;
         swarmFile = null;
         windowTitle = null;
      }

      private Builder(SwarmDeploymentCfg swarmDeploymentCfg) {
         deploymentColor = DeploymentColor.value(swarmDeploymentCfg.getDeploymentColor());
         pid = swarmDeploymentCfg.getPid() != null ? swarmDeploymentCfg.getPid() : -1;
         swarmFile = swarmDeploymentCfg.getSwarmFilePath() != null ? new File(swarmDeploymentCfg.getSwarmFilePath())
                                                                   : null;
         windowTitle = swarmDeploymentCfg.getWindowTitle();
      }

      public SwarmDeployment build() {
         return new SwarmDeployment(this);
      }

      public Builder deploymentColor(DeploymentColor deploymentColor) {
         this.deploymentColor = deploymentColor;
         return this;
      }

      public Builder file(File swarmFile) {
         this.swarmFile = swarmFile;
         return this;
      }

      public Builder pid(int pid) {
         this.pid = pid;
         return this;
      }

      public Builder windowTitle(String windowTitle) {
         this.windowTitle = windowTitle;
         return this;
      }
   }
}
