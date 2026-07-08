package com.amitrangralabs.stockinsights.domain.config;

import org.springframework.context.annotation.Configuration;

/**
 * Explicit wiring for the domain layer (business services in {@code domain.service}).
 *
 * <p>Domain services are plain Java objects with no framework annotations. They receive their
 * outbound {@code port} dependencies through constructors. As services are added (starting in M1
 * with {@code MarketDataRefreshService}, {@code DashboardService}, etc.), each gets one
 * {@code @Bean} method here, taking the relevant {@code *Port} beans (built in {@code OutboundConfig})
 * as method parameters. This keeps the domain object graph readable in one place.
 *
 * <p>Empty for M0 — no domain services exist yet.
 */
@Configuration
public class DomainConfig {
}
