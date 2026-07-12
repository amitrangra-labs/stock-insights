package com.amitrangralabs.stockinsights.domain.config;

import com.amitrangralabs.stockinsights.domain.service.DashboardService;
import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import com.amitrangralabs.stockinsights.domain.service.StockDetailService;
import com.amitrangralabs.stockinsights.domain.service.SymbolSearchService;
import com.amitrangralabs.stockinsights.domain.service.WatchlistService;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import com.amitrangralabs.stockinsights.port.SymbolSearchPort;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Explicit wiring for the domain layer. Each service is a plain Java object constructed here with
 * its {@code port} dependencies; reading this file reveals the whole domain graph.
 *
 * <p>The set of tracked tickers now lives in the {@link WatchlistPort} (so it is editable at
 * runtime). {@link #watchlistSeeder} seeds it from {@code stock-insights.tracked-tickers} on first
 * start; the services read it dynamically.
 */
@Configuration
public class DomainConfig {

    private static final Logger log = LoggerFactory.getLogger(DomainConfig.class);

    @Bean
    public MarketDataRefreshService marketDataRefreshService(
            MarketDataPort marketDataPort,
            MarketDataRepositoryPort marketDataRepositoryPort,
            PriceHistoryPort priceHistoryPort,
            PriceHistoryRepositoryPort priceHistoryRepositoryPort,
            WatchlistPort watchlistPort) {
        return new MarketDataRefreshService(
                marketDataPort,
                marketDataRepositoryPort,
                priceHistoryPort,
                priceHistoryRepositoryPort,
                watchlistPort);
    }

    @Bean
    public DashboardService dashboardService(
            MarketDataRepositoryPort marketDataRepositoryPort, WatchlistPort watchlistPort) {
        return new DashboardService(marketDataRepositoryPort, watchlistPort);
    }

    @Bean
    public StockDetailService stockDetailService(
            MarketDataRepositoryPort marketDataRepositoryPort,
            PriceHistoryRepositoryPort priceHistoryRepositoryPort,
            WatchlistPort watchlistPort) {
        return new StockDetailService(
                marketDataRepositoryPort, priceHistoryRepositoryPort, watchlistPort);
    }

    @Bean
    public WatchlistService watchlistService(WatchlistPort watchlistPort, Environment env) {
        return new WatchlistService(watchlistPort, trackedTickers(env));
    }

    @Bean
    public SymbolSearchService symbolSearchService(SymbolSearchPort symbolSearchPort) {
        return new SymbolSearchService(symbolSearchPort);
    }

    /**
     * Seeds the watchlist from {@code stock-insights.tracked-tickers} the first time the app starts
     * (i.e. when the watchlist table is empty). After that the watchlist is user-managed.
     */
    @Bean
    public ApplicationRunner watchlistSeeder(WatchlistService watchlistService) {
        return args -> {
            boolean wasEmpty = watchlistService.tickers().isEmpty();
            watchlistService.seedIfEmpty();
            if (wasEmpty) {
                log.info("Seeded watchlist from configuration: {}", watchlistService.tickers());
            }
        };
    }

    /** Binds {@code stock-insights.tracked-tickers} (a YAML list) to a {@code List<String>}. */
    private static List<String> trackedTickers(Environment env) {
        return Binder.get(env)
                .bind("stock-insights.tracked-tickers", Bindable.listOf(String.class))
                .orElse(List.of());
    }
}
