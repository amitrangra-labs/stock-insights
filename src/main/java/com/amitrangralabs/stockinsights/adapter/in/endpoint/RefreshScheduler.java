package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Journey A trigger: periodically drives {@link MarketDataRefreshService} on two cadences.
 *
 * <p>Constructed in {@code InboundConfig}. Cadences come from configuration; initial delays let the
 * app finish starting before the first fetch. {@code @Scheduled} here is a scheduling directive
 * (processed by {@code @EnableScheduling} on {@code Application}), not dependency injection —
 * consistent with the project's no-autowiring rule.
 */
public class RefreshScheduler {

    private final MarketDataRefreshService refreshService;

    public RefreshScheduler(MarketDataRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    /** Frequently-changing data (quote + news). */
    @Scheduled(
            initialDelayString = "${stock-insights.refresh-initial-delay-ms:10000}",
            fixedDelayString = "${stock-insights.refresh-interval-ms:300000}")
    public void refreshLive() {
        refreshService.refreshLive();
    }

    /** Slowly-changing data (profile, fundamentals, ratings, history). */
    @Scheduled(
            initialDelayString = "${stock-insights.reference-refresh-initial-delay-ms:20000}",
            fixedDelayString = "${stock-insights.reference-refresh-interval-ms:86400000}")
    public void refreshReference() {
        refreshService.refreshReference();
    }
}
