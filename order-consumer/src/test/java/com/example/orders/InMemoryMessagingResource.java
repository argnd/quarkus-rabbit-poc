package com.example.orders;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.util.Map;

public class InMemoryMessagingResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return InMemoryConnector.switchIncomingChannelsToInMemory("orders");
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
