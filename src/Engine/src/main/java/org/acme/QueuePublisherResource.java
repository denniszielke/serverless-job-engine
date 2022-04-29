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

@Path("/publish")
public class QueuePublisherResource {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response publish(String body) {

        String localHostName = null;

        logger.info("Found body: " + body);

        try {
            InetAddress address = InetAddress.getLocalHost();
            localHostName = address.getHostName();

            logger.info("localHostName : "+localHostName);
        }catch (Exception e) {
            logger.error("Something went wrong when retrieving hostname.");
        }
    
        try {
            
            String message = "my message has been created " + body + " has been processed on host " + localHostName;
            byte[] bytes = message.getBytes();
            byte[] encodedBytes = Base64.getUrlEncoder().encode(bytes);

            Map<String,String> metadata = new HashMap<>();

            daprClient.invokeBinding("queue", "create", encodedBytes, metadata).block();
            
            logger.info("marking engine instance as free"); 
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing queues.");
            logger.error(e.toString());
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok("ok").build();
    }
}