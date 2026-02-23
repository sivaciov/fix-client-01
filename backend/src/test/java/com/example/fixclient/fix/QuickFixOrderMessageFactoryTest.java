package com.example.fixclient.fix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fixclient.backend.orders.OrderSide;
import com.fixclient.backend.orders.OrderType;
import com.fixclient.backend.orders.TimeInForce;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.field.MsgType;

class QuickFixOrderMessageFactoryTest {

    @Test
    void buildsLimitOrderWithPrice() throws Exception {
        QuickFixOrderMessageFactory factory = new QuickFixOrderMessageFactory();

        Message message = factory.build(new OrderSubmission(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-02-23T00:00:00Z"),
                "AAPL",
                OrderSide.BUY,
                100,
                OrderType.LIMIT,
                new BigDecimal("123.45"),
                TimeInForce.GTC));

        assertEquals(MsgType.ORDER_SINGLE, message.getHeader().getString(MsgType.FIELD));
        assertEquals("00000000-0000-0000-0000-000000000001", message.getString(11));
        assertEquals("AAPL", message.getString(55));
        assertEquals('1', message.getChar(54));
        assertEquals('2', message.getChar(40));
        assertEquals('1', message.getChar(59));
        assertEquals(123.45d, message.getDouble(44));
    }

    @Test
    void marketOrderOmitsPrice() throws Exception {
        QuickFixOrderMessageFactory factory = new QuickFixOrderMessageFactory();

        Message message = factory.build(new OrderSubmission(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-02-23T00:00:00Z"),
                "MSFT",
                OrderSide.SELL,
                50,
                OrderType.MARKET,
                new BigDecimal("321.12"),
                TimeInForce.IOC));

        assertEquals('2', message.getChar(54));
        assertEquals('1', message.getChar(40));
        assertEquals('3', message.getChar(59));
        assertFalse(message.isSetField(44));
    }
}
