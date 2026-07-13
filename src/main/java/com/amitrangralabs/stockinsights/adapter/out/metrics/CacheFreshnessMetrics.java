package com.amitrangralabs.stockinsights.adapter.out.metrics;

import com.amitrangralabs.stockinsights.port.WatchlistPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Exposes cache-freshness gauges to Micrometer (and thus Prometheus/Grafana).
 *
 * <p>This is the observability counterpart of the app's core design: pages read from the H2 cache
 * and never call an upstream API inline, so <em>data freshness</em> is a first-class reliability
 * signal that is independent of request latency. See {@code docs/SLO.md} (freshness plane).
 *
 * <p>An outbound adapter: it reads the cache directly via {@link JdbcClient} (like the repository
 * adapters) and pushes to the metrics registry — the domain services stay framework-free. It is a
 * {@link MeterBinder}, so Spring Boot's Micrometer auto-configuration binds it to the registry
 * automatically once it is registered as a bean in {@code OutboundConfig}.
 *
 * <p>Two <em>aggregate</em> gauges are exposed rather than one series per ticker, to avoid
 * unbounded label cardinality as the watchlist grows:
 * <ul>
 *   <li>{@code cache.quote.oldest.age} — age (seconds) of the oldest cached quote across the
 *       watchlist; drives a "staleness" alert.</li>
 *   <li>{@code cache.quote.fresh.ratio} — fraction of watchlist tickers whose cached quote is within
 *       the freshness threshold; this is the freshness SLI.</li>
 * </ul>
 * Gauges are evaluated lazily on each Prometheus scrape via the supplier functions below.
 */
public class CacheFreshnessMetrics implements MeterBinder {

    private final JdbcClient jdbc;
    private final WatchlistPort watchlist;
    private final Clock clock;
    private final long freshnessThresholdMillis;

    /**
     * @param freshnessThresholdSeconds a cached quote counts as "fresh" if it is younger than this;
     *     typically twice the live-refresh interval so one missed cycle is tolerated.
     */
    public CacheFreshnessMetrics(
            JdbcClient jdbc, WatchlistPort watchlist, Clock clock, long freshnessThresholdSeconds) {
        this.jdbc = jdbc;
        this.watchlist = watchlist;
        this.clock = clock;
        this.freshnessThresholdMillis = freshnessThresholdSeconds * 1000L;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("cache.quote.oldest.age", this, CacheFreshnessMetrics::oldestQuoteAgeSeconds)
                .baseUnit("seconds")
                .description("Age of the oldest cached quote across the watchlist")
                .register(registry);
        Gauge.builder("cache.quote.fresh.ratio", this, CacheFreshnessMetrics::freshQuoteRatio)
                .description(
                        "Fraction of watchlist tickers whose cached quote is within the freshness threshold")
                .register(registry);
    }

    /** Seconds since the oldest tracked quote was cached; NaN when nothing is cached yet. */
    double oldestQuoteAgeSeconds() {
        List<Long> asOfMillis = cachedQuoteTimestamps();
        if (asOfMillis.isEmpty()) {
            return Double.NaN;
        }
        long oldest = asOfMillis.stream().min(Long::compare).orElseThrow();
        return (clock.millis() - oldest) / 1000.0;
    }

    /** Fraction of tracked tickers with a quote younger than the threshold; 1.0 when none tracked. */
    double freshQuoteRatio() {
        List<String> tickers = watchlist.list();
        if (tickers.isEmpty()) {
            return 1.0;
        }
        long cutoff = clock.millis() - freshnessThresholdMillis;
        long fresh = cachedQuoteTimestamps().stream().filter(asOf -> asOf >= cutoff).count();
        return (double) fresh / tickers.size();
    }

    /** {@code as_of} epoch-millis of every cached quote for a currently-tracked ticker. */
    private List<Long> cachedQuoteTimestamps() {
        List<String> tickers = watchlist.list();
        if (tickers.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT as_of FROM quotes WHERE ticker IN (:tickers)")
                .param("tickers", tickers)
                .query(Long.class)
                .list();
    }
}
