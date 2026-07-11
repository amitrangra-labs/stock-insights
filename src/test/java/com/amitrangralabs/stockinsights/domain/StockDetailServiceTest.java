package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.object.StockDetail;
import com.amitrangralabs.stockinsights.domain.service.StockDetailService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Pure unit tests for Journey C — reads cached data through fake repositories. */
class StockDetailServiceTest {

    private StockDetailService serviceWithData() {
        var cache = new FakeMarketDataRepository();
        cache.saveQuote(new Quote("AAPL", 180, 1, 0.5, 181, 179, 180, 179,
                Instant.parse("2026-01-02T15:00:00Z")));
        cache.saveProfile(new CompanyProfile("AAPL", "Apple Inc", "NASDAQ", "USD", "Tech", 3_000_000, "", ""));
        var histRepo = new FakePriceHistoryRepository();
        histRepo.saveHistory("AAPL", List.of(
                new PricePoint(LocalDate.of(2026, 1, 1), 178, 181, 177, 179, 1_000_000),
                new PricePoint(LocalDate.of(2026, 1, 2), 179, 182, 178, 180, 1_200_000)));
        return new StockDetailService(cache, histRepo, List.of("AAPL", "MSFT"));
    }

    @Test
    void assemblesProfileQuoteAndHistoryForTrackedTicker() {
        Optional<StockDetail> detail = serviceWithData().getDetail("AAPL");

        assertThat(detail).isPresent();
        StockDetail d = detail.get();
        assertThat(d.displayName()).isEqualTo("Apple Inc");
        assertThat(d.hasQuote()).isTrue();
        assertThat(d.hasProfile()).isTrue();
        assertThat(d.hasHistory()).isTrue();
        assertThat(d.history()).hasSize(2);
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertThat(serviceWithData().getDetail("aapl")).isPresent();
        assertThat(serviceWithData().getHistory("aapl")).hasSize(2);
    }

    @Test
    void untrackedTickerIsEmptyAndNotFetched() {
        var service = serviceWithData();
        assertThat(service.getDetail("TSLA")).isEmpty();
        assertThat(service.getHistory("TSLA")).isEmpty();
    }

    @Test
    void trackedTickerWithNoCachedDataStillReturnsDetail() {
        var service = new StockDetailService(
                new FakeMarketDataRepository(), new FakePriceHistoryRepository(), List.of("MSFT"));

        Optional<StockDetail> detail = service.getDetail("MSFT");

        assertThat(detail).isPresent();
        assertThat(detail.get().displayName()).isEqualTo("MSFT"); // falls back to ticker
        assertThat(detail.get().hasQuote()).isFalse();
        assertThat(detail.get().hasHistory()).isFalse();
    }
}
