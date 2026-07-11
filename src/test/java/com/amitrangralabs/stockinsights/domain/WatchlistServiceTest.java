package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.service.WatchlistService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for watchlist add/remove normalisation and validation. */
class WatchlistServiceTest {

    @Test
    void addNormalisesToUppercaseAndTrims() {
        var service = new WatchlistService(new FakeWatchlist());
        Optional<String> added = service.add("  tsla ");
        assertThat(added).contains("TSLA");
        assertThat(service.tickers()).containsExactly("TSLA");
    }

    @Test
    void addRejectsInvalidSymbols() {
        var service = new WatchlistService(new FakeWatchlist());
        assertThat(service.add("")).isEmpty();
        assertThat(service.add("  ")).isEmpty();
        assertThat(service.add("!!!")).isEmpty();
        assertThat(service.add("123")).isEmpty(); // must start with a letter
        assertThat(service.tickers()).isEmpty();
    }

    @Test
    void addIsIdempotentAndCaseInsensitive() {
        var service = new WatchlistService(new FakeWatchlist("AAPL"));
        assertThat(service.add("aapl")).contains("AAPL");
        assertThat(service.tickers()).containsExactly("AAPL"); // no duplicate
    }

    @Test
    void removeIsCaseInsensitive() {
        var service = new WatchlistService(new FakeWatchlist("AAPL", "MSFT"));
        service.remove("aapl");
        assertThat(service.tickers()).containsExactly("MSFT");
    }

    @Test
    void isTrackedReflectsMembership() {
        var service = new WatchlistService(new FakeWatchlist("AAPL"));
        assertThat(service.isTracked("aapl")).isTrue();
        assertThat(service.isTracked("TSLA")).isFalse();
    }
}
