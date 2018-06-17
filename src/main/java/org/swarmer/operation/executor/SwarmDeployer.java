package org.swarmer.operation.executor;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.operation.InfiniteLoopOperation;
import org.swarmer.util.SwarmUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SwarmDeployer extends InfiniteLoopOperation {
   public static final  String               OP_NAME             = "Swarm Deployment Executor";
   private static final Logger               LOG                 = LogManager.getLogger(SwarmDeployer.class);
   private static final SwarmJob.Action[]    VALID_ACTION_VALUES = new SwarmJob.Action[]{SwarmJob.Action.RUN_NEW,
                                                                                         SwarmJob.Action.RUN_APPEND};
   private static final Set<SwarmJob.Action> VALID_ACTIONS       = new HashSet<>(Arrays.asList(VALID_ACTION_VALUES));

   public SwarmDeployer(String name, SwarmerCtx context) {
      super(name, context);
   }

   @Override
   protected void operationInitialize() { }

   @Override
   protected void loopBlock() {
      SwarmJob swarmJob = getContext().popSwarmJob(VALID_ACTIONS);
      if ((swarmJob != null) && !getContext().isDeploymentInProgress(swarmJob.getContainerName())) {
         if (swarmJob.isRunNew()) {
            executeDeploymentNew(swarmJob);
         } else if (swarmJob.isRunAppend()) {
            ExceptionThrower.throwIllegalArgumentException("RUN_APPEND is currently not supported!");
         }
      } else if (swarmJob != null) {
         getContext().addSwarmJob(swarmJob);
      }
      SwarmUtil.waitFor(1000);
   }

   private void executeDeploymentNew(SwarmJob swarmJob) {
      try {
         getContext().deploymentInProgress(swarmJob.getContainerName());
         DeploymentContainerCfg containerCfg = getContext().getDeploymentContainerCfg(swarmJob.getContainerName());
         SwarmDeploymentExecutor swarmDeploymentExecutor =
                 new SwarmDeploymentExecutor(getContext(), containerCfg, swarmJob);
         swarmDeploymentExecutor.execute();
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         getContext().clearDeploymentInProgress(swarmJob.getContainerName());
      }
   }

   @Override
   protected void handleError(Exception exception) {
      LOG.error("Exception from processWatchEvents. Continue with watch. Error stacktrace: \n {}",
                ExceptionUtils.getStackTrace(exception));
   }

   @Override
   protected void operationFinalize() {

   }

}
