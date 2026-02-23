package com.fixclient.backend.execution;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderDetailsResponse(
        String orderId,
        String lastExecType,
        String lastOrdStatus,
        BigDecimal filledQty,
        BigDecimal leavesQty,
        BigDecimal avgPx,
        BigDecimal lastPx,
        BigDecimal lastQty,
        String lastText,
        Instant updatedAt) {

    public static OrderDetailsResponse from(String orderId, OrderExecutionState state) {
        if (state == null) {
            return new OrderDetailsResponse(orderId, null, null, null, null, null, null, null, null, null);
        }
        return new OrderDetailsResponse(
                orderId,
                state.lastExecType(),
                state.lastOrdStatus(),
                state.filledQty(),
                state.leavesQty(),
                state.avgPx(),
                state.lastPx(),
                state.lastQty(),
                state.lastText(),
                state.updatedAt());
    }
}
