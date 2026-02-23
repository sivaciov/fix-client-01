package com.fixclient.backend;

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
        return new FixStatusResponse(fixControlService.getStatus().name(), "", List.of());
    }

    public record FixStatusResponse(String status, String details, List<String> sessions) {
    }
}
