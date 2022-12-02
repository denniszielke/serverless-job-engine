package org.acme;

import java.net.InetAddress;
import java.util.Calendar;  
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dapr.client.DaprClient;
import io.dapr.client.ObjectSerializer;
import io.dapr.client.domain.CloudEvent;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.inject.Inject;
import javax.ws.rs.POST;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

@Path("/requests")
public class ConsumerSerializerResource {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    @Inject
    Hostname hostname;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response receive(byte[] body) {
        JobRequest request = null;
        String localHostName = hostname.getHostName();

        logger.info("Triggered by subscription event on %s", localHostName);

        try{
            HashMap<String, String> data;

            request = new JobRequest();
            // request.guid = data.get("MessageId");
            // request.message = data.get("Message");
            
            logger.info(String.format("message %s has been received by %s with message %s.", request.guid, localHostName, request.message));
        }catch (Exception e) {
            logger.error("Something went wrong when retrieving cloud event.");
            logger.error(e.getMessage(), e);
            return Response.status(Status.OK).build();
        }   
       
        Calendar calendar = Calendar.getInstance();
        long seconds = calendar.getTimeInMillis();
        String blobName = Long.toString(seconds);

        try {

            String currentState = daprClient.getState("state", localHostName, String.class).block().getValue();
            
            if(currentState == null || currentState.isBlank() || currentState.equals("free"))
            {
                logger.info("accepting new job");
                daprClient.saveState("state", localHostName, "busy").block(); 
                daprClient.saveState("state", request.guid, "job accepted by " + localHostName).block(); 
            }
            else{
                logger.info("already busy");
                daprClient.saveState("state", localHostName, "busy").block(); 
                return Response.status(Status.TOO_MANY_REQUESTS).build();
            }

            // here we simulate compute heavy work.
            TimeUnit.MILLISECONDS.sleep(20000);
            
            String message = String.format("my message %s has been processed on host %s with message %s.", request.guid, localHostName, request.message);
            logger.info(message);
            byte[] bytes = message.getBytes();
            byte[] encodedBytes = Base64.getUrlEncoder().encode(bytes);

            Map<String,String> metadata = new HashMap<>();
            metadata.put("blobName", blobName + ".txt");

            daprClient.invokeBinding("output", "create", encodedBytes, metadata).block();
            
            logger.info("marking engine instance as free");
            daprClient.saveState("state", request.guid, "job completed by " + localHostName).block(); 
            daprClient.saveState("state", localHostName, "free").block(); 
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing queues.");
            logger.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok("ok").build();
    }
}