package com.fixclient.backend.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderRecord(
        UUID orderId,
        Instant createdAt,
        String symbol,
        OrderSide side,
        Integer qty,
        OrderType type,
        BigDecimal price,
        TimeInForce tif,
        OrderStatus status,
        String message) {
}
