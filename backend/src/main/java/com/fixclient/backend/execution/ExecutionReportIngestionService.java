package com.fixclient.backend.execution;

import com.fixclient.backend.orders.OrderService;
import org.springframework.stereotype.Service;
import quickfix.Message;

@Service
public class ExecutionReportIngestionService {

    private final ExecutionReportMapper mapper;
    private final ExecutionReportStateStore stateStore;
    private final OrderService orderService;

    public ExecutionReportIngestionService(
            ExecutionReportMapper mapper,
            ExecutionReportStateStore stateStore,
            OrderService orderService) {
        this.mapper = mapper;
        this.stateStore = stateStore;
        this.orderService = orderService;
    }

    public void ingest(Message executionReportMessage) {
        ingest(mapper.fromFixMessage(executionReportMessage));
    }

    public void ingest(ExecutionReportEvent event) {
        stateStore.update(event);
        orderService.applyExecutionReport(event);
    }
}
