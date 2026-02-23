package com.fixclient.backend.orders;

import java.util.UUID;

public record CreateOrderResponse(UUID orderId, OrderStatus status, String message) {
}
