package com.fixclient.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class MarketDataStore {

    private final ConcurrentHashMap<String, MarketQuote> quotesBySymbol = new ConcurrentHashMap<>();

    public MarketQuote upsert(
            String symbol,
            BigDecimal bid,
            BigDecimal ask,
            BigDecimal last,
            Instant updatedAt,
            String source) {
        return quotesBySymbol.compute(symbol, (ignored, existing) -> {
            BigDecimal nextBid = bid != null ? bid : existing != null ? existing.bid() : null;
            BigDecimal nextAsk = ask != null ? ask : existing != null ? existing.ask() : null;
            BigDecimal nextLast = last != null ? last : existing != null ? existing.last() : null;
            validateNotCrossed(nextBid, nextAsk);
            return new MarketQuote(symbol, nextBid, nextAsk, nextLast, updatedAt, source);
        });
    }

    public Optional<MarketQuote> find(String symbol) {
        return Optional.ofNullable(quotesBySymbol.get(symbol));
    }

    public List<MarketQuote> all() {
        List<MarketQuote> values = new ArrayList<>(quotesBySymbol.values());
        values.sort(Comparator.comparing(MarketQuote::symbol));
        return List.copyOf(values);
    }

    public int symbolsTracked() {
        return quotesBySymbol.size();
    }

    public Instant latestUpdateAt() {
        return quotesBySymbol.values().stream()
                .map(MarketQuote::updatedAt)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private void validateNotCrossed(BigDecimal bid, BigDecimal ask) {
        if (bid != null && ask != null && ask.compareTo(bid) < 0) {
            throw new IllegalArgumentException("ask must be greater than or equal to bid");
        }
    }
}
