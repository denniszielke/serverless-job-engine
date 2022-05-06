package org.acme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.protobuf.Internal.EnumVerifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

@Path("/dapr/subscribe")
public class DaprSubscriptionResource {

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRunningEngines() {

        JsonArray subscriptions = Json.createArrayBuilder()
            .add(
                Json.createObjectBuilder()
                    .add("pubsubname", "requests")
                    .add("topic", "requests")
                    .add("route", "requests")
            ).build();
       
        
        return Response.ok(subscriptions).build();
    }
}