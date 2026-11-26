package com.example.orders.model;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreated(String id, String customer, BigDecimal amount, Instant createdAt) {
}
