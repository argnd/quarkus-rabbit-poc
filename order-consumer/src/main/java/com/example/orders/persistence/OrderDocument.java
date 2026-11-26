package com.example.orders.persistence;

import com.example.orders.model.OrderCreated;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.math.BigDecimal;
import java.time.Instant;

@MongoEntity(collection = "orders")
public class OrderDocument extends PanacheMongoEntityBase {

    // L'identifiant de commande sert de _id : son unicité fait le dédoublonnage entre instances.
    @BsonId
    public String id;

    public String customer;

    public BigDecimal amount;

    public Instant createdAt;

    public static OrderDocument from(OrderCreated order) {
        OrderDocument document = new OrderDocument();
        document.id = order.id();
        document.customer = order.customer();
        document.amount = order.amount();
        document.createdAt = order.createdAt();
        return document;
    }

    public OrderCreated toOrderCreated() {
        return new OrderCreated(id, customer, amount, createdAt);
    }
}
