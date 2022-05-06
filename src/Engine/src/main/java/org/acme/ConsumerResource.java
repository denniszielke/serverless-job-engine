package org.acme;

import java.net.InetAddress;
import java.net.URI;
import java.util.Calendar;  
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dapr.client.DaprClientGrpc;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;
import io.dapr.utils.TypeRef;
import io.dapr.client.domain.CloudEvent;

import io.vertx.core.eventbus.impl.clustered.Serializer;
import io.vertx.core.json.JsonObject;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import okhttp3.OkHttpClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.inject.Inject;
import javax.validation.constraints.Null;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

@Path("/requests")
public class ConsumerResource {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    @POST
    // @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response receive(byte[] body) {

        String localHostName = null;

        try {
            InetAddress address = InetAddress.getLocalHost();
            localHostName = address.getHostName();

            logger.info("localHostName : "+localHostName);
        }catch (Exception e) {
            logger.error("Something went wrong when retrieving hostname.");
            return Response.status(Status.BAD_REQUEST).build();
        }

        CloudEvent event = null;
        JobRequest request = null;

        try{
            event = CloudEvent.deserialize(body);
            logger.info("Consumed contenttype: " + event.getDatacontenttype());
            logger.info("Consumed event id: " + event.getId());
            request = OBJECT_MAPPER.convertValue(event.getData(), JobRequest.class);
            logger.info("message " + request.guid + " has been received by " + localHostName + " with message " + request.message);
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
                return Response.status(Status.BAD_REQUEST).build();
            }

            TimeUnit.MILLISECONDS.sleep(20000);
            
            String message = "my message from " + body + " has been processed on host " + localHostName;
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