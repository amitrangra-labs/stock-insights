package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Journey A: refreshes the local cache for every tracked ticker.
 *
 * <p>Plain Java, framework-free. Constructed in {@code DomainConfig} with the two ports and the
 * tracked-ticker list; driven by {@code RefreshScheduler}.
 *
 * <p>Each ticker's quote and profile are fetched and saved independently: a failure for one ticker
 * (or one of its two fetches) is logged and skipped so it never aborts the rest of the cycle. This
 * is what keeps a flaky or rate-limited provider from taking the whole dashboard down.
 */
public class MarketDataRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRefreshService.class);

    private final MarketDataPort marketData;
    private final MarketDataRepositoryPort repository;
    private final List<String> trackedTickers;

    public MarketDataRefreshService(
            MarketDataPort marketData,
            MarketDataRepositoryPort repository,
            List<String> trackedTickers) {
        this.marketData = marketData;
        this.repository = repository;
        this.trackedTickers = List.copyOf(trackedTickers);
    }

    /** Refresh quote and profile for every tracked ticker. Never throws. */
    public void refreshAll() {
        if (trackedTickers.isEmpty()) {
            log.info("No tracked tickers configured; nothing to refresh.");
            return;
        }
        log.info("Refreshing market data for {} ticker(s): {}", trackedTickers.size(), trackedTickers);
        int quotes = 0;
        int profiles = 0;
        for (String ticker : trackedTickers) {
            if (refreshQuote(ticker)) {
                quotes++;
            }
            if (refreshProfile(ticker)) {
                profiles++;
            }
        }
        log.info("Refresh complete: {}/{} quotes, {}/{} profiles updated.",
                quotes, trackedTickers.size(), profiles, trackedTickers.size());
    }

    private boolean refreshQuote(String ticker) {
        try {
            Quote quote = marketData.fetchQuote(ticker);
            repository.saveQuote(quote);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not refresh quote for {}: {}", ticker, e.getMessage());
            return false;
        }
    }

    private boolean refreshProfile(String ticker) {
        try {
            CompanyProfile profile = marketData.fetchProfile(ticker);
            repository.saveProfile(profile);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not refresh profile for {}: {}", ticker, e.getMessage());
            return false;
        }
    }
}
