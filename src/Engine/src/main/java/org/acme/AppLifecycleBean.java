package org.acme;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.State;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.StateOptions.Concurrency;
import io.dapr.client.domain.StateOptions.Consistency;
import io.dapr.exceptions.DaprException;
import java.util.concurrent.TimeUnit;
import io.grpc.Status;

@ApplicationScoped
public class AppLifecycleBean {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    void onStop(@Observes ShutdownEvent ev) {               
        logger.info("The application is stopping...");

        String localHostName = null;

        try {
            InetAddress address = InetAddress.getLocalHost();
            localHostName = address.getHostName();

        }catch (Exception e) {
            localHostName = "unknown";
        }
        
        int attempts = 3; // we try three times

        do {

            try {

                State<String> runningEngineCounterState = daprClient.getState("state", "hosts", String.class).block();
                String eTag = null;
                String newHostList = null;

                if (runningEngineCounterState == null || runningEngineCounterState.getValue() == null || runningEngineCounterState.getError() != null){
                    newHostList = localHostName;
                }else
                {
                    String existingHosts = runningEngineCounterState.getValue();
                    eTag = runningEngineCounterState.getEtag();
                    if (existingHosts.length() > 0 ){
                        if (existingHosts.contains( ",")){
                            newHostList = "";
                            String[] existingHostsArray = existingHosts.split(",");
                            for (String hostString : existingHostsArray) {                                
                                if (hostString.length() > 0 && !localHostName.equals(hostString)){
                                    if (newHostList.length() > 0)
                                    {
                                        newHostList = newHostList + ",";
                                    }
                                    newHostList = newHostList + existingHosts;
                                }
                            } 
                        }else{
                            if(existingHosts.length() > 1 && !localHostName.equals(existingHosts)){
                                newHostList = existingHosts;
                            }
                        }
                    }
                   
                }

                try {
                    if (eTag != null) {
                        StateOptions operation = new StateOptions(Consistency.STRONG, Concurrency.LAST_WRITE);
                        daprClient.saveState("state", "hosts", eTag, newHostList, operation).block();
                    }else
                    {
                        daprClient.saveState("state", "hosts", newHostList).block();
                    }

                    attempts = 0;
                } catch (DaprException ex) {
                    logger.error(ex.getMessage(), ex);
                    if (ex.getErrorCode().equals(Status.Code.ABORTED.toString())) {
                        // Expected error due to etag mismatch.
                        System.out.println(String.format("Expected failure. %s", ex.getErrorCode()));
                    } else {
                        System.out.println("Unexpected exception.");
                        throw ex;
                    }
                }

            } catch (Exception e) {
                logger.error("Something went wrong during dapr state counter update.");
                logger.error(e.getMessage(), e);
                
            } finally {
                attempts--;
                
            }

        } while (attempts > 0);
    }

}