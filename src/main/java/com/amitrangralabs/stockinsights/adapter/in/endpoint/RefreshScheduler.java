package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Journey A trigger: periodically drives {@link MarketDataRefreshService} on two cadences.
 *
 * <p>Constructed in {@code InboundConfig}. Cadences come from configuration; initial delays let the
 * app finish starting before the first fetch. {@code @Scheduled} here is a scheduling directive
 * (processed by {@code @EnableScheduling} on {@code Application}), not dependency injection —
 * consistent with the project's no-autowiring rule.
 *
 * <p>Each cadence is wrapped in a Micrometer {@link Timer} ({@code refresh.cycle.duration} tagged by
 * {@code tier}), so Prometheus/Grafana can chart refresh duration and, via the timer's count,
 * confirm the background refresh loop is still running (cadence liveness) — a freshness-plane SLI.
 */
public class RefreshScheduler {

    private final MarketDataRefreshService refreshService;
    private final Timer liveRefreshTimer;
    private final Timer referenceRefreshTimer;

    public RefreshScheduler(MarketDataRefreshService refreshService, MeterRegistry meterRegistry) {
        this.refreshService = refreshService;
        this.liveRefreshTimer =
                Timer.builder("refresh.cycle.duration")
                        .tag("tier", "live")
                        .description("Duration of a live (quote + news) refresh cycle")
                        .register(meterRegistry);
        this.referenceRefreshTimer =
                Timer.builder("refresh.cycle.duration")
                        .tag("tier", "reference")
                        .description("Duration of a reference (profile/fundamentals/ratings/history) refresh cycle")
                        .register(meterRegistry);
    }

    /** Frequently-changing data (quote + news). */
    @Scheduled(
            initialDelayString = "${stock-insights.refresh-initial-delay-ms:10000}",
            fixedDelayString = "${stock-insights.refresh-interval-ms:300000}")
    public void refreshLive() {
        liveRefreshTimer.record(refreshService::refreshLive);
    }

    /** Slowly-changing data (profile, fundamentals, ratings, history). */
    @Scheduled(
            initialDelayString = "${stock-insights.reference-refresh-initial-delay-ms:20000}",
            fixedDelayString = "${stock-insights.reference-refresh-interval-ms:86400000}")
    public void refreshReference() {
        referenceRefreshTimer.record(refreshService::refreshReference);
    }
}
