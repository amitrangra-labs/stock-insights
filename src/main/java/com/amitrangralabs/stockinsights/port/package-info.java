/**
 * Outbound ports: interfaces the domain uses to reach the outside world.
 *
 * <p>These interfaces sit on the boundary between {@code domain.service} and
 * {@code adapter.out.client}. The domain depends only on these interfaces; concrete
 * implementations live in {@code adapter.out.client} and are wired in {@code OutboundConfig}.
 *
 * <p>There is intentionally no inbound port layer: inbound adapters
 * ({@code adapter.in.endpoint}) call domain services directly as a concrete dependency.
 *
 * <p>This package contains interfaces only — nothing to construct, so it has no configuration
 * class. Planned interfaces: {@code MarketDataPort} (fetch quote/profile/financials/estimates/
 * news/ratings) and {@code MarketDataRepositoryPort} (save/find cached data).
 */
package com.amitrangralabs.stockinsights.port;
