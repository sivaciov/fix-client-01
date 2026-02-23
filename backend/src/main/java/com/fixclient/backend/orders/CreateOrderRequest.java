package com.fixclient.backend.orders;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String symbol,
        OrderSide side,
        Integer qty,
        OrderType type,
        BigDecimal price,
        TimeInForce tif) {
}
