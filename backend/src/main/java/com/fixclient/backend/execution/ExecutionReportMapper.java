package com.fixclient.backend.execution;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.OrderID;
import quickfix.field.OrdStatus;
import quickfix.field.Text;

@Component
public class ExecutionReportMapper {

    private final Clock clock;

    public ExecutionReportMapper() {
        this(Clock.systemUTC());
    }

    ExecutionReportMapper(Clock clock) {
        this.clock = clock;
    }

    public ExecutionReportEvent fromFixMessage(Message message) {
        return new ExecutionReportEvent(
                readString(message, ClOrdID.FIELD),
                readString(message, OrderID.FIELD),
                readCharAsString(message, ExecType.FIELD),
                readCharAsString(message, OrdStatus.FIELD),
                readDecimal(message, CumQty.FIELD),
                readDecimal(message, LeavesQty.FIELD),
                readDecimal(message, AvgPx.FIELD),
                readDecimal(message, LastPx.FIELD),
                readDecimal(message, LastQty.FIELD),
                readString(message, Text.FIELD),
                Instant.now(clock));
    }

    private String readString(Message message, int field) {
        try {
            return message.isSetField(field) ? message.getString(field) : null;
        } catch (FieldNotFound ignored) {
            return null;
        }
    }

    private String readCharAsString(Message message, int field) {
        try {
            return message.isSetField(field) ? String.valueOf(message.getChar(field)) : null;
        } catch (FieldNotFound ignored) {
            return null;
        }
    }

    private BigDecimal readDecimal(Message message, int field) {
        try {
            return message.isSetField(field) ? BigDecimal.valueOf(message.getDouble(field)) : null;
        } catch (FieldNotFound ignored) {
            return null;
        }
    }
}
