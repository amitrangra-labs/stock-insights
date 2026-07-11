package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import com.amitrangralabs.stockinsights.domain.service.WatchlistService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds/removes tickers on the watchlist from the dashboard forms.
 *
 * <p>Stereotype-free; wired in {@code InboundConfig}. Uses the POST-redirect-GET pattern (returns a
 * redirect to {@code /dashboard}) so a refresh of the resulting page doesn't re-submit the form.
 * When a ticker is added, its data is fetched immediately so it shows up populated right away.
 */
@RequestMapping
public class WatchlistEndpoint {

    private final WatchlistService watchlistService;
    private final MarketDataRefreshService refreshService;

    public WatchlistEndpoint(
            WatchlistService watchlistService, MarketDataRefreshService refreshService) {
        this.watchlistService = watchlistService;
        this.refreshService = refreshService;
    }

    @PostMapping("/watchlist")
    public String add(@RequestParam("ticker") String ticker) {
        watchlistService.add(ticker).ifPresent(refreshService::refreshTicker);
        return "redirect:/dashboard";
    }

    @PostMapping("/watchlist/{ticker}/remove")
    public String remove(@PathVariable("ticker") String ticker) {
        watchlistService.remove(ticker);
        return "redirect:/dashboard";
    }
}
