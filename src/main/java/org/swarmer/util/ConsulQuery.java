package org.swarmer.util;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.health.model.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class ConsulQuery {
   private static final Logger       LOG = LoggerFactory.getLogger(ConsulQuery.class);
   private final        ConsulClient consulClient;

   public static ConsulQuery url(String consulUrlText) throws MalformedURLException {
      URL consulUrl = new URL(consulUrlText);
      return new ConsulQuery(consulUrl);
   }

   private ConsulQuery(URL consulUrl) {
      this.consulClient = new ConsulClient(consulUrl.getHost(), consulUrl.getPort());
   }

   public boolean deregisterCriticalServices(String serviceName) {
      List<Check> serviceChecks       = getAllCriticalServices(serviceName);
      boolean     serviceDeregistered = !serviceChecks.isEmpty();

      for (Check check : serviceChecks) {
         LOG.info("Deregistering Service from Consul [{}]", check.getServiceId());
         consulClient.agentServiceDeregister(check.getServiceId());
      }
      return serviceDeregistered;
   }

   List<Check> getAllCriticalServices(String serviceName) {
      final Response<List<Check>> healthChecksForService = consulClient.getHealthChecksForService(serviceName,
                                                                                                  QueryParams.DEFAULT);
      List<Check> checks = healthChecksForService.getValue();

      return checks.stream()
                   .filter(def -> def.getStatus() == Check.CheckStatus.CRITICAL)
                   .collect(Collectors.toList());
   }

   public Check getServiceCheck(String serviceName, String serviceId) {
      Check               resultCheck      = null;
      List<HealthService> passingInstances = getPassingServices(serviceName);
      List<HealthService> result = passingInstances.stream()
                                                   .filter(def -> def.getService().getId().equalsIgnoreCase(serviceId))
                                                   .collect(Collectors.toList());
      if (!result.isEmpty()) {
         List<Check> checks = result.get(0).getChecks().stream()
                                    .filter(check -> check.getServiceId().equalsIgnoreCase(serviceId))
                                    .collect(Collectors.toList());
         resultCheck = checks.get(0);
      }

      return resultCheck;
   }

   private List<HealthService> getPassingServices(String serviceName) {
      Response<List<HealthService>> healthyServices = consulClient.getHealthServices(serviceName, true,
                                                                                     QueryParams.DEFAULT);

      return healthyServices.getValue();
   }
}
