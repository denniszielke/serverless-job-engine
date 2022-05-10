package org.acme;

import java.lang.reflect.Array;
import java.net.InetAddress;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;

@Path("/deadcode")
public class DeadCodeResource {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response receive2(byte[] body) {
        
        CloudEvent event2 = null;

        try{
            event2 = new CloudEvent<>("id", "source", "type", "specversion", body);
            
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing counter state.");
            logger.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok(event2).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response receive3(byte[] body) {
        
        CloudEvent event = null;

        try{
            event = new CloudEvent<>("id", "source", "type", "specversion", "datacontenttype", body);
            
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing counter state.");
            logger.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok(event).build();
    }
}