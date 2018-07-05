package org.swarmer.operation.executor;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.State;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.operation.InfiniteLoopOperation;
import org.swarmer.util.SwarmUtil;

public class SwarmJobExecutor extends InfiniteLoopOperation {
   public static final  String                   OP_NAME = "Swarm Deployment Executor";
   private static final Logger                   LOG     = LoggerFactory.getLogger(SwarmJobExecutor.class);
   private final        SwarmJobProcessorCreator processCreator;
   private              SwarmJob                 swarmJob;

   public SwarmJobExecutor(String name, SwarmerCtx ctx) {
      super(name, ctx);
      processCreator = new SwarmJobProcessorCreator(ctx);
   }

   @Override
   protected void operationInitialize() { }

   @Override
   protected void loopBlock() throws Exception {
      swarmJob = getContext().popSwarmJob();
      if ((swarmJob != null) && !getContext().isDeploymentInProgress(swarmJob.getContainerName())) {
         swarmJob.setState(State.RUNNING);
         executeSwarmJob(swarmJob);
         swarmJob.setState(State.FINISHED);
      } else if (swarmJob != null) {
         getContext().addSwarmJob(swarmJob);
      }
      SwarmUtil.waitFor(1000);
   }

   private void executeSwarmJob(SwarmJob swarmJob) throws Exception {
      try {
         getContext().deploymentInProgress(swarmJob.getContainerName());
         SwarmJobProcessor jobProcessor = processCreator.create(swarmJob.getAction());
         jobProcessor.init(swarmJob).process();
      } finally {
         getContext().clearDeploymentInProgress(swarmJob.getContainerName());
      }
   }

   @Override
   protected void handleError(Exception exception) {
      swarmJob.setState(State.ERROR);
      LOG.warn("Exception from processWatchEvents. Continue with watch. Error stacktrace: \n {}",
               ExceptionUtils.getStackTrace(exception));
   }

   @Override
   protected void operationFinalize() {

   }
}
