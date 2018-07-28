package org.swarmer.context;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.json.SwarmDeploymentCfg;

import java.io.File;

@ToString
@Accessors(fluent = true)
public class SwarmDeployment implements CtxVisitableElement {
   @Getter
   private final String          consulServiceId;
   @Getter
   private final DeploymentColor deploymentColor;
   @Getter
   private final int             pid;
   @Getter
   private final File            swarmFile;
   @Getter
   private final String          windowTitle;

   public static SwarmDeploymentBuilder builder(SwarmDeploymentCfg deploymentCfg) {
      return new SwarmDeploymentBuilder(deploymentCfg);
   }

   public static SwarmDeploymentBuilder builder() {
      return new SwarmDeploymentBuilder();
   }

   @lombok.Builder
   private SwarmDeployment(String consulServiceId, DeploymentColor deploymentColor, int pid, File swarmFile,
                           String windowTitle) {
      this.consulServiceId = consulServiceId;
      this.deploymentColor = deploymentColor;
      this.pid = pid;
      this.swarmFile = swarmFile;
      this.windowTitle = windowTitle;
   }

   @Override
   public void visit(CtxElementVisitor visitor) throws Exception {
      visitor.visit(this);
   }

   SwarmDeploymentCfg getSwarmerDeploymentCfg() {
      return new SwarmDeploymentCfg(consulServiceId, deploymentColor.value(), swarmFile.getAbsolutePath(), pid,
                                    windowTitle);
   }

   public static class SwarmDeploymentBuilder {
      private static final Logger LOG = LoggerFactory.getLogger(SwarmDeployment.SwarmDeploymentBuilder.class);

      private SwarmDeploymentBuilder() {}

      private SwarmDeploymentBuilder(SwarmDeploymentCfg swarmDeploymentCfg) {
         deploymentColor = DeploymentColor.value(swarmDeploymentCfg.getDeploymentColor());
         pid = swarmDeploymentCfg.getPid() != null ? swarmDeploymentCfg.getPid() : -1;
         swarmFile = swarmDeploymentCfg.getSwarmFilePath() != null ? new File(swarmDeploymentCfg.getSwarmFilePath())
                                                                   : null;
         windowTitle = swarmDeploymentCfg.getWindowTitle();
      }

      public SwarmDeployment build() {
         SwarmDeployment resultDeployment = new SwarmDeployment(consulServiceId, deploymentColor, pid, swarmFile,
                                                                windowTitle);
         LOG.debug("Created {}.", resultDeployment);
         return resultDeployment;
      }
   }
}
