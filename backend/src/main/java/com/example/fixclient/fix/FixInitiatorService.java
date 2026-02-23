package com.example.fixclient.fix;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import quickfix.ConfigError;
import quickfix.RuntimeError;
import quickfix.SessionID;
import quickfix.SessionSettings;

@Service
public class FixInitiatorService {

    static final String INITIATOR_CONFIG_PATH = "fix/initiator.cfg";

    private final QuickFixInitiatorFactory initiatorFactory;
    private final AtomicReference<InitiatorServiceStatus> status =
            new AtomicReference<>(new InitiatorServiceStatus(InitiatorStatus.STOPPED, null, List.of()));

    private QuickFixInitiator initiator;

    public FixInitiatorService(QuickFixInitiatorFactory initiatorFactory) {
        this.initiatorFactory = initiatorFactory;
    }

    public synchronized void start() {
        InitiatorServiceStatus currentStatus = status.get();
        if (currentStatus.status() == InitiatorStatus.RUNNING || currentStatus.status() == InitiatorStatus.STARTING) {
            return;
        }

        status.set(new InitiatorServiceStatus(InitiatorStatus.STARTING, null, currentStatus.sessions()));
        try {
            SessionSettings settings = loadSessionSettings();
            List<String> sessions = extractSessions(settings);
            initiator = initiatorFactory.create(settings);
            initiator.start();
            status.set(new InitiatorServiceStatus(InitiatorStatus.RUNNING, null, sessions));
        } catch (IOException | ConfigError | RuntimeError ex) {
            status.set(new InitiatorServiceStatus(InitiatorStatus.ERROR, ex.getMessage(), currentStatus.sessions()));
            initiator = null;
            throw new IllegalStateException("Failed to start FIX initiator", ex);
        }
    }

    public synchronized void stop() {
        InitiatorServiceStatus currentStatus = status.get();
        if (initiator != null) {
            initiator.stop();
            initiator = null;
        }
        status.set(new InitiatorServiceStatus(InitiatorStatus.STOPPED, null, currentStatus.sessions()));
    }

    public InitiatorServiceStatus getStatus() {
        return status.get();
    }

    SessionSettings loadSessionSettings() throws IOException, ConfigError {
        ClassPathResource config = new ClassPathResource(INITIATOR_CONFIG_PATH);
        try (InputStream inputStream = config.getInputStream()) {
            return new SessionSettings(inputStream);
        }
    }

    private List<String> extractSessions(SessionSettings settings) {
        List<String> sessions = new ArrayList<>();
        var iterator = settings.sectionIterator();
        while (iterator.hasNext()) {
            SessionID sessionID = iterator.next();
            sessions.add(sessionID.toString());
        }
        return sessions;
    }
}
