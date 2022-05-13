package org.acme;

import java.net.InetAddress;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/host")
public class HostResource {

    @Inject
    Hostname hostname;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String host() {      
        return  hostname.getHostName();
    }
}