package com.amitrangralabs.stockinsights.adapter.out.config;

import com.amitrangralabs.stockinsights.adapter.out.client.CatalogSymbolSearch;
import com.amitrangralabs.stockinsights.adapter.out.client.FinnhubClient;
import com.amitrangralabs.stockinsights.adapter.out.client.FinnhubTradeStream;
import com.amitrangralabs.stockinsights.adapter.out.client.H2PriceHistoryRepository;
import com.amitrangralabs.stockinsights.adapter.out.client.H2RepositoryClient;
import com.amitrangralabs.stockinsights.adapter.out.client.H2WatchlistRepository;
import com.amitrangralabs.stockinsights.adapter.out.client.YahooFinanceClient;
import com.amitrangralabs.stockinsights.adapter.out.metrics.CacheFreshnessMetrics;
import com.amitrangralabs.stockinsights.domain.service.PriceStreamService;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import com.amitrangralabs.stockinsights.port.SymbolSearchPort;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

/**
 * Explicit wiring for all outbound adapters. Each concrete client is constructed by hand and
 * exposed as its <em>port interface</em> type, so the domain only ever sees the interface.
 *
 * <p>Config values are read from the {@link Environment} (bound from {@code application.yml} /
 * environment variables) rather than injected via annotations — keeping the wiring visible here.
 * Swapping the data provider is a one-line change to {@link #marketDataPort}.
 */
@Configuration
public class OutboundConfig {

    /** A {@link RestClient} pre-pointed at Finnhub's base URL. */
    @Bean
    public RestClient finnhubRestClient(RestClient.Builder builder, Environment env) {
        String baseUrl = env.getProperty(
                "stock-insights.market-data.finnhub.base-url", "https://finnhub.io/api/v1");
        return builder.baseUrl(baseUrl).build();
    }

    /** The market-data provider. Swap the implementation here to change providers. */
    @Bean
    public MarketDataPort marketDataPort(RestClient finnhubRestClient, Environment env) {
        String apiKey = env.getProperty("stock-insights.market-data.finnhub.api-key", "");
        long minCallIntervalMs = env.getProperty(
                "stock-insights.market-data.finnhub.min-call-interval-ms", Long.class, 1100L);
        return new FinnhubClient(finnhubRestClient, apiKey, minCallIntervalMs);
    }

    /** The local quote/profile cache, backed by H2 through {@link JdbcClient}. */
    @Bean
    public MarketDataRepositoryPort marketDataRepositoryPort(JdbcClient jdbcClient) {
        return new H2RepositoryClient(jdbcClient);
    }

    /** A {@link RestClient} for Yahoo Finance, with a browser-like User-Agent (Yahoo requires one). */
    @Bean
    public RestClient yahooRestClient(RestClient.Builder builder, Environment env) {
        String baseUrl = env.getProperty(
                "stock-insights.market-data.yahoo.base-url", "https://query1.finance.yahoo.com");
        return builder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Mozilla/5.0 (stock-insights)")
                .build();
    }

    /** The price-history provider (keyless). Swap the implementation here to change providers. */
    @Bean
    public PriceHistoryPort priceHistoryPort(RestClient yahooRestClient, Environment env) {
        String range = env.getProperty("stock-insights.market-data.yahoo.range", "3mo");
        String interval = env.getProperty("stock-insights.market-data.yahoo.interval", "1d");
        return new YahooFinanceClient(yahooRestClient, range, interval);
    }

    /** The local price-history cache, backed by H2 through {@link JdbcClient}. */
    @Bean
    public PriceHistoryRepositoryPort priceHistoryRepositoryPort(JdbcClient jdbcClient) {
        return new H2PriceHistoryRepository(jdbcClient);
    }

    /** The persisted watchlist, backed by H2 through {@link JdbcClient}. */
    @Bean
    public WatchlistPort watchlistPort(JdbcClient jdbcClient) {
        return new H2WatchlistRepository(jdbcClient);
    }

    /** Symbol search for add-ticker autocomplete, backed by the bundled catalog (keyless). */
    @Bean
    public SymbolSearchPort symbolSearchPort() {
        return new CatalogSymbolSearch();
    }

    /** Real-time trade stream from Finnhub's WebSocket (no-op without an API key). */
    @Bean(destroyMethod = "close")
    public FinnhubTradeStream finnhubTradeStream(
            WatchlistPort watchlistPort,
            PriceStreamService priceStreamService,
            ObjectMapper objectMapper,
            Environment env) {
        String wsUrl = env.getProperty(
                "stock-insights.market-data.finnhub.ws-url", "wss://ws.finnhub.io");
        String apiKey = env.getProperty("stock-insights.market-data.finnhub.api-key", "");
        return new FinnhubTradeStream(wsUrl, apiKey, watchlistPort, priceStreamService, objectMapper);
    }

    /** Starts the trade stream once the application is up. */
    @Bean
    public ApplicationRunner tradeStreamStarter(FinnhubTradeStream finnhubTradeStream) {
        return args -> finnhubTradeStream.start();
    }

    /**
     * Cache-freshness gauges for Prometheus. A quote counts as "fresh" if it is younger than twice
     * the live-refresh interval, so a single missed refresh cycle does not breach the freshness SLI.
     * Registered as a {@link CacheFreshnessMetrics} bean; Spring Boot's Micrometer auto-config binds
     * every {@code MeterBinder} bean to the registry.
     */
    @Bean
    public CacheFreshnessMetrics cacheFreshnessMetrics(
            JdbcClient jdbcClient, WatchlistPort watchlistPort, Environment env) {
        long liveIntervalMs =
                env.getProperty("stock-insights.refresh-interval-ms", Long.class, 300000L);
        long freshnessThresholdSeconds = (liveIntervalMs / 1000L) * 2L;
        return new CacheFreshnessMetrics(
                jdbcClient, watchlistPort, Clock.systemUTC(), freshnessThresholdSeconds);
    }
}
