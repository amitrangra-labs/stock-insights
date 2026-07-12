package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.service.WatchlistService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for watchlist add/remove normalisation, validation, seeding, and reset. */
class WatchlistServiceTest {

    private static WatchlistService service(FakeWatchlist watchlist) {
        return new WatchlistService(watchlist, List.of());
    }

    @Test
    void addNormalisesToUppercaseAndTrims() {
        var service = service(new FakeWatchlist());
        Optional<String> added = service.add("  tsla ");
        assertThat(added).contains("TSLA");
        assertThat(service.tickers()).containsExactly("TSLA");
    }

    @Test
    void addRejectsInvalidSymbols() {
        var service = service(new FakeWatchlist());
        assertThat(service.add("")).isEmpty();
        assertThat(service.add("  ")).isEmpty();
        assertThat(service.add("!!!")).isEmpty();
        assertThat(service.add("123")).isEmpty(); // must start with a letter
        assertThat(service.tickers()).isEmpty();
    }

    @Test
    void addIsIdempotentAndCaseInsensitive() {
        var service = service(new FakeWatchlist("AAPL"));
        assertThat(service.add("aapl")).contains("AAPL");
        assertThat(service.tickers()).containsExactly("AAPL"); // no duplicate
    }

    @Test
    void removeIsCaseInsensitive() {
        var service = service(new FakeWatchlist("AAPL", "MSFT"));
        service.remove("aapl");
        assertThat(service.tickers()).containsExactly("MSFT");
    }

    @Test
    void isTrackedReflectsMembership() {
        var service = service(new FakeWatchlist("AAPL"));
        assertThat(service.isTracked("aapl")).isTrue();
        assertThat(service.isTracked("TSLA")).isFalse();
    }

    @Test
    void seedIfEmptyPopulatesDefaultsOnlyWhenEmpty() {
        var empty = new WatchlistService(new FakeWatchlist(), List.of("NVDA", "AAPL"));
        empty.seedIfEmpty();
        assertThat(empty.tickers()).containsExactly("NVDA", "AAPL");

        var nonEmpty = new WatchlistService(new FakeWatchlist("TSLA"), List.of("NVDA", "AAPL"));
        nonEmpty.seedIfEmpty();
        assertThat(nonEmpty.tickers()).containsExactly("TSLA"); // untouched
    }

    @Test
    void resetToDefaultsClearsCustomTickersAndRestoresDefaults() {
        var service = new WatchlistService(
                new FakeWatchlist("TSLA", "TOST"), List.of("NVDA", "AAPL", "MSFT"));

        List<String> result = service.resetToDefaults();

        assertThat(result).containsExactly("NVDA", "AAPL", "MSFT");
        assertThat(service.tickers()).containsExactly("NVDA", "AAPL", "MSFT");
    }
}
