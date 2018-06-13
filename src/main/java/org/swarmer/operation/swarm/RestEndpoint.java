package org.swarmer.operation.swarm;

import org.rapidoid.annotation.Controller;
import org.rapidoid.annotation.GET;
import org.swarmer.exception.ExceptionThrower;

@Controller("/config")
public class RestEndpoint {

   @GET("/reload")
   public String doGetHello() {
      ExceptionThrower.throwRuntimeError("Helo Exception");
      return "Hello world";
   }
}
