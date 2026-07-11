package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.object.StockDetail;
import com.amitrangralabs.stockinsights.domain.service.StockDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Journey C: renders the stock detail page for a tracked ticker.
 *
 * <p>Stereotype-free (routed via {@code EndpointHandlerMapping}); constructed in
 * {@code InboundConfig}. Unknown/untracked tickers yield 404 rather than fetching arbitrary symbols.
 */
@RequestMapping
public class StockDetailEndpoint {

    private final StockDetailService stockDetailService;

    public StockDetailEndpoint(StockDetailService stockDetailService) {
        this.stockDetailService = stockDetailService;
    }

    @GetMapping("/stocks/{ticker}")
    public String detail(@PathVariable String ticker, Model model) {
        StockDetail detail = stockDetailService.getDetail(ticker)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Not a tracked ticker: " + ticker));
        model.addAttribute("detail", detail);
        return "detail";
    }
}
