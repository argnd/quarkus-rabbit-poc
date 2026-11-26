package com.example.orders.api;

import com.example.orders.model.OrderCreated;
import com.example.orders.service.OrderService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/orders")
public class OrderResource {

    private final OrderService orderService;

    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    @GET
    public List<OrderCreated> list() {
        return orderService.findAll();
    }
}
