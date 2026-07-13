package com.amitrangralabs.stockinsights;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the observability surface: the in-app {@code /observability} page renders and embeds
 * Grafana, and the Actuator Prometheus scrape endpoint exposes the custom cache-freshness gauge.
 *
 * <p>{@code @AutoConfigureObservability} is required because Spring Boot's test framework disables
 * metrics export (and thus the Prometheus registry + scrape endpoint) by default under
 * {@code @SpringBootTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class ObservabilityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void observabilityPageRendersAndEmbedsGrafana() throws Exception {
        mockMvc.perform(get("/observability"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Observability")))
                .andExpect(content().string(containsString("<iframe")))
                .andExpect(content().string(containsString("localhost:3000")));
    }

    @Test
    void prometheusScrapeEndpointExposesCustomFreshnessGauge() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cache_quote_fresh_ratio")));
    }
}
