package com.fixclient.backend.execution;

import org.springframework.stereotype.Service;
import quickfix.Message;

@Service
public class ExecutionReportIngestionService {

    private final ExecutionReportMapper mapper;
    private final ExecutionReportStateStore stateStore;

    public ExecutionReportIngestionService(ExecutionReportMapper mapper, ExecutionReportStateStore stateStore) {
        this.mapper = mapper;
        this.stateStore = stateStore;
    }

    public void ingest(Message executionReportMessage) {
        ingest(mapper.fromFixMessage(executionReportMessage));
    }

    public void ingest(ExecutionReportEvent event) {
        stateStore.update(event);
    }
}
