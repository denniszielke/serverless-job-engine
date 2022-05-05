package org.acme;

import javax.inject.Singleton;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.State;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.StateOptions.Concurrency;
import io.dapr.client.domain.StateOptions.Consistency;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.CommonProtos.Etag;
import io.grpc.Status;

@Singleton
public class StartupEngineCounter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    private Boolean counted = false;

    void check() {
        if (Boolean.TRUE.equals(counted)){
            return;
        }

        String localHostName = null;

        try {
            InetAddress address = InetAddress.getLocalHost();
            localHostName = address.getHostName();

        }catch (Exception e) {
            localHostName = "unknown";
        }

        localHostName = "engine--mkc23m5-5b54cc89cd-xcnwr";
        // c89cd-xcnwr,engine--mkc23m5-5b54cc89cd-42wfn,engine--mkc23m5-5b54cc89cd-xcnwr,engine--mkc23m5-5b54cc89cd-xcnwr,engine--mkc23m5-5b54cc89cd-42wfn,engine--mkc23m5-5b54cc89cd-xcnwr,engine--mkc23m5-5b54cc89cd-xcnwr,engine--mkc23m5-5b54cc89cd-42wfn,engine--9u

        logger.info("The application is registering...");

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
                            newHostList = localHostName;
                            String[] existingHostsArray = existingHosts.split(",");
                            for (String hostString : existingHostsArray) {
                                if (hostString.length() > 0 && !localHostName.equals(hostString)){
                                    newHostList = newHostList + "," + hostString;
                                }
                            } 
                        }else{
                            if(existingHosts.length() > 1 && !localHostName.equals(existingHosts)){
                                newHostList = localHostName + "," + existingHosts;
                            }
                            else{
                                newHostList = localHostName;
                            }
                        }
                    }else{
                        newHostList = localHostName;
                    }
                   
                }

                try {
                    if (eTag != null) {
                        logger.info("etag is " + eTag);
                        StateOptions operation = new StateOptions(Consistency.STRONG, Concurrency.LAST_WRITE);
                        daprClient.saveState("state", "hosts", eTag, newHostList, operation).block();
                    }else
                    {
                        daprClient.saveState("state", "hosts", newHostList).block();
                    }

                    attempts = 0;
                    counted = true;
                } catch (DaprException ex) {
                    logger.error(ex.getMessage(), ex);
                    if (ex.getErrorCode().equals(Status.Code.ABORTED.toString())) {
                        // Expected error due to etag mismatch.
                        TimeUnit.MILLISECONDS.sleep(1000);
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
