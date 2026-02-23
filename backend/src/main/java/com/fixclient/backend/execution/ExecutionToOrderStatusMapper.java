package com.fixclient.backend.execution;

import com.fixclient.backend.orders.OrderStatus;
import java.util.Locale;

public final class ExecutionToOrderStatusMapper {

    private ExecutionToOrderStatusMapper() {
    }

    public static OrderStatus map(String execType, String ordStatus) {
        OrderStatus fromOrdStatus = fromOrdStatus(ordStatus);
        if (fromOrdStatus != null) {
            return fromOrdStatus;
        }
        return fromExecType(execType);
    }

    private static OrderStatus fromOrdStatus(String ordStatus) {
        String normalized = normalize(ordStatus);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "0", "NEW" -> OrderStatus.NEW;
            case "1", "PARTIALLY_FILLED" -> OrderStatus.PARTIALLY_FILLED;
            case "2", "FILLED" -> OrderStatus.FILLED;
            case "4", "CANCELED", "CANCELLED" -> OrderStatus.CANCELED;
            case "8", "REJECTED" -> OrderStatus.REJECTED;
            default -> null;
        };
    }

    private static OrderStatus fromExecType(String execType) {
        String normalized = normalize(execType);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "0", "NEW" -> OrderStatus.NEW;
            case "1", "PARTIAL_FILL", "PARTIALLY_FILLED" -> OrderStatus.PARTIALLY_FILLED;
            case "2", "FILL", "FILLED" -> OrderStatus.FILLED;
            case "4", "CANCELED", "CANCELLED" -> OrderStatus.CANCELED;
            case "8", "REJECTED" -> OrderStatus.REJECTED;
            default -> null;
        };
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
