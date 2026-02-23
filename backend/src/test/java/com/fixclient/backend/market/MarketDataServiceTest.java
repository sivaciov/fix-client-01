package com.fixclient.backend.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MarketDataServiceTest {

    @Test
    void simulateNormalizesSymbolAndSetsServerTimestamp() {
        MarketDataStore store = new MarketDataStore();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-23T13:15:00Z"), ZoneOffset.UTC);
        MarketDataService service = new MarketDataService(store, fixedClock);

        MarketQuote quote = service.simulate(new SimulateQuoteRequest(" aapl ", BigDecimal.valueOf(190.1), null, null));

        assertEquals("AAPL", quote.symbol());
        assertEquals("SIMULATED", quote.source());
        assertEquals(Instant.parse("2026-02-23T13:15:00Z"), quote.updatedAt());
        assertEquals("190.1", service.quote("AAPL").bid().toPlainString());
    }

    @Test
    void simulateRejectsInvalidPayloads() {
        MarketDataService service = new MarketDataService(new MarketDataStore(), Clock.systemUTC());

        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.simulate(new SimulateQuoteRequest("", BigDecimal.ONE, null, null)));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.simulate(new SimulateQuoteRequest("AAPL", null, null, null)));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.simulate(new SimulateQuoteRequest("AAPL", BigDecimal.valueOf(-1), null, null)));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.simulate(new SimulateQuoteRequest("AAPL", BigDecimal.TEN, BigDecimal.ONE, null)));
    }

    @Test
    void quoteReturnsNotFoundForUnknownSymbol() {
        MarketDataService service = new MarketDataService(new MarketDataStore(), Clock.systemUTC());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.quote("UNKNOWN"));
    }

    private void assertStatus(HttpStatus expected, Runnable call) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call::run);
        assertEquals(expected, ex.getStatusCode());
    }
}
