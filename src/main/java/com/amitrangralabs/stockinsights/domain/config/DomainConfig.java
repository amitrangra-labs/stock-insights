package com.amitrangralabs.stockinsights.domain.config;

import com.amitrangralabs.stockinsights.domain.service.DashboardService;
import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import java.util.List;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Explicit wiring for the domain layer. Each service is a plain Java object constructed here with
 * its {@code port} dependencies and configuration; reading this file reveals the whole domain graph.
 *
 * <p>The tracked-ticker list is bound from configuration once ({@link #trackedTickers}) and passed
 * into the services — the services themselves stay framework-free.
 */
@Configuration
public class DomainConfig {

    @Bean
    public MarketDataRefreshService marketDataRefreshService(
            MarketDataPort marketDataPort,
            MarketDataRepositoryPort marketDataRepositoryPort,
            Environment env) {
        return new MarketDataRefreshService(
                marketDataPort, marketDataRepositoryPort, trackedTickers(env));
    }

    @Bean
    public DashboardService dashboardService(
            MarketDataRepositoryPort marketDataRepositoryPort, Environment env) {
        return new DashboardService(marketDataRepositoryPort, trackedTickers(env));
    }

    /** Binds {@code stock-insights.tracked-tickers} (a YAML list) to a {@code List<String>}. */
    private static List<String> trackedTickers(Environment env) {
        return Binder.get(env)
                .bind("stock-insights.tracked-tickers", Bindable.listOf(String.class))
                .orElse(List.of());
    }
}
