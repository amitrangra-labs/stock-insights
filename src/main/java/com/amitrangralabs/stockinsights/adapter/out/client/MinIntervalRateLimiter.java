package com.amitrangralabs.stockinsights.adapter.out.client;

/**
 * A tiny throughput limiter that enforces a minimum interval between calls.
 *
 * <p>Used by {@link FinnhubClient} to space out requests so a burst (a scheduled refresh plus an
 * on-add refresh) never exceeds the provider's per-minute cap. Thread-safe: concurrent callers are
 * serialised and paced, each waiting its turn.
 *
 * <p>A non-positive interval disables throttling ({@link #acquire()} returns immediately).
 */
final class MinIntervalRateLimiter {

    private final long minIntervalNanos;
    private long nextAllowedNanos;

    MinIntervalRateLimiter(long minIntervalMillis) {
        this.minIntervalNanos = Math.max(0L, minIntervalMillis) * 1_000_000L;
        this.nextAllowedNanos = System.nanoTime();
    }

    /** Block until at least the configured interval has elapsed since the previous call. */
    synchronized void acquire() {
        if (minIntervalNanos == 0L) {
            return;
        }
        long now = System.nanoTime();
        if (now < nextAllowedNanos) {
            long waitNanos = nextAllowedNanos - now;
            try {
                Thread.sleep(waitNanos / 1_000_000L, (int) (waitNanos % 1_000_000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            now = System.nanoTime();
        }
        nextAllowedNanos = Math.max(now, nextAllowedNanos) + minIntervalNanos;
    }
}
