package com.amitrangralabs.stockinsights;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.adapter.in.endpoint.WatchlistEndpoint;
import com.amitrangralabs.stockinsights.domain.FakeMarketDataRepository;
import com.amitrangralabs.stockinsights.domain.FakePriceHistoryRepository;
import com.amitrangralabs.stockinsights.domain.FakeWatchlist;
import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.MarketDataRefreshService;
import com.amitrangralabs.stockinsights.domain.service.WatchlistService;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.amitrangralabs.stockinsights.port.PriceHistoryPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

/** Unit test for add/remove wiring, including the (here synchronous) on-add refresh. */
class WatchlistEndpointTest {

    private static final Executor SYNC = Runnable::run;

    private static MarketDataPort marketDataPort() {
        return new MarketDataPort() {
            public Quote fetchQuote(String t) {
                return new Quote(t, 100, 1, 1, 101, 99, 100, 99, Instant.parse("2026-01-02T15:00:00Z"));
            }

            public CompanyProfile fetchProfile(String t) {
                return new CompanyProfile(t, t + " Inc", "NASDAQ", "USD", "Tech", 1000, "", "");
            }

            public List<NewsItem> fetchNews(String t) {
                return List.of();
            }

            public AnalystRating fetchRatings(String t) {
                return new AnalystRating(LocalDate.of(2026, 1, 1), 1, 1, 1, 0, 0);
            }

            public Fundamentals fetchFundamentals(String t) {
                return new Fundamentals(t, 20.0, 5.0, 100.0, 80.0, 0.0, 1.0);
            }
        };
    }

    private WatchlistEndpoint endpoint(FakeWatchlist watchlist, FakeMarketDataRepository repo) {
        return endpoint(watchlist, repo, List.of());
    }

    private WatchlistEndpoint endpoint(
            FakeWatchlist watchlist, FakeMarketDataRepository repo, List<String> defaults) {
        PriceHistoryPort history = t ->
                List.of(new PricePoint(LocalDate.of(2026, 1, 2), 99, 101, 98, 100, 1_000_000));
        var refresh = new MarketDataRefreshService(
                marketDataPort(), repo, history, new FakePriceHistoryRepository(), watchlist);
        return new WatchlistEndpoint(new WatchlistService(watchlist, defaults), refresh, SYNC);
    }

    @Test
    void addTracksTickerRefreshesItAndRedirects() {
        var watchlist = new FakeWatchlist();
        var repo = new FakeMarketDataRepository();
        var endpoint = endpoint(watchlist, repo);

        String view = endpoint.add("tsla");

        assertThat(view).isEqualTo("redirect:/dashboard");
        assertThat(watchlist.contains("TSLA")).isTrue();      // normalised + added
        assertThat(repo.quotes).containsKey("TSLA");          // on-add refresh ran
    }

    @Test
    void addIgnoresInvalidSymbol() {
        var watchlist = new FakeWatchlist();
        var endpoint = endpoint(watchlist, new FakeMarketDataRepository());

        assertThat(endpoint.add("!!!")).isEqualTo("redirect:/dashboard");
        assertThat(watchlist.list()).isEmpty();
    }

    @Test
    void removeUntracksTicker() {
        var watchlist = new FakeWatchlist("AAPL", "MSFT");
        var endpoint = endpoint(watchlist, new FakeMarketDataRepository());

        String view = endpoint.remove("aapl");

        assertThat(view).isEqualTo("redirect:/dashboard");
        assertThat(watchlist.list()).containsExactly("MSFT");
    }

    @Test
    void resetClearsCustomListAndRestoresDefaults() {
        var watchlist = new FakeWatchlist("TOST", "PLTR"); // user-added tickers
        var repo = new FakeMarketDataRepository();
        var endpoint = endpoint(watchlist, repo, List.of("NVDA", "AAPL"));

        String view = endpoint.reset();

        assertThat(view).isEqualTo("redirect:/dashboard");
        assertThat(watchlist.list()).containsExactly("NVDA", "AAPL"); // custom removed, defaults in
        assertThat(repo.quotes.keySet()).contains("NVDA", "AAPL");     // refetched (sync executor)
    }
}
