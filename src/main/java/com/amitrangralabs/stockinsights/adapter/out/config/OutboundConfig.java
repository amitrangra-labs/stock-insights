package com.amitrangralabs.stockinsights.adapter.out.config;

import com.amitrangralabs.stockinsights.adapter.out.client.FinnhubClient;
import com.amitrangralabs.stockinsights.adapter.out.client.H2RepositoryClient;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
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
        return new FinnhubClient(finnhubRestClient, apiKey);
    }

    /** The local cache, backed by H2 through {@link JdbcClient} (auto-configured by Boot). */
    @Bean
    public MarketDataRepositoryPort marketDataRepositoryPort(JdbcClient jdbcClient) {
        return new H2RepositoryClient(jdbcClient);
    }
}
