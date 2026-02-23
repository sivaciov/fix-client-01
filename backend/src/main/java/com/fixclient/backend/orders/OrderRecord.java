package com.fixclient.backend.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderRecord(
        UUID orderId,
        String clOrdId,
        Instant createdAt,
        String symbol,
        OrderSide side,
        Integer qty,
        OrderType type,
        BigDecimal price,
        TimeInForce tif,
        OrderStatus status,
        String message) {

    public OrderRecord withStatusAndMessage(OrderStatus nextStatus, String nextMessage) {
        return new OrderRecord(
                orderId,
                clOrdId,
                createdAt,
                symbol,
                side,
                qty,
                type,
                price,
                tif,
                nextStatus,
                nextMessage == null ? message : nextMessage);
    }
}
