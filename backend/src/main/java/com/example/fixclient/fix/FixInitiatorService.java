package com.example.fixclient.fix;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import quickfix.ConfigError;
import quickfix.FieldConvertError;
import quickfix.RuntimeError;
import quickfix.SessionID;
import quickfix.SessionSettings;

@Service
public class FixInitiatorService {

    static final String INITIATOR_CONFIG_PATH = "fix/initiator.cfg";
    private static final String SETTING_SENDER_COMP_ID = "SenderCompID";
    private static final String SETTING_TARGET_COMP_ID = "TargetCompID";
    private static final String SETTING_SOCKET_CONNECT_HOST = "SocketConnectHost";
    private static final String SETTING_SOCKET_CONNECT_PORT = "SocketConnectPort";

    private final QuickFixInitiatorFactory initiatorFactory;
    private final AtomicReference<InitiatorServiceStatus> status =
            new AtomicReference<>(InitiatorServiceStatus.initial());

    private QuickFixInitiator initiator;

    public FixInitiatorService(QuickFixInitiatorFactory initiatorFactory) {
        this.initiatorFactory = initiatorFactory;
    }

    public synchronized void start() {
        InitiatorServiceStatus currentStatus = status.get();
        if (currentStatus.status() == InitiatorStatus.RUNNING || currentStatus.status() == InitiatorStatus.STARTING) {
            return;
        }

        status.set(withStatus(currentStatus, InitiatorStatus.STARTING, null, "Start requested", null));
        List<String> sessions = currentStatus.sessions();
        FixSessionConfig config = currentStatus.config();
        try {
            SessionSettings settings = loadSessionSettings();
            sessions = extractSessions(settings);
            config = extractConfig(settings);
            initiator = initiatorFactory.create(settings);
            initiator.start();
            status.set(new InitiatorServiceStatus(
                    InitiatorStatus.RUNNING,
                    null,
                    sessions,
                    config,
                    new InitiatorDiagnostics("Initiator started", null, Instant.now())));
        } catch (IOException | ConfigError | RuntimeError ex) {
            status.set(new InitiatorServiceStatus(
                    InitiatorStatus.ERROR,
                    ex.getMessage(),
                    sessions,
                    config,
                    new InitiatorDiagnostics("Start failed", ex.getMessage(), Instant.now())));
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
        status.set(withStatus(currentStatus, InitiatorStatus.STOPPED, null, "Initiator stopped", null));
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

    private FixSessionConfig extractConfig(SessionSettings settings) {
        SessionID firstSession = firstSession(settings);
        String senderCompId = readSetting(settings, firstSession, SETTING_SENDER_COMP_ID);
        String targetCompId = readSetting(settings, firstSession, SETTING_TARGET_COMP_ID);
        String host = readSetting(settings, firstSession, SETTING_SOCKET_CONNECT_HOST);
        Integer port = readIntSetting(settings, firstSession, SETTING_SOCKET_CONNECT_PORT);

        return new FixSessionConfig(senderCompId, targetCompId, host, port);
    }

    private SessionID firstSession(SessionSettings settings) {
        var iterator = settings.sectionIterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private String readSetting(SessionSettings settings, SessionID sessionID, String key) {
        try {
            if (sessionID != null && settings.isSetting(sessionID, key)) {
                return settings.getString(sessionID, key);
            }
            if (settings.isSetting(key)) {
                return settings.getString(key);
            }
        } catch (ConfigError ignored) {
            // fall through to empty fallback
        }
        return "";
    }

    private Integer readIntSetting(SessionSettings settings, SessionID sessionID, String key) {
        try {
            if (sessionID != null && settings.isSetting(sessionID, key)) {
                return settings.getInt(sessionID, key);
            }
            if (settings.isSetting(key)) {
                return settings.getInt(key);
            }
        } catch (ConfigError | FieldConvertError ignored) {
            // fall through to null fallback
        }
        return null;
    }

    private InitiatorServiceStatus withStatus(
            InitiatorServiceStatus currentStatus,
            InitiatorStatus nextStatus,
            String details,
            String lastEvent,
            String lastError) {
        return new InitiatorServiceStatus(
                nextStatus,
                details,
                currentStatus.sessions(),
                currentStatus.config(),
                new InitiatorDiagnostics(lastEvent, lastError, Instant.now()));
    }
}
