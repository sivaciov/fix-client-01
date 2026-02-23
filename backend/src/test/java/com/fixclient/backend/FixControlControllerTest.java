package com.fixclient.backend;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fixclient.fix.InitiatorStatus;
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
        when(fixControlService.getStatus()).thenReturn(InitiatorStatus.STOPPED);

        mockMvc.perform(get("/fix/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.details").value(""))
                .andExpect(jsonPath("$.sessions").isArray())
                .andExpect(jsonPath("$.sessions").isEmpty());
    }

    @Test
    void startCallsServiceAndReturnsStatusPayload() throws Exception {
        when(fixControlService.getStatus()).thenReturn(InitiatorStatus.RUNNING);

        mockMvc.perform(post("/fix/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.details").value(""))
                .andExpect(jsonPath("$.sessions").isArray());

        verify(fixControlService, times(1)).start();
    }

    @Test
    void startFailureStillReturnsStatusPayload() throws Exception {
        doThrow(new IllegalStateException("failed to start")).when(fixControlService).start();
        when(fixControlService.getStatus()).thenReturn(InitiatorStatus.ERROR);

        mockMvc.perform(post("/fix/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.details").value(""))
                .andExpect(jsonPath("$.sessions").isArray());

        verify(fixControlService, times(1)).start();
    }

    @Test
    void stopCallsServiceAndReturnsStatusPayload() throws Exception {
        when(fixControlService.getStatus()).thenReturn(InitiatorStatus.STOPPED);

        mockMvc.perform(post("/fix/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.details").value(""))
                .andExpect(jsonPath("$.sessions").isArray());

        verify(fixControlService, times(1)).stop();
    }
}
