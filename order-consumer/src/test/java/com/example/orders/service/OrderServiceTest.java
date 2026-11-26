package com.example.orders.service;

import com.example.orders.model.OrderCreated;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class OrderServiceTest {

    private static final int THREADS = 8;

    @Inject
    OrderService orderService;

    // Simule plusieurs instances qui reçoivent la même commande au même moment.
    @Test
    void storesSingleRowWhenSameOrderIsRegisteredConcurrently() throws Exception {
        OrderCreated order = new OrderCreated("cmd-100", "Diane Leroy", new BigDecimal("15.00"),
                Instant.parse("2026-11-26T10:15:30Z"));
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Void>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    orderService.register(order);
                    return null;
                }));
            }
            start.countDown();
            for (Future<Void> result : results) {
                result.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, orderService.findAll().stream().filter(o -> "cmd-100".equals(o.id())).count());
    }
}
