package com.fixclient.backend.market;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointsReturnExpectedPayloadShapes() throws Exception {
        String symbol = "A2TST1";
        mockMvc.perform(post("/market/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"" + symbol + "\",\"bid\":190.1,\"ask\":190.2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(symbol))
                .andExpect(jsonPath("$.source").value("SIMULATED"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/market/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbolsTracked").isNumber())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/market/quote").param("symbol", symbol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(symbol));

        mockMvc.perform(get("/market/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].symbol").value(hasItem(symbol)));
    }
}
