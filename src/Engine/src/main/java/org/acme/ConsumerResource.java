package org.acme;

import java.net.InetAddress;
import java.util.Calendar;  
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dapr.client.DaprClient;
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

@Path("/requests")
public class ConsumerResource {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    @Inject
    ProcessingLockService lock;

    @Inject
    Hostname hostname;

    @Inject
    MeterRegistry registry;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response receive(CloudEvent<HashMap> event) {
        JobRequest request = null;
        String localHostName = hostname.getHostName();

        logger.info("Triggered by subscription event on %s", localHostName);

        try{
            HashMap<String, String> data;
            logger.info(String.format("Consumed contenttype: %s", event.getDatacontenttype()));
            logger.info(String.format("Consumed event id: %s", event.getId()));
            data = OBJECT_MAPPER.convertValue(event.getData(), HashMap.class);
            request = new JobRequest();
            request.guid = data.get("MessageId");
            request.message = data.get("Message");
            registry.counter("messages_counter", Tags.of("name", "received")).increment();
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

            if (!lock.isBusy())
            {
                logger.info("accepting new job");
                daprClient.saveState("state", localHostName, "busy").block(); 
                daprClient.saveState("state", request.guid, "job accepted by " + localHostName).block(); 
                registry.counter("messages_counter", Tags.of("name", "accepted")).increment();
                lock.setBusy(true);
            }            
            else{
                logger.info("already busy");
                registry.counter("messages_counter", Tags.of("name", "blocked")).increment();                
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
            lock.setBusy(false);
            registry.counter("messages_counter", Tags.of("name", "processed")).increment();
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing queues.");
            logger.error(e.getMessage(), e);
            lock.setBusy(false);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok("ok").build();
    }
}