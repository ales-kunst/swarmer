package org.swarmer.context;

import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmDeploymentCfg;
import org.swarmer.json.SwarmerCfg;

import java.util.ArrayList;
import java.util.List;

public class CfgCreator implements CtxElementVisitor<SwarmerCfg> {

   private long                         swarmerCtxId;
   private SwarmerCfg.GeneralData       generalData;
   private List<DeploymentContainerCfg> containerCfgs;

   public CfgCreator() {
      generalData = null;
      containerCfgs = new ArrayList<>();
      swarmerCtxId = -1;
   }


   @Override
   public void visit(SwarmerCtx ctx) throws CloneNotSupportedException {
      swarmerCtxId = ctx.getId();
      generalData = (SwarmerCfg.GeneralData) ctx.swarmerCfgGeneralData().clone();
   }

   @Override
   public void visit(DeploymentContainer container) throws CloneNotSupportedException {
      DeploymentContainerCfg containerCfg = (DeploymentContainerCfg) container.deploymentContainerCfg().clone();
      containerCfg.clearSwarmDeploymentList();
      containerCfg.setWatchKeyHash(container.watchKeyHash());
      containerCfgs.add(containerCfg);
   }

   @Override
   public void visit(SwarmDeployment deployment) {
      SwarmDeploymentCfg deploymentCfg = new SwarmDeploymentCfg(deployment.deploymentColor.value(),
                                                                deployment.swarmFile.getAbsolutePath(),
                                                                deployment.pid,
                                                                deployment.windowTitle);
      getLastContainer().addSwarmDeploymentCfg(deploymentCfg);
   }

   @Override
   public SwarmerCfg getData() {
      return new SwarmerCfg(generalData, containerCfgs, swarmerCtxId);
   }

   private DeploymentContainerCfg getLastContainer() {
      int lastElemIndex = containerCfgs.size() - 1;
      return containerCfgs.isEmpty() ? null : containerCfgs.get(lastElemIndex);
   }
}
