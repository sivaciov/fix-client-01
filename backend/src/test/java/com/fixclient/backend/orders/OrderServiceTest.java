package com.fixclient.backend.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fixclient.fix.OrderSendResult;
import com.example.fixclient.fix.OrderSender;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    @Test
    void limitRequiresPrice() {
        OrderSender sender = submission -> new OrderSendResult(true, "accepted");
        OrderService service = new OrderService(sender, new InMemoryOrderStore());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createOrder(new CreateOrderRequest("AAPL", OrderSide.BUY, 100, OrderType.LIMIT, null, TimeInForce.DAY)));

        assertEquals("price is required for LIMIT orders", ex.getMessage());
    }

    @Test
    void marketIgnoresPrice() {
        OrderSender sender = submission -> {
            assertNull(submission.price());
            return new OrderSendResult(true, "accepted");
        };
        OrderService service = new OrderService(sender, new InMemoryOrderStore());

        CreateOrderResponse response = service.createOrder(
                new CreateOrderRequest("aapl", OrderSide.BUY, 100, OrderType.MARKET, new BigDecimal("123.45"), TimeInForce.DAY));

        assertNotNull(response.orderId());
        assertEquals(OrderStatus.ACCEPTED, response.status());
    }
}
