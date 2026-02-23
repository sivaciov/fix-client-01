package com.fixclient.backend.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketDataStoreTest {

    @Test
    void upsertMergesMissingFieldsFromExistingQuote() {
        MarketDataStore store = new MarketDataStore();
        Instant t1 = Instant.parse("2026-02-23T12:00:00Z");
        Instant t2 = Instant.parse("2026-02-23T12:00:05Z");

        store.upsert("AAPL", BigDecimal.valueOf(150.10), BigDecimal.valueOf(150.20), BigDecimal.valueOf(150.15), t1, "SIMULATED");
        MarketQuote updated = store.upsert("AAPL", null, BigDecimal.valueOf(150.25), null, t2, "SIMULATED");

        assertEquals("AAPL", updated.symbol());
        assertEquals("150.1", updated.bid().toPlainString());
        assertEquals("150.25", updated.ask().toPlainString());
        assertEquals("150.15", updated.last().toPlainString());
        assertEquals(t2, updated.updatedAt());
    }

    @Test
    void statusDataReflectsTrackedSymbolsAndLatestTimestamp() {
        MarketDataStore store = new MarketDataStore();
        Instant earlier = Instant.parse("2026-02-23T10:00:00Z");
        Instant later = Instant.parse("2026-02-23T11:00:00Z");

        assertEquals(0, store.symbolsTracked());
        assertNull(store.latestUpdateAt());

        store.upsert("MSFT", BigDecimal.ONE, BigDecimal.valueOf(2), null, earlier, "SIMULATED");
        store.upsert("AAPL", BigDecimal.TEN, BigDecimal.valueOf(11), null, later, "SIMULATED");

        assertEquals(2, store.symbolsTracked());
        assertEquals(later, store.latestUpdateAt());
        assertEquals("AAPL", store.all().get(0).symbol());
        assertEquals("MSFT", store.all().get(1).symbol());
    }
}
