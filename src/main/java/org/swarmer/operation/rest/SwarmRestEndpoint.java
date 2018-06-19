package org.swarmer.operation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rapidoid.annotation.Controller;
import org.rapidoid.annotation.GET;
import org.rapidoid.annotation.Param;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.context.SwarmerCtxManager;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.json.SwarmerCfg;

@Controller("/swarm")
public class SwarmRestEndpoint {
   private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

   @GET("/start")
   public String startAdditionalSwarm(@Param("container_name") String containerName) throws Exception {
      SwarmerCtx ctx         = SwarmerCtxManager.instance().getCtx();
      SwarmerCfg cfg         = ctx.getCfg();
      boolean    canContinue = cfg.containerExists(containerName) && cfg.hasContainerDeployments(containerName);
      if (canContinue) {
         ctx.addSwarmJob(SwarmJob.builder()
                                 .action(SwarmJob.Action.RUN_APPEND)
                                 .containerName(containerName)
                                 .build());
      } else {
         ExceptionThrower.throwValidationException("There is no container with the name [" + containerName + "]"
                                                   + " or there are no deployments for this container.");
      }
      return "OK";
   }

}
