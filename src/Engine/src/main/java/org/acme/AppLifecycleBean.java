package org.acme;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        
        int attempts = 3; // we try three times

        do {

            try {

                State<Integer> runningEngineState = daprClient.getState("state", "counter", Integer.class).block();

                int runningEnginesCount = 0;

                if (runningEngineState == null || runningEngineState.getValue() == null || runningEngineState.getError() != null || runningEngineState.getValue() < 1) {
                    runningEnginesCount = 0;
                } else {
                    runningEnginesCount = runningEngineState.getValue();
                    runningEnginesCount -= 1;
                }

                try {
                    StateOptions operation = new StateOptions(Consistency.STRONG, Concurrency.LAST_WRITE);
                    daprClient.saveState("state", "counter", runningEngineState.getEtag(), runningEnginesCount, operation).block();
                    attempts = 0;
                } catch (DaprException ex) {
                    logger.error(ex.toString());
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
                logger.error(e.toString());
                
            } finally {
                attempts--;
                
            }

        } while (attempts > 0);
    }

}