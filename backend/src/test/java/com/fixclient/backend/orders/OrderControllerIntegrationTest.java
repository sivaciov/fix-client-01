package com.fixclient.backend.orders;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fixclient.fix.OrderSendResult;
import com.example.fixclient.fix.OrderSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderSender orderSender;

    @BeforeEach
    void resetSenderDefault() {
        when(orderSender.send(any())).thenReturn(new OrderSendResult(true, "Order accepted"));
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
    void marketOrderIgnoresPriceAndAcceptsWhenSenderAccepts() throws Exception {
        when(orderSender.send(any())).thenReturn(new OrderSendResult(true, "Order sent"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "AAPL",
                                  "side": "BUY",
                                  "qty": 100,
                                  "type": "MARKET",
                                  "price": 123.45,
                                  "tif": "DAY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message").value("Order sent"));

        ArgumentCaptor<com.example.fixclient.fix.OrderSubmission> captor = ArgumentCaptor.forClass(com.example.fixclient.fix.OrderSubmission.class);
        verify(orderSender).send(captor.capture());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().price());
    }

    @Test
    void rejectsWhenFixStatusNotRunning() throws Exception {
        when(orderSender.send(any())).thenReturn(new OrderSendResult(false, "Order rejected: FIX initiator is not RUNNING"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "AAPL",
                                  "side": "SELL",
                                  "qty": 10,
                                  "type": "MARKET",
                                  "tif": "IOC"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Order rejected: FIX initiator is not RUNNING"));
    }

    @Test
    void getOrdersReturnsMostRecentFirst() throws Exception {
        when(orderSender.send(any())).thenReturn(new OrderSendResult(true, "Order accepted"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "MSFT",
                                  "side": "BUY",
                                  "qty": 50,
                                  "type": "MARKET",
                                  "tif": "DAY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "AAPL",
                                  "side": "SELL",
                                  "qty": 25,
                                  "type": "LIMIT",
                                  "price": 198.12,
                                  "tif": "GTC"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].type").value("LIMIT"))
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$[1].symbol").value("MSFT"));
    }
}
