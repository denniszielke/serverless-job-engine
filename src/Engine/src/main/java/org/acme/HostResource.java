package org.acme;

import java.net.InetAddress;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/host")
public class HostResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String host() {
        String localHostName = null;

        try {
            InetAddress address = InetAddress.getLocalHost();
            localHostName = address.getHostName();

        }catch (Exception e) {
            
        }
        
        return localHostName;
    }
}