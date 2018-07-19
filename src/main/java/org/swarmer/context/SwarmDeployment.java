package org.swarmer.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.json.SwarmDeploymentCfg;

import java.io.File;

public class SwarmDeployment implements CtxVisitableElement {
   private final String          consulServiceId;
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
      this.consulServiceId = builder.consulServiceId;
      this.deploymentColor = builder.deploymentColor;
      this.pid = builder.pid;
      this.swarmFile = builder.swarmFile;
      this.windowTitle = builder.windowTitle;
   }

   public String consulServiceId() { return consulServiceId; }

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
   public String toString() {
      return "SwarmDeployment [" +
             "consulServiceId='" + consulServiceId + '\'' +
             ", deploymentColor=" + deploymentColor +
             ", pid=" + pid +
             ", swarmFile=" + swarmFile +
             ", windowTitle='" + windowTitle + '\'' +
             ']';
   }

   @Override
   public void visit(CtxElementVisitor visitor) throws Exception {
      visitor.visit(this);
   }

   public String windowTitle() {
      return windowTitle;
   }

   SwarmDeploymentCfg getSwarmerDeploymentCfg() {
      return new SwarmDeploymentCfg(consulServiceId, deploymentColor.value(), swarmFile.getAbsolutePath(), pid,
                                    windowTitle);
   }

   public static class Builder {
      private static final Logger          LOG = LoggerFactory.getLogger(SwarmDeployment.Builder.class);
      private              String          consulServiceId;
      private              DeploymentColor deploymentColor;
      private              int             pid;
      private              File            swarmFile;
      private              String          windowTitle;

      private Builder() {
         consulServiceId = null;
         pid = -1;
         swarmFile = null;
         windowTitle = null;
      }

      private Builder(SwarmDeploymentCfg swarmDeploymentCfg) {
         consulServiceId = swarmDeploymentCfg.getConsulServiceId();
         deploymentColor = DeploymentColor.value(swarmDeploymentCfg.getDeploymentColor());
         pid = swarmDeploymentCfg.getPid() != null ? swarmDeploymentCfg.getPid() : -1;
         swarmFile = swarmDeploymentCfg.getSwarmFilePath() != null ? new File(swarmDeploymentCfg.getSwarmFilePath())
                                                                   : null;
         windowTitle = swarmDeploymentCfg.getWindowTitle();
      }

      public SwarmDeployment build() {
         SwarmDeployment resultDeployment = new SwarmDeployment(this);
         LOG.debug("Created {}.", resultDeployment);
         return resultDeployment;
      }

      public Builder consulServiceId(String consulServiceId) {
         this.consulServiceId = consulServiceId;
         return this;
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
