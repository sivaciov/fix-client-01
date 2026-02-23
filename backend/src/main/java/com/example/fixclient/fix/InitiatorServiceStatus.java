package com.example.fixclient.fix;

import java.util.List;

public record InitiatorServiceStatus(InitiatorStatus status, String details, List<String> sessions) {

    public InitiatorServiceStatus {
        sessions = List.copyOf(sessions);
    }
}
