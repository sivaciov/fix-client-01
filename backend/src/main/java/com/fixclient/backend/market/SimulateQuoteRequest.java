package com.fixclient.backend.market;

import java.math.BigDecimal;

public record SimulateQuoteRequest(String symbol, BigDecimal bid, BigDecimal ask, BigDecimal last) {}
