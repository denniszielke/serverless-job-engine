package org.acme;

import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;

@Singleton
public class Hostname {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String hostName = null;

    public String getHostName() {
        if (hostName != null){
            return hostName;
        }
     
        try {
            InetAddress address = InetAddress.getLocalHost();
            hostName = address.getHostName();

        }catch (Exception e) {
            hostName = "unknown";
            logger.error(e.getMessage(), e);
        }

        return hostName;
    }

}
