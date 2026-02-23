package com.fixclient.backend.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketDataService {

    static final String SIMULATED_SOURCE = "SIMULATED";

    private final MarketDataStore store;
    private final Clock clock;

    @Autowired
    public MarketDataService(MarketDataStore store) {
        this(store, Clock.systemUTC());
    }

    MarketDataService(MarketDataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public MarketStatusResponse status() {
        return new MarketStatusResponse(store.symbolsTracked(), store.latestUpdateAt());
    }

    public MarketQuote quote(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        return store.find(normalizedSymbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown symbol: " + normalizedSymbol));
    }

    public List<MarketQuote> quotes() {
        return store.all();
    }

    public MarketQuote simulate(SimulateQuoteRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String symbol = normalizeSymbol(request.symbol());
        validateQuoteValues(request.bid(), request.ask(), request.last());

        Instant now = Instant.now(clock);
        return store.upsert(symbol, request.bid(), request.ask(), request.last(), now, SIMULATED_SOURCE);
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
        }

        String normalized = symbol.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9._-]{1,24}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol has invalid format");
        }
        return normalized;
    }

    private void validateQuoteValues(BigDecimal bid, BigDecimal ask, BigDecimal last) {
        if (bid == null && ask == null && last == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one of bid, ask, or last must be provided");
        }

        validateNonNegative("bid", bid);
        validateNonNegative("ask", ask);
        validateNonNegative("last", last);

        if (bid != null && ask != null && ask.compareTo(bid) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ask must be greater than or equal to bid");
        }
    }

    private void validateNonNegative(String field, BigDecimal value) {
        if (value != null && value.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be non-negative");
        }
    }
}
