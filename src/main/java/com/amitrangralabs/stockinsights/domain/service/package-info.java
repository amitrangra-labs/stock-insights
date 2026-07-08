/**
 * Domain services: the business logic core.
 *
 * <p>Plain Java classes with <strong>zero</strong> framework imports (no Spring, JPA, or HTTP
 * client). They depend only on {@code domain.object} types and the outbound {@code port}
 * interfaces, injected via constructors. Each service is instantiated by hand in
 * {@code DomainConfig}.
 *
 * <p>Planned services: {@code MarketDataRefreshService}, {@code DashboardService},
 * {@code StockDetailService}, {@code PriceHistoryService}.
 */
package com.amitrangralabs.stockinsights.domain.service;
