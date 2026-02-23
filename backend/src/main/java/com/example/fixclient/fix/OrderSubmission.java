package com.example.fixclient.fix;

import com.fixclient.backend.orders.OrderSide;
import com.fixclient.backend.orders.OrderType;
import com.fixclient.backend.orders.TimeInForce;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSubmission(
        UUID orderId,
        Instant createdAt,
        String symbol,
        OrderSide side,
        Integer qty,
        OrderType type,
        BigDecimal price,
        TimeInForce tif) {
}
