package com.amitrangralabs.stockinsights.adapter.in.config;

import com.amitrangralabs.stockinsights.adapter.in.endpoint.DashboardEndpoint;
import com.amitrangralabs.stockinsights.adapter.in.endpoint.HealthEndpoint;
import com.amitrangralabs.stockinsights.adapter.in.endpoint.HomeEndpoint;
import com.amitrangralabs.stockinsights.adapter.in.endpoint.RefreshScheduler;
import com.amitrangralabs.stockinsights.domain.service.DashboardService;
import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Explicit wiring for all inbound adapters (web endpoints, REST endpoints, schedulers).
 *
 * <p>One {@code @Bean} method per inbound adapter class. As the app grows, domain services built in
 * {@code DomainConfig} are injected here as method parameters and passed to the endpoint
 * constructors — so this file remains the single, readable description of how HTTP/scheduled entry
 * points connect to the domain. No {@code @Controller}/{@code @Component} scanning is involved.
 */
@Configuration
public class InboundConfig {

    /**
     * Makes Spring Boot use {@link EndpointHandlerMapping}, so our stereotype-free {@code @Bean}
     * endpoints (type-level {@code @RequestMapping}, no {@code @Controller}) are routed. Without
     * this, Spring 6.2+ would ignore them. See {@link EndpointHandlerMapping} for details.
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new EndpointHandlerMapping();
            }
        };
    }

    @Bean
    public HealthEndpoint healthEndpoint() {
        return new HealthEndpoint();
    }

    @Bean
    public HomeEndpoint homeEndpoint() {
        return new HomeEndpoint();
    }

    @Bean
    public DashboardEndpoint dashboardEndpoint(DashboardService dashboardService) {
        return new DashboardEndpoint(dashboardService);
    }

    @Bean
    public RefreshScheduler refreshScheduler(MarketDataRefreshService marketDataRefreshService) {
        return new RefreshScheduler(marketDataRefreshService);
    }
}
