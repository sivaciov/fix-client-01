package com.fixclient.backend;

import com.example.fixclient.fix.InitiatorServiceStatus;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FixControlController {

    private final FixControlService fixControlService;

    public FixControlController(FixControlService fixControlService) {
        this.fixControlService = fixControlService;
    }

    @GetMapping("/fix/status")
    public FixStatusResponse status() {
        return currentStatus();
    }

    @PostMapping("/fix/start")
    public FixStatusResponse start() {
        try {
            fixControlService.start();
        } catch (IllegalStateException ignored) {
            // Return normalized status payload; service status exposes error state.
        }
        return currentStatus();
    }

    @PostMapping("/fix/stop")
    public FixStatusResponse stop() {
        fixControlService.stop();
        return currentStatus();
    }

    private FixStatusResponse currentStatus() {
        InitiatorServiceStatus status = fixControlService.getStatus();
        return new FixStatusResponse(
                status.status().name(),
                status.details() == null ? "" : status.details(),
                status.sessions(),
                new FixConfigResponse(
                        status.config().senderCompId(),
                        status.config().targetCompId(),
                        status.config().host(),
                        status.config().port()),
                new FixDiagnosticsResponse(
                        status.diagnostics().lastEvent(),
                        status.diagnostics().lastError() == null ? "" : status.diagnostics().lastError(),
                        status.diagnostics().lastUpdatedAt().toString()));
    }

    public record FixStatusResponse(
            String status,
            String details,
            List<String> sessions,
            FixConfigResponse config,
            FixDiagnosticsResponse diagnostics) {
    }

    public record FixConfigResponse(String senderCompId, String targetCompId, String host, Integer port) {
    }

    public record FixDiagnosticsResponse(String lastEvent, String lastError, String lastUpdatedAt) {
    }
}
