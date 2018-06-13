package org.swarmer.util;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.health.model.HealthService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class ConsulQuery {
   private final ConsulClient     consulClientNew;

   public static ConsulQuery url(String consulUrlText) throws MalformedURLException {
      URL consulUrl = new URL(consulUrlText);
      return new ConsulQuery(consulUrl);
   }

   private ConsulQuery(URL consulUrl) {
      this.consulClientNew = new ConsulClient(consulUrl.getHost(), consulUrl.getPort());
   }

   public Check getSwarmInstance(String serviceName, String serviceId) {
      Check               resultCheck  = null;
      List<HealthService> allInstances = getAllSwarmInstances(serviceName);
      List<HealthService> result = allInstances.stream()
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

   public List<HealthService> getAllSwarmInstances(String serviceName) {
      Response<List<HealthService>> healthyServices = consulClientNew.getHealthServices(serviceName, true,
                                                                                        QueryParams.DEFAULT);

      return healthyServices.getValue();
   }
}
