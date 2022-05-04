package org.acme;

import java.net.InetAddress;
import javax.ws.rs.GET;
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
import io.dapr.client.domain.State;

@Path("/count")
public class EngineResource {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRunningEngines() {
        Counter counter = null;

        try {

            State<Counter> runningEngineState = daprClient.getState("state", "counter", Counter.class).block();
 
            if (runningEngineState == null || runningEngineState.getValue() == null || runningEngineState.getError() != null) {
                counter = new Counter();
                counter.Count = 1;
            } else {
                counter = runningEngineState.getValue();
            }         
             
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing counter state.");
            logger.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok(counter).build();
    }
}