package org.acme;

import java.net.InetAddress;
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
import io.dapr.client.domain.State;
import io.dapr.utils.TypeRef;
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

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import io.vertx.core.json.JsonObject;

@Path("/receive")
public class QueueResource {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient dapr;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response receive(String body) {

        String localHostName = null;

        logger.info("Found body: " + body);

        Calendar calendar = Calendar.getInstance();
        long seconds = calendar.getTimeInMillis();
        String blobName = Long.toString(seconds);

        try {
            InetAddress address = InetAddress.getLocalHost();
            localHostName = address.getHostName();

            logger.info("localHostName : "+localHostName);
        }catch (Exception e) {
            logger.error("Something went wrong when retrieving hostname.");
        }
    
        try (DaprClient daprClient = (new DaprClientBuilder().build())) {

            String currentState = daprClient.getState("lock", localHostName, String.class).block().getValue();
            
            if(currentState == null || currentState.isBlank() || currentState.equals("free"))
            {
                logger.info("accepting new job");
                daprClient.saveState("lock", localHostName, "busy").block(); 
            }
            else{
                logger.info("already busy");
                daprClient.saveState("lock", localHostName, "busy").block(); 
                return Response.status(Status.BAD_REQUEST).build();
            }

            TimeUnit.MILLISECONDS.sleep(10000);
            
            String message = "my message from " + body + " has been processed on host " + localHostName;
            byte[] bytes = message.getBytes();
            byte[] encodedBytes = Base64.getUrlEncoder().encode(bytes);

            Map<String,String> metadata = new HashMap<>();
            metadata.put("blobName", blobName + ".txt");

            daprClient.invokeBinding("output", "create", encodedBytes, metadata).block();
            
            logger.info("marking engine instance as free");
            daprClient.saveState("lock", localHostName, "free").block(); 
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction.");
            logger.error(e.toString());
        }

        return Response.ok("ok").build();
    }
}