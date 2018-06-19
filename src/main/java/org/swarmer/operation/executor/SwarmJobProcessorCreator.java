package org.swarmer.operation.executor;

import org.swarmer.context.SwarmJob.Action;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.exception.ExceptionThrower;

import java.util.HashMap;
import java.util.Map;

public class SwarmJobProcessorCreator {
   private SwarmerCtx                     ctx;
   private Map<Action, SwarmJobProcessor> jobProcessors;

   public SwarmJobProcessorCreator(SwarmerCtx ctx) {
      this.ctx = ctx;
      jobProcessors = new HashMap<>();
   }

   public SwarmJobProcessor create(Action action) {
      if (!jobProcessors.containsKey(action)) {
         SwarmJobProcessor jobProcessor = createNewJobProcessor(action);
         jobProcessors.put(action, jobProcessor);
      }
      return jobProcessors.get(action);
   }

   private SwarmJobProcessor createNewJobProcessor(Action action) {
      SwarmJobProcessor resultProcessor = null;
      switch (action) {
         case RUN_NEW:
            resultProcessor = new NewSwarmDeployment(ctx);
            break;
         case RUN_APPEND:
            resultProcessor = new AppendSwarmDeployment(ctx);
            break;
         default:
            ExceptionThrower.throwIllegalArgumentException("Illegal action " + action);
      }

      return resultProcessor;
   }
}
