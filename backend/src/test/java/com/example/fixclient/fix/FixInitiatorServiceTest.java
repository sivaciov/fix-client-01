package com.example.fixclient.fix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;

class FixInitiatorServiceTest {

    @Test
    void loadSessionSettingsFromClasspath() throws Exception {
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        FixInitiatorService service = new FixInitiatorService(factory);

        SessionSettings settings = service.loadSessionSettings();

        assertNotNull(settings);
        SessionID sessionID = settings.sectionIterator().next();
        assertEquals("FIX.4.4", settings.getString(sessionID, "BeginString"));
        assertEquals("initiator", settings.getString(sessionID, "ConnectionType"));
    }

    @Test
    void startSetsRunningWhenInitiatorStartsAndReportsStatusDetails() throws Exception {
        QuickFixInitiator initiator = mock(QuickFixInitiator.class);
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(any(SessionSettings.class))).thenReturn(initiator);

        FixInitiatorService service = new FixInitiatorService(factory);

        service.start();

        InitiatorServiceStatus status = service.getStatus();
        assertEquals(InitiatorStatus.RUNNING, status.status());
        assertNull(status.details());
        assertEquals(1, status.sessions().size());
        assertEquals("FIX.4.4:YOUR_SENDER_COMP_ID->YOUR_TARGET_COMP_ID", status.sessions().get(0));
        assertEquals("YOUR_SENDER_COMP_ID", status.config().senderCompId());
        assertEquals("YOUR_TARGET_COMP_ID", status.config().targetCompId());
        assertEquals("localhost", status.config().host());
        assertEquals(9876, status.config().port());
        assertEquals("Initiator started", status.diagnostics().lastEvent());
        assertNull(status.diagnostics().lastError());
        assertNotNull(status.diagnostics().lastUpdatedAt());
        verify(initiator).start();
    }

    @Test
    void startSetsErrorWhenInitiatorFails() throws Exception {
        QuickFixInitiator initiator = mock(QuickFixInitiator.class);
        doThrow(new ConfigError("boom")).when(initiator).start();

        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(any(SessionSettings.class))).thenReturn(initiator);

        FixInitiatorService service = new FixInitiatorService(factory);

        assertThrows(IllegalStateException.class, service::start);

        InitiatorServiceStatus status = service.getStatus();
        assertEquals(InitiatorStatus.ERROR, status.status());
        assertEquals("boom", status.details());
        assertEquals(1, status.sessions().size());
        assertEquals("FIX.4.4:YOUR_SENDER_COMP_ID->YOUR_TARGET_COMP_ID", status.sessions().get(0));
        assertEquals("Start failed", status.diagnostics().lastEvent());
        assertEquals("boom", status.diagnostics().lastError());
        assertNotNull(status.diagnostics().lastUpdatedAt());
        assertEquals("YOUR_SENDER_COMP_ID", status.config().senderCompId());
    }

    @Test
    void startIsIdempotentWhenAlreadyRunning() throws Exception {
        QuickFixInitiator initiator = mock(QuickFixInitiator.class);
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(any(SessionSettings.class))).thenReturn(initiator);

        FixInitiatorService service = new FixInitiatorService(factory);

        service.start();
        service.start();

        assertEquals(InitiatorStatus.RUNNING, service.getStatus().status());
        verify(initiator, times(1)).start();
        verify(factory, times(1)).create(any(SessionSettings.class));
    }

    @Test
    void stopTransitionsToStoppedAndIsIdempotent() throws Exception {
        QuickFixInitiator initiator = mock(QuickFixInitiator.class);
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(any(SessionSettings.class))).thenReturn(initiator);

        FixInitiatorService service = new FixInitiatorService(factory);
        service.start();

        service.stop();
        service.stop();

        InitiatorServiceStatus status = service.getStatus();
        assertEquals(InitiatorStatus.STOPPED, status.status());
        assertNull(status.details());
        assertEquals(1, status.sessions().size());
        assertEquals("Initiator stopped", status.diagnostics().lastEvent());
        assertNull(status.diagnostics().lastError());
        verify(initiator, times(1)).stop();
    }

    @Test
    void stopBeforeStartIsNoOpAndKeepsStoppedStatus() {
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        FixInitiatorService service = new FixInitiatorService(factory);

        service.stop();
        service.stop();

        InitiatorServiceStatus status = service.getStatus();
        assertEquals(InitiatorStatus.STOPPED, status.status());
        assertNull(status.details());
        assertEquals(0, status.sessions().size());
        assertEquals("YOUR_SENDER_COMP_ID", status.config().senderCompId());
        assertEquals("YOUR_TARGET_COMP_ID", status.config().targetCompId());
        assertEquals("localhost", status.config().host());
        assertEquals(9876, status.config().port());
        assertEquals("Initiator stopped", status.diagnostics().lastEvent());
        assertNull(status.diagnostics().lastError());
        assertNotNull(status.diagnostics().lastUpdatedAt());
    }

    @Test
    void startCanRecoverAfterErrorAndEventuallyRuns() throws Exception {
        QuickFixInitiator firstInitiator = mock(QuickFixInitiator.class);
        doThrow(new ConfigError("first boot failure")).when(firstInitiator).start();

        QuickFixInitiator secondInitiator = mock(QuickFixInitiator.class);
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(any(SessionSettings.class))).thenReturn(firstInitiator, secondInitiator);

        FixInitiatorService service = new FixInitiatorService(factory);

        assertThrows(IllegalStateException.class, service::start);
        service.start();

        InitiatorServiceStatus status = service.getStatus();
        assertEquals(InitiatorStatus.RUNNING, status.status());
        assertNull(status.details());
        assertEquals(1, status.sessions().size());
        assertEquals("Initiator started", status.diagnostics().lastEvent());
        assertNull(status.diagnostics().lastError());
        assertNotNull(status.diagnostics().lastUpdatedAt());
        verify(firstInitiator, times(1)).start();
        verify(secondInitiator, times(1)).start();
    }

    @Test
    void initialStatusContainsDefaultConfigAndDiagnostics() {
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        FixInitiatorService service = new FixInitiatorService(factory);

        InitiatorServiceStatus status = service.getStatus();

        assertEquals(InitiatorStatus.STOPPED, status.status());
        assertNull(status.details());
        assertTrue(status.sessions().isEmpty());
        assertEquals("YOUR_SENDER_COMP_ID", status.config().senderCompId());
        assertEquals("YOUR_TARGET_COMP_ID", status.config().targetCompId());
        assertEquals("localhost", status.config().host());
        assertEquals(9876, status.config().port());
        assertEquals("Not started", status.diagnostics().lastEvent());
        assertNull(status.diagnostics().lastError());
        assertNotNull(status.diagnostics().lastUpdatedAt());
    }
}
