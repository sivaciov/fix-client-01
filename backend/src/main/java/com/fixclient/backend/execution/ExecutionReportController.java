package com.fixclient.backend.execution;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionReportController {

    private final ExecutionReportStateStore stateStore;

    public ExecutionReportController(ExecutionReportStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @GetMapping("/orders/{orderId}")
    public OrderDetailsResponse getOrder(@PathVariable String orderId) {
        return OrderDetailsResponse.from(orderId, stateStore.latestFor(orderId));
    }

    @GetMapping("/exec-reports")
    public List<ExecutionReportEvent> listExecutionReports() {
        return stateStore.recentReports();
    }
}
