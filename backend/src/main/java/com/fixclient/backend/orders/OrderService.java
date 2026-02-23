package com.fixclient.backend.orders;

import com.fixclient.backend.execution.ExecutionReportEvent;
import com.fixclient.backend.execution.ExecutionToOrderStatusMapper;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderStore orderStore;

    public OrderService(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    public OrderRecord createOrder(CreateOrderRequest request) {
        validate(request);

        UUID orderId = UUID.randomUUID();
        String clOrdId = orderId.toString();

        OrderRecord record = new OrderRecord(
                orderId,
                clOrdId,
                Instant.now(),
                request.symbol().trim().toUpperCase(Locale.ROOT),
                request.side(),
                request.qty(),
                request.type(),
                request.type() == OrderType.MARKET ? null : request.price(),
                request.tif(),
                OrderStatus.NEW,
                "Order created");

        orderStore.add(record);
        return record;
    }

    public List<OrderRecord> listOrders() {
        return orderStore.listRecent();
    }

    public OrderRecord getOrderById(String orderId) {
        return orderStore.findByOrderId(parseUuid(orderId))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    public void applyExecutionReport(ExecutionReportEvent event) {
        Optional<OrderRecord> order = findByIdentifiers(event.orderId(), event.clOrdId());
        if (order.isEmpty()) {
            return;
        }

        OrderStatus mappedStatus = ExecutionToOrderStatusMapper.map(event.execType(), event.ordStatus());
        if (mappedStatus == null && event.text() == null) {
            return;
        }

        OrderRecord current = order.get();
        OrderRecord updated = current.withStatusAndMessage(
                mappedStatus == null ? current.status() : mappedStatus,
                event.text());

        orderStore.update(updated);
    }

    private Optional<OrderRecord> findByIdentifiers(String orderId, String clOrdId) {
        if (orderId != null && !orderId.isBlank()) {
            try {
                Optional<OrderRecord> byOrderId = orderStore.findByOrderId(parseUuid(orderId));
                if (byOrderId.isPresent()) {
                    return byOrderId;
                }
            } catch (IllegalArgumentException ignored) {
                // Fall back to clOrdId lookup for non-UUID orderId values.
            }
        }
        if (clOrdId != null && !clOrdId.isBlank()) {
            return orderStore.findByClOrdId(clOrdId);
        }
        return Optional.empty();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("orderId must be a valid UUID");
        }
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
