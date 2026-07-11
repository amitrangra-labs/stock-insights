package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.service.StockDetailService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * JSON API backing the detail-page chart: cached daily history for a tracked ticker.
 *
 * <p>Stereotype-free; wired in {@code InboundConfig}. Returns an empty array for untracked tickers
 * or when nothing is cached yet, so the client can render an "no history" state without special
 * error handling.
 */
@RequestMapping
public class PriceHistoryApiEndpoint {

    private final StockDetailService stockDetailService;

    public PriceHistoryApiEndpoint(StockDetailService stockDetailService) {
        this.stockDetailService = stockDetailService;
    }

    @GetMapping("/api/stocks/{ticker}/history")
    @ResponseBody
    public List<PricePoint> history(@PathVariable String ticker) {
        return stockDetailService.getHistory(ticker);
    }
}
