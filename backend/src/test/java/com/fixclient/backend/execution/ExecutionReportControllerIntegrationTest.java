package com.fixclient.backend.execution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void simulateRequiresIdentifier() throws Exception {
        mockMvc.perform(post("/exec-reports/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "execType": "0",
                                  "ordStatus": "0"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Either orderId or clOrdId is required"));
    }

    @Test
    void simulateAcceptsClOrdIdAndRequiredFields() throws Exception {
        mockMvc.perform(post("/exec-reports/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clOrdId": "cl-101",
                                  "execType": "0",
                                  "ordStatus": "0",
                                  "text": "ack"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clOrdId").value("cl-101"))
                .andExpect(jsonPath("$.execType").value("0"))
                .andExpect(jsonPath("$.ordStatus").value("0"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }
}
