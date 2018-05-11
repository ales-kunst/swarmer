package org.swarmer.test.rest;

import org.swarmer.test.entity.Car;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/hello")
public class HelloWorldEndpoint {

   @GET
   @Produces("text/plain")
   public Response doGet() {
      return Response.ok("Hello from WildFly Swarm!").build();
   }

   @GET
   @Path("/car")
   @Produces(MediaType.APPLICATION_JSON)
   public Response doGetCar() {
      Car car = new Car();
      car.setModel(101);
      car.setName("Zastava");
      car.addColor("green");
      return Response.ok().entity(car).build();
   }

   @POST
   @Path("/car")
   @Consumes({MediaType.APPLICATION_JSON})
   @Produces({MediaType.TEXT_PLAIN})
   public Response doPostCar(Car car) {
      System.out.println("To je: " + car);
      return Response.ok("Ok").build();
   }


}