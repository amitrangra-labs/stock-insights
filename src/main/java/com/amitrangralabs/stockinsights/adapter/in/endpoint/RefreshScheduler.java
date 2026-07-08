package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Journey A trigger: periodically drives {@link MarketDataRefreshService}.
 *
 * <p>Constructed in {@code InboundConfig}. The cadence comes from configuration; the initial delay
 * lets the app finish starting before the first fetch. {@code @Scheduled} here is a scheduling
 * directive (processed by {@code @EnableScheduling} on {@code Application}), not dependency
 * injection — consistent with the project's no-autowiring rule.
 */
public class RefreshScheduler {

    private final MarketDataRefreshService refreshService;

    public RefreshScheduler(MarketDataRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Scheduled(
            initialDelayString = "${stock-insights.refresh-initial-delay-ms:10000}",
            fixedDelayString = "${stock-insights.refresh-interval-ms:300000}")
    public void refresh() {
        refreshService.refreshAll();
    }
}
