package com.fixclient.backend.market;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(String symbol, BigDecimal bid, BigDecimal ask, BigDecimal last, Instant updatedAt, String source) {}
