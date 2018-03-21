package org.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.executor.SwarmDeployer;

public class SwarmDeployRunner extends Runner {
   private static final Logger LOG = LogManager.getLogger(SwarmDeployRunner.class);

   public SwarmDeployRunner() {
      super();
   }

   @Override
   public void runLocal() {
      SwarmDeployer swarmDeployer = new SwarmDeployer();
      swarmDeployer.start();
   }

   @Override
   protected String getName() {
      return SwarmDeployRunner.class.getName();
   }
}
