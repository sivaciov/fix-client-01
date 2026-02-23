package com.fixclient.backend.execution;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderExecutionState(
        String execType,
        String ordStatus,
        BigDecimal filledQty,
        BigDecimal leavesQty,
        BigDecimal avgPx,
        BigDecimal lastPx,
        BigDecimal lastQty,
        String text,
        Instant updatedAt) {
}
