package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import com.amitrangralabs.stockinsights.port.MarketDataException;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure unit tests for Journey A — no Spring context, hand-built fake port. */
class MarketDataRefreshServiceTest {

    private static Quote quote(String ticker) {
        return new Quote(ticker, 100, 1, 1, 101, 99, 100, 99, Instant.parse("2026-01-02T15:00:00Z"));
    }

    private static CompanyProfile profile(String ticker) {
        return new CompanyProfile(ticker, ticker + " Inc", "NASDAQ", "USD", "Tech", 1000, "", "");
    }

    @Test
    void refreshesQuoteAndProfileForEveryTicker() {
        var repo = new FakeMarketDataRepository();
        MarketDataPort port = new MarketDataPort() {
            @Override
            public Quote fetchQuote(String ticker) {
                return quote(ticker);
            }

            @Override
            public CompanyProfile fetchProfile(String ticker) {
                return profile(ticker);
            }
        };
        var service = new MarketDataRefreshService(port, repo, List.of("AAPL", "MSFT"));

        service.refreshAll();

        assertThat(repo.quotes.keySet()).containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(repo.profiles.keySet()).containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(repo.quotes.get("AAPL").current()).isEqualTo(100);
    }

    @Test
    void oneFailingTickerDoesNotAbortTheRest() {
        var repo = new FakeMarketDataRepository();
        MarketDataPort port = new MarketDataPort() {
            @Override
            public Quote fetchQuote(String ticker) {
                if (ticker.equals("BAD")) {
                    throw new MarketDataException("boom");
                }
                return quote(ticker);
            }

            @Override
            public CompanyProfile fetchProfile(String ticker) {
                if (ticker.equals("BAD")) {
                    throw new MarketDataException("boom");
                }
                return profile(ticker);
            }
        };
        var service = new MarketDataRefreshService(port, repo, List.of("AAPL", "BAD", "MSFT"));

        service.refreshAll();

        assertThat(repo.quotes.keySet()).isEqualTo(Set.of("AAPL", "MSFT"));
        assertThat(repo.profiles.keySet()).isEqualTo(Set.of("AAPL", "MSFT"));
    }

    @Test
    void quoteIsStoredEvenIfProfileFetchFails() {
        var repo = new FakeMarketDataRepository();
        MarketDataPort port = new MarketDataPort() {
            @Override
            public Quote fetchQuote(String ticker) {
                return quote(ticker);
            }

            @Override
            public CompanyProfile fetchProfile(String ticker) {
                throw new MarketDataException("profile unavailable");
            }
        };
        var service = new MarketDataRefreshService(port, repo, List.of("AAPL"));

        service.refreshAll();

        assertThat(repo.quotes).containsKey("AAPL");
        assertThat(repo.profiles).isEmpty();
    }
}
