package com.fixclient.backend.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.MsgType;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.Text;

class ExecutionReportMapperTest {

    @Test
    void mapsExpectedFixExecutionReportFields() {
        Instant now = Instant.parse("2026-02-23T12:30:00Z");
        ExecutionReportMapper mapper = new ExecutionReportMapper(Clock.fixed(now, ZoneOffset.UTC));

        Message message = new Message();
        message.getHeader().setString(MsgType.FIELD, MsgType.EXECUTION_REPORT);
        message.setString(ClOrdID.FIELD, "cl-100");
        message.setString(OrderID.FIELD, "ord-501");
        message.setChar(ExecType.FIELD, ExecType.FILL);
        message.setChar(OrdStatus.FIELD, OrdStatus.FILLED);
        message.setDouble(CumQty.FIELD, 100d);
        message.setDouble(LeavesQty.FIELD, 0d);
        message.setDouble(AvgPx.FIELD, 101.55d);
        message.setDouble(LastPx.FIELD, 101.70d);
        message.setDouble(LastQty.FIELD, 10d);
        message.setString(Text.FIELD, "done");

        ExecutionReportEvent event = mapper.fromFixMessage(message);

        assertEquals("cl-100", event.clOrdId());
        assertEquals("ord-501", event.orderId());
        assertEquals(String.valueOf(ExecType.FILL), event.lastExecType());
        assertEquals(String.valueOf(OrdStatus.FILLED), event.lastOrdStatus());
        assertEquals("100.0", event.filledQty().toPlainString());
        assertEquals("0.0", event.leavesQty().toPlainString());
        assertEquals("101.55", event.avgPx().toPlainString());
        assertEquals("101.7", event.lastPx().toPlainString());
        assertEquals("10.0", event.lastQty().toPlainString());
        assertEquals("done", event.lastText());
        assertEquals(now, event.updatedAt());
    }

    @Test
    void leavesMissingFieldsAsNull() {
        ExecutionReportMapper mapper = new ExecutionReportMapper();

        Message message = new Message();
        message.getHeader().setString(MsgType.FIELD, MsgType.EXECUTION_REPORT);

        ExecutionReportEvent event = mapper.fromFixMessage(message);

        assertNotNull(event.updatedAt());
        assertNull(event.clOrdId());
        assertNull(event.orderId());
        assertNull(event.filledQty());
        assertNull(event.lastExecType());
    }
}
