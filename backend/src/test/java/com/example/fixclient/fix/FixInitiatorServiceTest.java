package com.example.fixclient.fix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    void startSetsRunningWhenInitiatorStarts() throws Exception {
        QuickFixInitiator initiator = mock(QuickFixInitiator.class);
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(org.mockito.ArgumentMatchers.any(SessionSettings.class))).thenReturn(initiator);

        FixInitiatorService service = new FixInitiatorService(factory);

        service.start();

        assertEquals(InitiatorStatus.RUNNING, service.getStatus());
        verify(initiator).start();
    }

    @Test
    void startSetsErrorWhenInitiatorFails() throws Exception {
        QuickFixInitiator initiator = mock(QuickFixInitiator.class);
        doThrow(new ConfigError("boom")).when(initiator).start();

        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(org.mockito.ArgumentMatchers.any(SessionSettings.class))).thenReturn(initiator);

        FixInitiatorService service = new FixInitiatorService(factory);

        assertThrows(IllegalStateException.class, service::start);
        assertEquals(InitiatorStatus.ERROR, service.getStatus());
    }

    @Test
    void stopTransitionsToStopped() throws Exception {
        QuickFixInitiator initiator = mock(QuickFixInitiator.class);
        QuickFixInitiatorFactory factory = mock(QuickFixInitiatorFactory.class);
        when(factory.create(org.mockito.ArgumentMatchers.any(SessionSettings.class))).thenReturn(initiator);

        FixInitiatorService service = new FixInitiatorService(factory);
        service.start();

        service.stop();

        assertEquals(InitiatorStatus.STOPPED, service.getStatus());
        verify(initiator).stop();
    }
}
