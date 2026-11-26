package com.example.orders.messaging;

import com.example.orders.InMemoryMessagingResource;
import com.example.orders.model.OrderCreated;
import com.example.orders.service.OrderService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(InMemoryMessagingResource.class)
class OrderConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    OrderService orderService;

    @Test
    void storesOrderReceivedOnChannel() {
        orders().send(message("cmd-001", "Alice Martin", 42.50));

        List<OrderCreated> stored = awaitOrder("cmd-001");
        assertEquals(1, stored.size());
        OrderCreated order = stored.get(0);
        assertEquals("Alice Martin", order.customer());
        assertEquals(0, new BigDecimal("42.50").compareTo(order.amount()));
        assertEquals(Instant.parse("2026-11-26T10:15:30Z"), order.createdAt());
    }

    @Test
    void ignoresRedeliveryOfSameOrder() {
        orders().send(message("cmd-002", "Bruno Petit", 10));
        orders().send(message("cmd-002", "Bruno Petit", 10));
        orders().send(message("cmd-002-suivante", "Bruno Petit", 10));

        awaitOrder("cmd-002-suivante");
        assertEquals(1, withId("cmd-002").size());
    }

    @Test
    void keepsConsumingAfterInvalidMessage() {
        orders().send(new JsonObject().put("customer", "Sans identifiant"));
        orders().send(message("cmd-003", "Chloé Durand", 99.99));

        assertEquals(1, awaitOrder("cmd-003").size());
        assertTrue(orderService.findAll().stream().noneMatch(o -> o.id() == null));
    }

    // Le consommateur tourne sur un thread worker : les messages sont traités dans l'ordre, mais après send().
    private List<OrderCreated> awaitOrder(String id) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (withId(id).isEmpty() && System.nanoTime() < deadline) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        return withId(id);
    }

    private InMemorySource<JsonObject> orders() {
        return connector.source("orders");
    }

    private List<OrderCreated> withId(String id) {
        return orderService.findAll().stream().filter(o -> id.equals(o.id())).toList();
    }

    private static JsonObject message(String id, String customer, double amount) {
        return new JsonObject()
                .put("id", id)
                .put("customer", customer)
                .put("amount", amount)
                .put("createdAt", "2026-11-26T10:15:30Z");
    }
}
