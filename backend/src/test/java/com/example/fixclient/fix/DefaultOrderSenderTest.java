package com.example.fixclient.fix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fixclient.backend.orders.OrderSide;
import com.fixclient.backend.orders.OrderType;
import com.fixclient.backend.orders.TimeInForce;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultOrderSenderTest {

    @Test
    void rejectsWhenFixNotRunning() {
        FixInitiatorService initiatorService = new FixInitiatorService(settings -> new NoOpInitiator());
        DefaultOrderSender sender = new DefaultOrderSender(initiatorService, new QuickFixOrderMessageFactory());

        OrderSendResult result = sender.send(sampleOrder(OrderType.MARKET));

        assertFalse(result.accepted());
        assertTrue(result.message().contains("not RUNNING"));
    }

    @Test
    void acceptsWhenRunning() {
        FixInitiatorService initiatorService = new FixInitiatorService(settings -> new NoOpInitiator());
        initiatorService.start();

        DefaultOrderSender sender = new DefaultOrderSender(initiatorService, new QuickFixOrderMessageFactory());
        OrderSendResult result = sender.send(sampleOrder(OrderType.LIMIT));

        assertTrue(result.accepted());
        assertTrue(result.message().contains("accepted"));
    }

    private static OrderSubmission sampleOrder(OrderType orderType) {
        return new OrderSubmission(
                UUID.randomUUID(),
                Instant.parse("2026-02-23T00:00:00Z"),
                "AAPL",
                OrderSide.BUY,
                100,
                orderType,
                new BigDecimal("123.45"),
                TimeInForce.DAY);
    }

    private static class NoOpInitiator implements QuickFixInitiator {
        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    }
}
