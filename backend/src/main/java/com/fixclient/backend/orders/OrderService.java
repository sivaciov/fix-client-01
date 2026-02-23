package com.fixclient.backend.orders;

import com.example.fixclient.fix.OrderSendResult;
import com.example.fixclient.fix.OrderSender;
import com.example.fixclient.fix.OrderSubmission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderSender orderSender;
    private final OrderStore orderStore;

    public OrderService(OrderSender orderSender, OrderStore orderStore) {
        this.orderSender = orderSender;
        this.orderStore = orderStore;
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        validate(request);

        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        BigDecimal normalizedPrice = request.type() == OrderType.MARKET ? null : request.price();

        OrderSubmission submission = new OrderSubmission(
                orderId,
                createdAt,
                request.symbol().trim().toUpperCase(Locale.ROOT),
                request.side(),
                request.qty(),
                request.type(),
                normalizedPrice,
                request.tif());

        OrderSendResult sendResult = orderSender.send(submission);
        OrderStatus orderStatus = sendResult.accepted() ? OrderStatus.ACCEPTED : OrderStatus.REJECTED;

        OrderRecord record = new OrderRecord(
                submission.orderId(),
                submission.createdAt(),
                submission.symbol(),
                submission.side(),
                submission.qty(),
                submission.type(),
                submission.price(),
                submission.tif(),
                orderStatus,
                sendResult.message());
        orderStore.add(record);

        return new CreateOrderResponse(record.orderId(), orderStatus, record.message());
    }

    public List<OrderRecord> listOrders() {
        return orderStore.listRecent();
    }

    private void validate(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (request.side() == null) {
            throw new IllegalArgumentException("side is required");
        }
        if (request.qty() == null || request.qty() <= 0) {
            throw new IllegalArgumentException("qty must be greater than 0");
        }
        if (request.type() == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (request.tif() == null) {
            throw new IllegalArgumentException("tif is required");
        }
        if (request.type() == OrderType.LIMIT && request.price() == null) {
            throw new IllegalArgumentException("price is required for LIMIT orders");
        }
        if (request.type() == OrderType.LIMIT && request.price().signum() <= 0) {
            throw new IllegalArgumentException("price must be greater than 0");
        }
    }
}
