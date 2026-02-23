package com.fixclient.backend.market;

public interface MarketDataSubscriber {

    void subscribe(String symbol);

    void unsubscribe(String symbol);
}
