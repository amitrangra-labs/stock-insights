package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
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
    private final PriceHistoryPort priceHistory;
    private final PriceHistoryRepositoryPort priceHistoryRepository;
    private final WatchlistPort watchlist;

    public MarketDataRefreshService(
            MarketDataPort marketData,
            MarketDataRepositoryPort repository,
            PriceHistoryPort priceHistory,
            PriceHistoryRepositoryPort priceHistoryRepository,
            WatchlistPort watchlist) {
        this.marketData = marketData;
        this.repository = repository;
        this.priceHistory = priceHistory;
        this.priceHistoryRepository = priceHistoryRepository;
        this.watchlist = watchlist;
    }

    /**
     * Refresh frequently-changing data (quote + news) for every tracked ticker. Never throws.
     * Runs on the short "live" cadence.
     */
    public void refreshLive() {
        List<String> tickers = watchlist.list();
        if (tickers.isEmpty()) {
            log.info("Watchlist is empty; nothing to refresh (live).");
            return;
        }
        log.info("Refreshing live data (quote, news) for {} ticker(s): {}", tickers.size(), tickers);
        int quotes = 0;
        int news = 0;
        for (String ticker : tickers) {
            if (refreshQuote(ticker)) {
                quotes++;
            }
            if (refreshNews(ticker)) {
                news++;
            }
        }
        int n = tickers.size();
        log.info("Live refresh complete: {}/{} quotes, {}/{} news.", quotes, n, news, n);
    }

    /**
     * Refresh slowly-changing data (profile, fundamentals, analyst ratings, price history) for
     * every tracked ticker. Never throws. Runs on the long "reference" cadence to save API calls.
     */
    public void refreshReference() {
        List<String> tickers = watchlist.list();
        if (tickers.isEmpty()) {
            log.info("Watchlist is empty; nothing to refresh (reference).");
            return;
        }
        log.info("Refreshing reference data (profile, fundamentals, ratings, history) for {} ticker(s): {}",
                tickers.size(), tickers);
        int profiles = 0;
        int fundamentals = 0;
        int ratings = 0;
        int histories = 0;
        for (String ticker : tickers) {
            if (refreshProfile(ticker)) {
                profiles++;
            }
            if (refreshFundamentals(ticker)) {
                fundamentals++;
            }
            if (refreshRatings(ticker)) {
                ratings++;
            }
            if (refreshHistory(ticker)) {
                histories++;
            }
        }
        int n = tickers.size();
        log.info("Reference refresh complete: {}/{} profiles, {}/{} fundamentals, {}/{} ratings, {}/{} histories.",
                profiles, n, fundamentals, n, ratings, n, histories, n);
    }

    /** Refresh everything for a single ticker (used right after it is added). Never throws. */
    public void refreshTicker(String ticker) {
        log.info("Refreshing all data for newly added ticker {}", ticker);
        refreshQuote(ticker);
        refreshNews(ticker);
        refreshProfile(ticker);
        refreshFundamentals(ticker);
        refreshRatings(ticker);
        refreshHistory(ticker);
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

    private boolean refreshHistory(String ticker) {
        try {
            List<PricePoint> history = priceHistory.fetchDailyHistory(ticker);
            priceHistoryRepository.saveHistory(ticker, history);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not refresh history for {}: {}", ticker, e.getMessage());
            return false;
        }
    }

    private boolean refreshNews(String ticker) {
        try {
            List<NewsItem> news = marketData.fetchNews(ticker);
            repository.saveNews(ticker, news);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not refresh news for {}: {}", ticker, e.getMessage());
            return false;
        }
    }

    private boolean refreshRatings(String ticker) {
        try {
            AnalystRating rating = marketData.fetchRatings(ticker);
            repository.saveRating(ticker, rating);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not refresh ratings for {}: {}", ticker, e.getMessage());
            return false;
        }
    }

    private boolean refreshFundamentals(String ticker) {
        try {
            Fundamentals fundamentals = marketData.fetchFundamentals(ticker);
            repository.saveFundamentals(fundamentals);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not refresh fundamentals for {}: {}", ticker, e.getMessage());
            return false;
        }
    }
}
