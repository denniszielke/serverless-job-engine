package org.acme;

import javax.enterprise.inject.Produces;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

/**
 * DaprConfiguration
 */
public class DaprConfiguration {

    @Produces
    DaprClient client() {
        return new DaprClientBuilder().build();
    }

}