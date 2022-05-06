package org.acme;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dapr.client.DaprClient;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;

@Path("/publish")
public class PublisherResource {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publish(JobRequest request) {

        String localHostName = null;

        if (request == null || request.guid == null){
            return Response.status(Status.BAD_REQUEST).build();
        }

        logger.info("Received publish body: " + request.message);

        try {
            InetAddress address = InetAddress.getLocalHost();
            localHostName = address.getHostName();
            logger.info("Triggered by publish event on " + localHostName);
        }catch (Exception e) {
            logger.error("Something went wrong when retrieving hostname.");
        }
    
        try {
            
            logger.info("message " + request.guid + " has been received by " + localHostName + " with message " + request.message);

            Map<String,String> metadata = new HashMap<>();
            metadata.put("MessageId", request.guid);
            metadata.put("ContentType", "application/json");
            
            daprClient.publishEvent("requests", "requests", request, metadata).block();

            logger.info("published message on topic requests"); 
            request.message = "OK";
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing queues.");
            logger.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok(request).build();
    }
}