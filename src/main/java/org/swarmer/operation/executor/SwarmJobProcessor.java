package org.swarmer.operation.executor;

import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;

public abstract class SwarmJobProcessor {

   private final SwarmerCtx ctx;

   public SwarmJobProcessor(SwarmerCtx ctx) {
      this.ctx = ctx;
   }

   public abstract SwarmJobProcessor init(SwarmJob swarmJob);

   public abstract void process() throws Exception;

   protected final SwarmerCtx getCtx() {
      return ctx;
   }
}
