package com.fixclient.backend;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fixclient.fix.FixSessionConfig;
import com.example.fixclient.fix.InitiatorDiagnostics;
import com.example.fixclient.fix.InitiatorServiceStatus;
import com.example.fixclient.fix.InitiatorStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FixControlController.class)
class FixControlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FixControlService fixControlService;

    @Test
    void statusReturnsExpectedPayloadShape() throws Exception {
        when(fixControlService.getStatus()).thenReturn(statusResponse(InitiatorStatus.STOPPED, null, List.of()));

        mockMvc.perform(get("/fix/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.details").value(""))
                .andExpect(jsonPath("$.sessions").isArray())
                .andExpect(jsonPath("$.sessions").isEmpty())
                .andExpect(jsonPath("$.config.senderCompId").value("SENDER"))
                .andExpect(jsonPath("$.config.targetCompId").value("TARGET"))
                .andExpect(jsonPath("$.config.host").value("localhost"))
                .andExpect(jsonPath("$.config.port").value(9876))
                .andExpect(jsonPath("$.diagnostics.lastEvent").value("Initiator stopped"))
                .andExpect(jsonPath("$.diagnostics.lastError").value(""))
                .andExpect(jsonPath("$.diagnostics.lastUpdatedAt").isNotEmpty());
    }

    @Test
    void startCallsServiceAndReturnsStatusPayload() throws Exception {
        when(fixControlService.getStatus()).thenReturn(statusResponse(
                InitiatorStatus.RUNNING,
                null,
                List.of("FIX.4.4:YOUR_SENDER_COMP_ID->YOUR_TARGET_COMP_ID")));

        mockMvc.perform(post("/fix/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.details").value(""))
                .andExpect(jsonPath("$.sessions").isArray())
                .andExpect(jsonPath("$.sessions[0]").value("FIX.4.4:YOUR_SENDER_COMP_ID->YOUR_TARGET_COMP_ID"))
                .andExpect(jsonPath("$.config.senderCompId").value("SENDER"))
                .andExpect(jsonPath("$.diagnostics.lastEvent").value("Initiator started"))
                .andExpect(jsonPath("$.diagnostics.lastUpdatedAt").isNotEmpty());

        verify(fixControlService, times(1)).start();
    }

    @Test
    void startFailureStillReturnsStatusPayload() throws Exception {
        doThrow(new IllegalStateException("failed to start")).when(fixControlService).start();
        when(fixControlService.getStatus())
                .thenReturn(statusResponse(InitiatorStatus.ERROR, "failed to start", List.of()));

        mockMvc.perform(post("/fix/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.details").value("failed to start"))
                .andExpect(jsonPath("$.sessions").isArray())
                .andExpect(jsonPath("$.diagnostics.lastEvent").value("Start failed"))
                .andExpect(jsonPath("$.diagnostics.lastError").value("failed to start"))
                .andExpect(jsonPath("$.diagnostics.lastUpdatedAt").isNotEmpty());

        verify(fixControlService, times(1)).start();
    }

    @Test
    void stopCallsServiceAndReturnsStatusPayload() throws Exception {
        when(fixControlService.getStatus()).thenReturn(statusResponse(InitiatorStatus.STOPPED, null, List.of()));

        mockMvc.perform(post("/fix/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.details").value(""))
                .andExpect(jsonPath("$.sessions").isArray())
                .andExpect(jsonPath("$.config.senderCompId").value("SENDER"))
                .andExpect(jsonPath("$.diagnostics.lastEvent").value("Initiator stopped"))
                .andExpect(jsonPath("$.diagnostics.lastUpdatedAt").isNotEmpty());

        verify(fixControlService, times(1)).stop();
    }

    private static InitiatorServiceStatus statusResponse(
            InitiatorStatus status, String details, List<String> sessions) {
        String event;
        if (status == InitiatorStatus.RUNNING) {
            event = "Initiator started";
        } else if (status == InitiatorStatus.ERROR) {
            event = "Start failed";
        } else if (status == InitiatorStatus.STARTING) {
            event = "Start requested";
        } else {
            event = "Initiator stopped";
        }
        return new InitiatorServiceStatus(
                status,
                details,
                sessions,
                new FixSessionConfig("SENDER", "TARGET", "localhost", 9876),
                new InitiatorDiagnostics(event, details, Instant.now()));
    }
}
