package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import com.amitrangralabs.stockinsights.port.MarketDataException;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure unit tests for Journey A — no Spring context, hand-built fake ports. */
class MarketDataRefreshServiceTest {

    private static Quote quote(String ticker) {
        return new Quote(ticker, 100, 1, 1, 101, 99, 100, 99, Instant.parse("2026-01-02T15:00:00Z"));
    }

    private static CompanyProfile profile(String ticker) {
        return new CompanyProfile(ticker, ticker + " Inc", "NASDAQ", "USD", "Tech", 1000, "", "");
    }

    private static List<PricePoint> history(String ticker) {
        return List.of(new PricePoint(LocalDate.of(2026, 1, 2), 99, 101, 98, 100, 1_000_000));
    }

    /** Fake market-data provider; throws for any ticker in {@code failing}. */
    private static MarketDataPort marketDataPort(Set<String> failing) {
        return new MarketDataPort() {
            @Override
            public Quote fetchQuote(String ticker) {
                if (failing.contains(ticker)) throw new MarketDataException("boom");
                return quote(ticker);
            }

            @Override
            public CompanyProfile fetchProfile(String ticker) {
                if (failing.contains(ticker)) throw new MarketDataException("boom");
                return profile(ticker);
            }
        };
    }

    private static PriceHistoryPort priceHistoryPort(Set<String> failing) {
        return ticker -> {
            if (failing.contains(ticker)) throw new MarketDataException("boom");
            return history(ticker);
        };
    }

    @Test
    void refreshesQuoteProfileAndHistoryForEveryTicker() {
        var repo = new FakeMarketDataRepository();
        var histRepo = new FakePriceHistoryRepository();
        var service = new MarketDataRefreshService(
                marketDataPort(Set.of()), repo, priceHistoryPort(Set.of()), histRepo,
                new FakeWatchlist("AAPL", "MSFT"));

        service.refreshAll();

        assertThat(repo.quotes.keySet()).containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(repo.profiles.keySet()).containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(histRepo.history.keySet()).containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(histRepo.history.get("AAPL")).hasSize(1);
    }

    @Test
    void oneFailingTickerDoesNotAbortTheRest() {
        var repo = new FakeMarketDataRepository();
        var histRepo = new FakePriceHistoryRepository();
        var service = new MarketDataRefreshService(
                marketDataPort(Set.of("BAD")), repo, priceHistoryPort(Set.of("BAD")), histRepo,
                new FakeWatchlist("AAPL", "BAD", "MSFT"));

        service.refreshAll();

        assertThat(repo.quotes.keySet()).isEqualTo(Set.of("AAPL", "MSFT"));
        assertThat(histRepo.history.keySet()).isEqualTo(Set.of("AAPL", "MSFT"));
    }

    @Test
    void failingHistoryDoesNotBlockQuoteAndProfile() {
        var repo = new FakeMarketDataRepository();
        var histRepo = new FakePriceHistoryRepository();
        // Quote/profile succeed; history provider fails for everything.
        var service = new MarketDataRefreshService(
                marketDataPort(Set.of()), repo, priceHistoryPort(Set.of("AAPL")), histRepo,
                new FakeWatchlist("AAPL"));

        service.refreshAll();

        assertThat(repo.quotes).containsKey("AAPL");
        assertThat(repo.profiles).containsKey("AAPL");
        assertThat(histRepo.history).isEmpty();
    }
}
