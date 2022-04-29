package org.acme;

import javax.inject.Singleton;
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
import io.grpc.Status;

@Singleton
public class StartupEngineCounter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    DaprClient daprClient;

    private Boolean counted = false;

    void check() {
        if (counted){
            return;
        }

        logger.info("The application is starting...");

        int attempts = 3; // we try three times

        do {

            try {

                State<Integer> runningEngineState = daprClient.getState("state", "count", Integer.class).block();

                int runningEnginesCount = 0;

                if (runningEngineState == null || runningEngineState.getValue() == null || runningEngineState.getError() != null || runningEngineState.getValue() < 1) {
                    runningEnginesCount = 1;
                } else {
                    runningEnginesCount = runningEngineState.getValue();
                    runningEnginesCount += 1;
                }

                try {
                    StateOptions operation = new StateOptions(Consistency.STRONG, Concurrency.LAST_WRITE);
                    daprClient.saveState("state", "count", runningEngineState.getEtag(), runningEnginesCount, operation).block();
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