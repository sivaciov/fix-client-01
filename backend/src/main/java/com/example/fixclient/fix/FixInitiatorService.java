package com.example.fixclient.fix;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import quickfix.ConfigError;
import quickfix.RuntimeError;
import quickfix.SessionSettings;

@Service
public class FixInitiatorService {

    static final String INITIATOR_CONFIG_PATH = "fix/initiator.cfg";

    private final QuickFixInitiatorFactory initiatorFactory;
    private final AtomicReference<InitiatorStatus> status = new AtomicReference<>(InitiatorStatus.STOPPED);

    private QuickFixInitiator initiator;

    public FixInitiatorService(QuickFixInitiatorFactory initiatorFactory) {
        this.initiatorFactory = initiatorFactory;
    }

    public synchronized void start() {
        if (status.get() == InitiatorStatus.RUNNING || status.get() == InitiatorStatus.STARTING) {
            return;
        }

        status.set(InitiatorStatus.STARTING);
        try {
            SessionSettings settings = loadSessionSettings();
            initiator = initiatorFactory.create(settings);
            initiator.start();
            status.set(InitiatorStatus.RUNNING);
        } catch (IOException | ConfigError | RuntimeError ex) {
            status.set(InitiatorStatus.ERROR);
            throw new IllegalStateException("Failed to start FIX initiator", ex);
        }
    }

    public synchronized void stop() {
        if (initiator != null) {
            initiator.stop();
            initiator = null;
        }
        status.set(InitiatorStatus.STOPPED);
    }

    public InitiatorStatus getStatus() {
        return status.get();
    }

    SessionSettings loadSessionSettings() throws IOException, ConfigError {
        ClassPathResource config = new ClassPathResource(INITIATOR_CONFIG_PATH);
        try (InputStream inputStream = config.getInputStream()) {
            return new SessionSettings(inputStream);
        }
    }
}
