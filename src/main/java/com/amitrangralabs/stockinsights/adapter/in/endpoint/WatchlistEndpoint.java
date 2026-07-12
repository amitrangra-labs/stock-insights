package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import com.amitrangralabs.stockinsights.domain.service.WatchlistService;
import java.util.concurrent.Executor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds/removes tickers on the watchlist from the dashboard forms.
 *
 * <p>Stereotype-free; wired in {@code InboundConfig}. Uses the POST-redirect-GET pattern (returns a
 * redirect to {@code /dashboard}) so a refresh of the resulting page doesn't re-submit the form.
 * When a ticker is added, its data is fetched on a background {@link Executor} so the (throttled,
 * multi-call) fetch never blocks the redirect — the row appears immediately and fills in shortly.
 */
@RequestMapping
public class WatchlistEndpoint {

    private final WatchlistService watchlistService;
    private final MarketDataRefreshService refreshService;
    private final Executor refreshExecutor;

    public WatchlistEndpoint(
            WatchlistService watchlistService,
            MarketDataRefreshService refreshService,
            Executor refreshExecutor) {
        this.watchlistService = watchlistService;
        this.refreshService = refreshService;
        this.refreshExecutor = refreshExecutor;
    }

    @PostMapping("/watchlist")
    public String add(@RequestParam("ticker") String ticker) {
        watchlistService.add(ticker).ifPresent(symbol ->
                refreshExecutor.execute(() -> refreshService.refreshTicker(symbol)));
        return "redirect:/dashboard";
    }

    @PostMapping("/watchlist/{ticker}/remove")
    public String remove(@PathVariable("ticker") String ticker) {
        watchlistService.remove(ticker);
        return "redirect:/dashboard";
    }
}
