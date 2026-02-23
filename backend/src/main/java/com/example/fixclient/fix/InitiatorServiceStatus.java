package com.example.fixclient.fix;

import java.util.List;

public record InitiatorServiceStatus(
        InitiatorStatus status,
        String details,
        List<String> sessions,
        FixSessionConfig config,
        InitiatorDiagnostics diagnostics) {

    public InitiatorServiceStatus {
        sessions = List.copyOf(sessions);
    }

    public static InitiatorServiceStatus initial() {
        return new InitiatorServiceStatus(
                InitiatorStatus.STOPPED,
                null,
                List.of(),
                FixSessionConfig.empty(),
                new InitiatorDiagnostics("Not started", null, java.time.Instant.now()));
    }
}
