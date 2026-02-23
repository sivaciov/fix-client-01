package com.fixclient.backend.market;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/market")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/status")
    public MarketStatusResponse status() {
        return marketDataService.status();
    }

    @GetMapping("/quote")
    public MarketQuote quote(@RequestParam String symbol) {
        return marketDataService.quote(symbol);
    }

    @GetMapping("/quotes")
    public List<MarketQuote> quotes() {
        return marketDataService.quotes();
    }

    @PostMapping("/simulate")
    public MarketQuote simulate(@RequestBody SimulateQuoteRequest request) {
        return marketDataService.simulate(request);
    }
}
