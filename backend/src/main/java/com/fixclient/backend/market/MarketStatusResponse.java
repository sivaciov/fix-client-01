package com.fixclient.backend.market;

import java.time.Instant;

public record MarketStatusResponse(int symbolsTracked, Instant updatedAt) {}
