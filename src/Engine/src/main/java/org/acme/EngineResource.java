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
        String[] hosts = null;

        try {            
            State<String> runningEngineCounterState = daprClient.getState("state", "hosts", String.class).block(); 
            if (runningEngineCounterState == null || runningEngineCounterState.getValue() == null || runningEngineCounterState.getError() != null){
                hosts = new String[] { "1"};
            }else
            {
                String existingHosts = runningEngineCounterState.getValue();
               
                if (existingHosts.length() > 0 ){
                    if (existingHosts.contains( ",")){
                        hosts = existingHosts.split(",");
                        
                    }else{
                        if(existingHosts.length() > 1){
                            hosts = new String[]{existingHosts};
                        }
                    }
                }else{
                    hosts = new String[] { "0" };
                }
               
            }

             
        }catch (Exception e) {
            logger.error("Something went wrong during dapr interaction while processing counter state.");
            logger.error(e.getMessage(), e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok(hosts).build();
    }
}