package com.example.fixclient.fix.market;

import com.fixclient.backend.market.MarketDataSubscriber;
import org.springframework.stereotype.Component;

@Component
public class QuickFixMarketDataSubscriberAdapter implements MarketDataSubscriber {

    @Override
    public void subscribe(String symbol) {
        // Placeholder seam for future MarketDataRequest wiring.
    }

    @Override
    public void unsubscribe(String symbol) {
        // Placeholder seam for future subscription lifecycle wiring.
    }
}
