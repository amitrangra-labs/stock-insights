package com.amitrangralabs.stockinsights.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Timing behaviour of the Finnhub call throttle. */
class MinIntervalRateLimiterTest {

    @Test
    void zeroIntervalDoesNotBlock() {
        var limiter = new MinIntervalRateLimiter(0);
        long start = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            limiter.acquire();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        assertThat(elapsedMs).isLessThan(100);
    }

    @Test
    void spacesCallsByAtLeastTheInterval() {
        long interval = 50;
        var limiter = new MinIntervalRateLimiter(interval);
        long start = System.nanoTime();
        limiter.acquire(); // first is immediate
        limiter.acquire(); // waits ~50ms
        limiter.acquire(); // waits ~50ms more
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        // Two gaps of ~50ms; allow scheduling slack on the upper bound.
        assertThat(elapsedMs).isGreaterThanOrEqualTo(2 * interval - 5);
        assertThat(elapsedMs).isLessThan(2000);
    }
}
