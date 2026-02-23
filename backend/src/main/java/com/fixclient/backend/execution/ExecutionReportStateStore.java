package com.fixclient.backend.execution;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class ExecutionReportStateStore {

    private static final int MAX_RECENT = 200;

    private final ConcurrentHashMap<String, OrderExecutionState> latestByOrderKey = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<ExecutionReportEvent> recentReports = new ConcurrentLinkedDeque<>();

    public void update(ExecutionReportEvent event) {
        Set<String> keys = keys(event);
        if (keys.isEmpty()) {
            return;
        }

        String primaryKey = keys.iterator().next();
        OrderExecutionState current = latestByOrderKey.get(primaryKey);
        OrderExecutionState merged = merge(current, event);

        for (String key : keys) {
            latestByOrderKey.put(key, merged);
        }

        recentReports.addFirst(event);
        while (recentReports.size() > MAX_RECENT) {
            recentReports.pollLast();
        }
    }

    public OrderExecutionState latestFor(String orderKey) {
        return latestByOrderKey.get(orderKey);
    }

    public List<ExecutionReportEvent> recentReports() {
        return List.copyOf(new ArrayList<>(recentReports));
    }

    private OrderExecutionState merge(OrderExecutionState current, ExecutionReportEvent event) {
        Instant now = event.updatedAt() == null ? Instant.now() : event.updatedAt();
        return new OrderExecutionState(
                firstNonNull(event.execType(), current == null ? null : current.execType()),
                firstNonNull(event.ordStatus(), current == null ? null : current.ordStatus()),
                firstNonNull(event.cumQty(), current == null ? null : current.filledQty()),
                firstNonNull(event.leavesQty(), current == null ? null : current.leavesQty()),
                firstNonNull(event.avgPx(), current == null ? null : current.avgPx()),
                firstNonNull(event.lastPx(), current == null ? null : current.lastPx()),
                firstNonNull(event.lastQty(), current == null ? null : current.lastQty()),
                firstNonNull(event.text(), current == null ? null : current.text()),
                now);
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred == null ? fallback : preferred;
    }

    private Set<String> keys(ExecutionReportEvent event) {
        Set<String> keys = new LinkedHashSet<>();
        if (notBlank(event.clOrdId())) {
            keys.add(event.clOrdId());
        }
        if (notBlank(event.orderId())) {
            keys.add(event.orderId());
        }
        return keys;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
