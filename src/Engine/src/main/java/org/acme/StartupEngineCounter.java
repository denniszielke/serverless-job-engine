package org.acme;

import javax.inject.Singleton;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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

        logger.info("The application is starting...");

        int attempts = 3; // we try three times

        do {

            try {
                State<Counter> runningEngineCounterState = daprClient.getState("state", "counter", Counter.class).block();
                String eTag = null;
                Counter counter = null;

                if (runningEngineCounterState == null || runningEngineCounterState.getValue() == null || runningEngineCounterState.getError() != null){
                    counter = new Counter();
                    counter.Count = 1;
                    counter.Hosts.add(localHostName);
                }else
                {
                    counter =  runningEngineCounterState.getValue();
                    eTag = runningEngineCounterState.getEtag();
                    List<String> hosts = new ArrayList<String>();
                    for (String hostString : counter.Hosts) {
                        if (!localHostName.equals(hostString)){
                            hosts.add(hostString);
                        }
                    }
                    hosts.add(localHostName);
                    counter.Count = counter.Hosts.size();
                }

                try {
                    if (eTag != null) {
                        StateOptions operation = new StateOptions(Consistency.STRONG, Concurrency.LAST_WRITE);
                        daprClient.saveState("state", "counter", eTag, counter, operation).block();
                    }else
                    {
                        daprClient.saveState("state", "counter", counter).block();
                    }

                    attempts = 0;
                } catch (DaprException ex) {
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
                logger.error(e.toString());
                
            } finally {
                attempts--;
            }

        } while (attempts > 0);

        counted = true;
    }

}