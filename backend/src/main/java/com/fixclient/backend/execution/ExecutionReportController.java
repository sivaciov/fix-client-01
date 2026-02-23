package com.fixclient.backend.execution;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionReportController {

    private final ExecutionReportStateStore stateStore;
    private final ExecutionReportIngestionService ingestionService;
    private final Clock clock = Clock.systemUTC();

    public ExecutionReportController(
            ExecutionReportStateStore stateStore,
            ExecutionReportIngestionService ingestionService) {
        this.stateStore = stateStore;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/exec-reports/simulate")
    public ExecutionReportEvent simulateExecutionReport(@RequestBody SimulateExecutionReportRequest request) {
        validate(request);

        ExecutionReportEvent event = new ExecutionReportEvent(
                request.clOrdId(),
                request.orderId(),
                request.execType(),
                request.ordStatus(),
                request.cumQty(),
                request.leavesQty(),
                request.avgPx(),
                request.lastPx(),
                request.lastQty(),
                request.text(),
                Instant.now(clock));

        ingestionService.ingest(event);
        return event;
    }

    @GetMapping("/exec-reports")
    public List<ExecutionReportEvent> listExecutionReports() {
        return stateStore.recentReports();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    private void validate(SimulateExecutionReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        boolean missingOrderId = request.orderId() == null || request.orderId().isBlank();
        boolean missingClOrdId = request.clOrdId() == null || request.clOrdId().isBlank();
        if (missingOrderId && missingClOrdId) {
            throw new IllegalArgumentException("Either orderId or clOrdId is required");
        }
        if (request.execType() == null || request.execType().isBlank()) {
            throw new IllegalArgumentException("execType is required");
        }
        if (request.ordStatus() == null || request.ordStatus().isBlank()) {
            throw new IllegalArgumentException("ordStatus is required");
        }
    }

    public record ErrorResponse(String message) {
    }
}
