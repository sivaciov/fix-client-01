package com.fixclient.backend.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExecutionReportStateStoreTest {

    @Test
    void updatesLatestStateForBothClOrdIdAndOrderIdKeys() {
        ExecutionReportStateStore store = new ExecutionReportStateStore();
        Instant now = Instant.parse("2026-02-23T15:00:00Z");

        store.update(new ExecutionReportEvent(
                "cl-1",
                "ord-1",
                "0",
                "0",
                BigDecimal.ZERO,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                null,
                null,
                "ack",
                now));

        store.update(new ExecutionReportEvent(
                "cl-1",
                "ord-1",
                "1",
                "1",
                BigDecimal.valueOf(5),
                null,
                null,
                BigDecimal.valueOf(100.5),
                BigDecimal.valueOf(5),
                "partial",
                now.plusSeconds(5)));

        OrderExecutionState byClOrdId = store.latestFor("cl-1");
        OrderExecutionState byOrderId = store.latestFor("ord-1");

        assertNotNull(byClOrdId);
        assertNotNull(byOrderId);
        assertEquals("1", byClOrdId.execType());
        assertEquals("1", byOrderId.ordStatus());
        assertEquals("5", byOrderId.filledQty().toPlainString());
        assertEquals("10", byOrderId.leavesQty().toPlainString());
        assertEquals("100.5", byOrderId.lastPx().toPlainString());
        assertEquals("partial", byOrderId.text());
        assertEquals(now.plusSeconds(5), byOrderId.updatedAt());
        assertEquals(2, store.recentReports().size());
        assertEquals("partial", store.recentReports().get(0).text());
    }

    @Test
    void ignoresEventsWithNoOrderKeys() {
        ExecutionReportStateStore store = new ExecutionReportStateStore();

        store.update(new ExecutionReportEvent(
                " ",
                null,
                "8",
                "8",
                null,
                null,
                null,
                null,
                null,
                "ignored",
                Instant.now()));

        assertNull(store.latestFor(""));
        assertEquals(0, store.recentReports().size());
    }
}
