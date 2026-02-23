package com.fixclient.backend.execution;

import java.math.BigDecimal;

public record SimulateExecutionReportRequest(
        String orderId,
        String clOrdId,
        String execType,
        String ordStatus,
        BigDecimal lastQty,
        BigDecimal lastPx,
        BigDecimal cumQty,
        BigDecimal leavesQty,
        BigDecimal avgPx,
        String text) {
}
