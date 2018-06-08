package org.swarmer.util;

import org.junit.Test;

public class ConsulQueryTest {

   @Test
   public void healthCheckService() {
      /*
      final String serviceName = "QnstMS";

      ConsulQuery           consulQuery     = ConsulQuery.url("http://127.0.0.1:8500");
      final ExecutorService executorService = Executors.newSingleThreadExecutor();
      final Future<?> submit = executorService.submit(() -> {
         // Let checks them in occasionally, and get a list of healthy nodes.
         for (int index = 0; index < 100; index++) {
            System.out.println("Checking in....");
            final List<EndpointDefinition> serviceDefinitions = consulQuery.getAllSwarmInstances(serviceName);
            serviceDefinitions.forEach(def -> System.out.println("Service: " + def));
            Sys.sleep(10_000);
         }
      });
      long seconds = 0;
      while (!submit.isDone()) {
         Sys.sleep(1000);
         seconds++;
         System.out.println("Running second: " + seconds);
      }

      ConsulQuery               consulQuery = ConsulQuery.url("http://127.0.0.1:8500");
      final List<HealthService> qnstMS      = consulQuery.getAllSwarmInstancesNew("QnstMS");
      final Check               check       = consulQuery.getSwarmInstanceNew("QnstMS", "QnstMS:127.0.0.1:8085");
      check.getServiceId();
      */
      // TODO Implement healthCheckService test method
   }

}