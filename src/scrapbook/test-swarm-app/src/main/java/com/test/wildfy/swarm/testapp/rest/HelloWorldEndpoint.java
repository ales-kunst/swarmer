package com.test.wildfy.swarm.testapp.rest;


import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.wildfly.swarm.topology.Advertise;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

@Advertise("QnstMS")
@Path("/hello")
public class HelloWorldEndpoint {

	@GET
	@Produces("text/plain")
	public Response doGet() {
		return Response.ok("Hello from WildFly Swarm!").build();
	}
}