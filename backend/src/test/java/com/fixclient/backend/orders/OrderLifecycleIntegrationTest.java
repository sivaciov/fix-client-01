package com.fixclient.backend.orders;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OrderLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createThenSimulateExecReportThenGetReflectsUpdatedStatus() throws Exception {
        MvcResult created = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "AAPL",
                                  "side": "BUY",
                                  "qty": 100,
                                  "type": "LIMIT",
                                  "price": 189.55,
                                  "tif": "DAY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.clOrdId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String orderId = objectMapper.readTree(body).get("orderId").asText();
        String clOrdId = objectMapper.readTree(body).get("clOrdId").asText();

        mockMvc.perform(post("/exec-reports/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "clOrdId": "%s",
                                  "execType": "1",
                                  "ordStatus": "1",
                                  "lastQty": 25,
                                  "lastPx": 189.60,
                                  "cumQty": 25,
                                  "leavesQty": 75,
                                  "avgPx": 189.60,
                                  "text": "partial fill"
                                }
                                """.formatted(orderId, clOrdId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.clOrdId").value(clOrdId));

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.clOrdId").value(clOrdId))
                .andExpect(jsonPath("$.status").value("PARTIALLY_FILLED"))
                .andExpect(jsonPath("$.message").value("partial fill"));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].orderId").value(orderId))
                .andExpect(jsonPath("$[0].clOrdId").value(clOrdId))
                .andExpect(jsonPath("$[0].status").value("PARTIALLY_FILLED"));
    }

    @Test
    void validationRejectsLimitWithoutPrice() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "AAPL",
                                  "side": "BUY",
                                  "qty": 100,
                                  "type": "LIMIT",
                                  "tif": "DAY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("price is required for LIMIT orders"));
    }

    @Test
    void marketOrderIgnoresPrice() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "MSFT",
                                  "side": "SELL",
                                  "qty": 20,
                                  "type": "MARKET",
                                  "price": 410.25,
                                  "tif": "IOC"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MARKET"))
                .andExpect(jsonPath("$.price").value(nullValue()));
    }
}
