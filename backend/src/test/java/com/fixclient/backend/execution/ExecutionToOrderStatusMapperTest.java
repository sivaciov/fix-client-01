package com.fixclient.backend.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fixclient.backend.orders.OrderStatus;
import org.junit.jupiter.api.Test;

class ExecutionToOrderStatusMapperTest {

    @Test
    void mapsOrdStatusCodesDeterministically() {
        assertEquals(OrderStatus.NEW, ExecutionToOrderStatusMapper.map("0", "0"));
        assertEquals(OrderStatus.PARTIALLY_FILLED, ExecutionToOrderStatusMapper.map("FILL", "1"));
        assertEquals(OrderStatus.FILLED, ExecutionToOrderStatusMapper.map("1", "2"));
        assertEquals(OrderStatus.CANCELED, ExecutionToOrderStatusMapper.map("8", "4"));
        assertEquals(OrderStatus.REJECTED, ExecutionToOrderStatusMapper.map("0", "8"));
    }

    @Test
    void fallsBackToExecTypeWhenOrdStatusMissing() {
        assertEquals(OrderStatus.PARTIALLY_FILLED, ExecutionToOrderStatusMapper.map("PARTIAL_FILL", null));
        assertEquals(OrderStatus.FILLED, ExecutionToOrderStatusMapper.map("2", "   "));
        assertEquals(OrderStatus.CANCELED, ExecutionToOrderStatusMapper.map("CANCELED", null));
    }

    @Test
    void returnsNullWhenUnrecognized() {
        assertNull(ExecutionToOrderStatusMapper.map("x", "y"));
    }
}
