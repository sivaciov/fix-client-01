package com.example.fixclient.fix;

import java.time.Instant;

public record InitiatorDiagnostics(String lastEvent, String lastError, Instant lastUpdatedAt) {
}
