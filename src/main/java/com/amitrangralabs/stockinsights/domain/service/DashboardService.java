package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.DashboardRow;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Journey B: builds the dashboard view model from cached data only.
 *
 * <p>Plain Java, framework-free. Reads through {@link MarketDataRepositoryPort} — it never calls the
 * external provider — so the page is fast and rate-limit-safe. Produces one {@link DashboardRow} per
 * tracked ticker, in configured order, filling placeholders for tickers not yet cached.
 */
public class DashboardService {

    private final MarketDataRepositoryPort repository;
    private final List<String> trackedTickers;

    public DashboardService(MarketDataRepositoryPort repository, List<String> trackedTickers) {
        this.repository = repository;
        this.trackedTickers = List.copyOf(trackedTickers);
    }

    public List<DashboardRow> getDashboard() {
        List<DashboardRow> rows = new ArrayList<>(trackedTickers.size());
        for (String ticker : trackedTickers) {
            Optional<Quote> quote = repository.findLatestQuote(ticker);
            Optional<CompanyProfile> profile = repository.findProfile(ticker);
            rows.add(toRow(ticker, quote, profile));
        }
        return rows;
    }

    private static DashboardRow toRow(
            String ticker, Optional<Quote> quote, Optional<CompanyProfile> profile) {
        String name = profile.map(CompanyProfile::name).filter(n -> !n.isBlank()).orElse(ticker);
        String currency = profile.map(CompanyProfile::currency).orElse(null);
        return new DashboardRow(
                ticker,
                name,
                currency,
                quote.map(Quote::current).orElse(null),
                quote.map(Quote::change).orElse(null),
                quote.map(Quote::percentChange).orElse(null),
                quote.map(Quote::asOf).orElse(null));
    }
}
