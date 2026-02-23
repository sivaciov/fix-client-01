package com.fixclient.backend.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fixclient.backend.execution.ExecutionReportEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    @Test
    void limitRequiresPrice() {
        OrderService service = new OrderService(new InMemoryOrderStore());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createOrder(new CreateOrderRequest("AAPL", OrderSide.BUY, 100, OrderType.LIMIT, null, TimeInForce.DAY)));

        assertEquals("price is required for LIMIT orders", ex.getMessage());
    }

    @Test
    void marketIgnoresPriceAndPersistsNormalizedOrder() {
        OrderService service = new OrderService(new InMemoryOrderStore());

        OrderRecord created = service.createOrder(
                new CreateOrderRequest("aapl", OrderSide.BUY, 100, OrderType.MARKET, new BigDecimal("123.45"), TimeInForce.DAY));

        assertNotNull(created.orderId());
        assertEquals(created.orderId().toString(), created.clOrdId());
        assertEquals("AAPL", created.symbol());
        assertNull(created.price());
        assertEquals(OrderStatus.NEW, created.status());
        assertEquals(1, service.listOrders().size());
        assertEquals(created.orderId(), service.listOrders().get(0).orderId());
    }

    @Test
    void applyExecutionReportUpdatesOrderStatusAndMessage() {
        OrderService service = new OrderService(new InMemoryOrderStore());
        OrderRecord created = service.createOrder(
                new CreateOrderRequest("MSFT", OrderSide.SELL, 10, OrderType.LIMIT, new BigDecimal("411.25"), TimeInForce.GTC));

        service.applyExecutionReport(new ExecutionReportEvent(
                created.clOrdId(),
                created.orderId().toString(),
                "1",
                "1",
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(7),
                BigDecimal.valueOf(411.25),
                BigDecimal.valueOf(411.30),
                BigDecimal.valueOf(3),
                "partial fill",
                Instant.parse("2026-02-23T17:30:00Z")));

        OrderRecord updated = service.getOrderById(created.orderId().toString());

        assertEquals(OrderStatus.PARTIALLY_FILLED, updated.status());
        assertEquals("partial fill", updated.message());
    }

    @Test
    void getOrderRejectsInvalidUuid() {
        OrderService service = new OrderService(new InMemoryOrderStore());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.getOrderById("not-a-uuid"));

        assertEquals("orderId must be a valid UUID", ex.getMessage());
    }

    @Test
    void getOrderReturnsNotFoundForMissingId() {
        OrderService service = new OrderService(new InMemoryOrderStore());
        UUID missing = UUID.fromString("00000000-0000-0000-0000-000000000042");

        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class, () -> service.getOrderById(missing.toString()));

        assertEquals("Order not found: " + missing, ex.getMessage());
    }
}
