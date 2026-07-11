package com.amitrangralabs.stockinsights;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
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

/**
 * Renders the real detail template with seeded cache data through the full Spring context, proving
 * the fundamentals / analyst-ratings / news sections render (and that the template avoids the
 * unavailable {@code #temporals} dialect).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DetailPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketDataRepositoryPort repository;

    @Autowired
    private WatchlistPort watchlist;

    @Test
    void detailPageRendersAllSections() throws Exception {
        watchlist.add("AAPL");
        repository.saveQuote(new Quote("AAPL", 190.5, 2.5, 1.33, 191, 188, 189, 188,
                Instant.parse("2026-06-30T20:00:00Z")));
        repository.saveProfile(new CompanyProfile("AAPL", "Apple Inc", "NASDAQ", "USD",
                "Technology", 3_000_000, "", "https://apple.com"));
        repository.saveFundamentals(new Fundamentals("AAPL", 28.5, 6.2, 320.1, 210.0, 0.55, 1.2));
        repository.saveRating("AAPL", new AnalystRating(LocalDate.of(2026, 6, 1), 10, 15, 5, 2, 1));
        repository.saveNews("AAPL", List.of(new NewsItem(
                42L, "Apple unveils something", "WSJ", "https://wsj.example/story",
                "A summary.", Instant.parse("2026-06-28T09:30:00Z"), "")));

        mockMvc.perform(get("/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.allOf(
                        Matchers.containsString("Apple Inc"),
                        Matchers.containsString("Fundamentals"),
                        Matchers.containsString("28.50"),          // P/E
                        Matchers.containsString("Analyst ratings"),
                        Matchers.containsString("Jun 2026"),        // periodDisplay()
                        Matchers.containsString("Recent news"),
                        Matchers.containsString("Apple unveils something"),
                        Matchers.containsString("2026-06-28"))));   // publishedDisplay()
    }
}
