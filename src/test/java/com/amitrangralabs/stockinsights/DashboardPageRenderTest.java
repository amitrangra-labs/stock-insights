package com.amitrangralabs.stockinsights;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Renders the rich dashboard through the full context to prove the new cells render. */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketDataRepositoryPort repository;

    @Autowired
    private PriceHistoryRepositoryPort priceHistory;

    @Autowired
    private WatchlistPort watchlist;

    @Test
    void dashboardRendersRichColumnsAndSparkline() throws Exception {
        watchlist.add("ZTST");
        repository.saveQuote(new Quote("ZTST", 100, 1.5, 1.5, 101, 98, 99, 98.5,
                Instant.parse("2026-06-30T20:00:00Z")));
        repository.saveProfile(new CompanyProfile("ZTST", "Ztest Corp", "NASDAQ", "USD",
                "Tech", 250_000, "", ""));
        repository.saveFundamentals(new Fundamentals("ZTST", 20.0, 5.0, 120.0, 80.0, 0.0, 1.0));
        priceHistory.saveHistory("ZTST", List.of(
                new PricePoint(LocalDate.of(2026, 6, 29), 96, 99, 95, 97, 10_000_000),
                new PricePoint(LocalDate.of(2026, 6, 30), 97, 101, 96, 100, 12_340_000)));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.allOf(
                        Matchers.containsString("Trend (30d)"),
                        Matchers.containsString("Day range"),
                        Matchers.containsString("52-wk range"),
                        Matchers.containsString("<polyline"),        // sparkline rendered
                        Matchers.containsString("rangebar-marker"),  // range position bar
                        Matchers.containsString("12.34M"),           // volume display
                        Matchers.containsString("250.00B"))));       // market cap (250,000 M)
    }
}
