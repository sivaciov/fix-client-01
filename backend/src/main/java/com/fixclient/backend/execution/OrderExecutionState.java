package com.fixclient.backend.execution;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderExecutionState(
        String lastExecType,
        String lastOrdStatus,
        BigDecimal filledQty,
        BigDecimal leavesQty,
        BigDecimal avgPx,
        BigDecimal lastPx,
        BigDecimal lastQty,
        String lastText,
        Instant updatedAt) {
}
