package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.object.StockDetail;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import java.util.List;
import java.util.Optional;

/**
 * Journey C: assembles the stock detail view model from cached data only.
 *
 * <p>Plain Java, framework-free. Reads profile/quote and price history through the two repository
 * ports — never the external providers — so detail-page loads are fast and rate-limit-safe.
 *
 * <p>Only tickers on the configured watchlist are served; anything else yields
 * {@link Optional#empty()} so the endpoint can return 404 rather than fetching arbitrary symbols.
 */
public class StockDetailService {

    private final MarketDataRepositoryPort marketDataRepository;
    private final PriceHistoryRepositoryPort priceHistoryRepository;
    private final List<String> trackedTickers;

    public StockDetailService(
            MarketDataRepositoryPort marketDataRepository,
            PriceHistoryRepositoryPort priceHistoryRepository,
            List<String> trackedTickers) {
        this.marketDataRepository = marketDataRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.trackedTickers = List.copyOf(trackedTickers);
    }

    /** Full detail (profile + quote + history) for a tracked ticker, or empty if not tracked. */
    public Optional<StockDetail> getDetail(String ticker) {
        String canonical = canonical(ticker);
        if (canonical == null) {
            return Optional.empty();
        }
        return Optional.of(new StockDetail(
                canonical,
                marketDataRepository.findProfile(canonical).orElse(null),
                marketDataRepository.findLatestQuote(canonical).orElse(null),
                priceHistoryRepository.findHistory(canonical)));
    }

    /** Cached daily history for a tracked ticker (empty if not tracked or nothing cached). */
    public List<PricePoint> getHistory(String ticker) {
        String canonical = canonical(ticker);
        return canonical == null ? List.of() : priceHistoryRepository.findHistory(canonical);
    }

    /** The uppercase form of a tracked ticker, or {@code null} if it is not on the watchlist. */
    private String canonical(String ticker) {
        if (ticker == null) {
            return null;
        }
        String upper = ticker.toUpperCase();
        return trackedTickers.contains(upper) ? upper : null;
    }
}
