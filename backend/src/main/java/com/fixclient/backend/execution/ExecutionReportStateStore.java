package com.fixclient.backend.execution;

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

        OrderExecutionState state = new OrderExecutionState(
                event.lastExecType(),
                event.lastOrdStatus(),
                event.filledQty(),
                event.leavesQty(),
                event.avgPx(),
                event.lastPx(),
                event.lastQty(),
                event.lastText(),
                event.updatedAt());

        for (String key : keys) {
            latestByOrderKey.put(key, state);
        }

        recentReports.addFirst(event);
        while (recentReports.size() > MAX_RECENT) {
            recentReports.pollLast();
        }
    }

    public OrderExecutionState latestFor(String orderId) {
        return latestByOrderKey.get(orderId);
    }

    public List<ExecutionReportEvent> recentReports() {
        return List.copyOf(new ArrayList<>(recentReports));
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
