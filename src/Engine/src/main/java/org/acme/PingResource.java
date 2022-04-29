package org.acme;

import java.net.InetAddress;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.inject.Inject;

@Path("/ping")
public class PingResource {

    @Inject
    StartupEngineCounter startupCounter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        startupCounter.check();
        return "Pong!";
    }
}