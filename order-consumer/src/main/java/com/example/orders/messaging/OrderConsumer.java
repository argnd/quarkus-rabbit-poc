package com.example.orders.messaging;

import com.example.orders.model.OrderCreated;
import com.example.orders.service.OrderService;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderConsumer {

    private final OrderService orderService;

    public OrderConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    // Une exception ici rejette le message : avec failure-strategy=reject il part en dead-letter queue.
    @Incoming("orders")
    public void consume(JsonObject payload) {
        orderService.register(payload.mapTo(OrderCreated.class));
    }
}
