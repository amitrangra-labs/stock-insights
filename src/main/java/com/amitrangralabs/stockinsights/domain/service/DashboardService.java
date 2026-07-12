package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.domain.object.DashboardRow;
import com.amitrangralabs.stockinsights.domain.object.DashboardView;
import com.amitrangralabs.stockinsights.domain.object.MarketSummary;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.util.ArrayList;
import java.util.List;

/**
 * Journey B: builds the dashboard view model from cached data only.
 *
 * <p>Plain Java, framework-free. Reads through the repository ports — never the external providers —
 * so the page is fast and rate-limit-safe. Produces one {@link DashboardRow} per ticker on the
 * {@link WatchlistPort}, joining the latest quote, fundamentals, profile, and a short price-history
 * spark; tickers with nothing cached still get a placeholder row.
 */
public class DashboardService {

    /** Length of the inline sparkline (trading days). */
    private static final int SPARK_DAYS = 30;

    private final MarketDataRepositoryPort repository;
    private final PriceHistoryRepositoryPort priceHistoryRepository;
    private final WatchlistPort watchlist;

    public DashboardService(
            MarketDataRepositoryPort repository,
            PriceHistoryRepositoryPort priceHistoryRepository,
            WatchlistPort watchlist) {
        this.repository = repository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.watchlist = watchlist;
    }

    public DashboardView getDashboard() {
        List<String> tickers = watchlist.list();
        List<DashboardRow> rows = new ArrayList<>(tickers.size());
        for (String ticker : tickers) {
            rows.add(DashboardRow.from(
                    ticker,
                    repository.findLatestQuote(ticker),
                    repository.findProfile(ticker),
                    repository.findFundamentals(ticker),
                    priceHistoryRepository.findHistory(ticker),
                    SPARK_DAYS));
        }
        return new DashboardView(rows, MarketSummary.from(rows));
    }
}
