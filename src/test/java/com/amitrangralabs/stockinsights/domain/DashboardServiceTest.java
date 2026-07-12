package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.DashboardRow;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.DashboardService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure unit tests for Journey B — reads cached data through fake repositories. */
class DashboardServiceTest {

    private DashboardService service(FakeMarketDataRepository repo, FakePriceHistoryRepository hist,
            FakeWatchlist watchlist) {
        return new DashboardService(repo, hist, watchlist);
    }

    @Test
    void buildsRichRowsPerTrackedTickerInOrder() {
        var repo = new FakeMarketDataRepository();
        repo.saveQuote(new Quote("MSFT", 420.5, 2.5, 0.6, 421, 418, 419,
                418, Instant.parse("2026-01-02T15:00:00Z")));
        repo.saveProfile(new CompanyProfile("MSFT", "Microsoft Corp", "NASDAQ", "USD",
                "Tech", 3_000_000, "", ""));
        repo.saveFundamentals(new Fundamentals("MSFT", 30.0, 12.0, 450.0, 300.0, 0.8, 0.9));
        var hist = new FakePriceHistoryRepository();
        hist.saveHistory("MSFT", List.of(
                new PricePoint(LocalDate.of(2026, 1, 1), 410, 415, 409, 412, 20_000_000),
                new PricePoint(LocalDate.of(2026, 1, 2), 412, 421, 411, 419, 25_500_000)));

        var service = service(repo, hist, new FakeWatchlist("AAPL", "MSFT"));
        List<DashboardRow> rows = service.getDashboard().rows();

        assertThat(rows).extracting(DashboardRow::ticker).containsExactly("AAPL", "MSFT");

        DashboardRow msft = rows.get(1);
        assertThat(msft.name()).isEqualTo("Microsoft Corp");
        assertThat(msft.price()).isEqualTo(420.5);
        assertThat(msft.hasQuote()).isTrue();
        assertThat(msft.isUp()).isTrue();
        assertThat(msft.hasDayRange()).isTrue();
        assertThat(msft.has52WeekRange()).isTrue();
        assertThat(msft.volume()).isEqualTo(25_500_000L); // latest history volume
        assertThat(msft.volumeDisplay()).isEqualTo("25.50M");
        assertThat(msft.marketCapDisplay()).isEqualTo("3.00T"); // 3,000,000 millions
        assertThat(msft.hasSpark()).isTrue();
        assertThat(msft.sparkPoints()).isNotBlank();
        assertThat(msft.sparkUp()).isTrue(); // 412 -> 419
    }

    @Test
    void tickerWithNoCachedDataStillGetsAPlaceholderRow() {
        var service = service(new FakeMarketDataRepository(), new FakePriceHistoryRepository(),
                new FakeWatchlist("AAPL"));

        DashboardRow row = service.getDashboard().rows().get(0);

        assertThat(row.ticker()).isEqualTo("AAPL");
        assertThat(row.name()).isEqualTo("AAPL"); // falls back to the ticker
        assertThat(row.hasQuote()).isFalse();
        assertThat(row.hasDayRange()).isFalse();
        assertThat(row.hasSpark()).isFalse();
        assertThat(row.volumeDisplay()).isEqualTo("—");
        assertThat(row.marketCapDisplay()).isEqualTo("—");
        assertThat(row.asOfDisplay()).isEqualTo("—");
    }

    @Test
    void negativeChangeIsNotMarkedUp() {
        var repo = new FakeMarketDataRepository();
        repo.saveQuote(new Quote("AAPL", 180, -1.5, -0.8, 182, 179, 181,
                181.5, Instant.parse("2026-01-02T15:00:00Z")));
        var service = service(repo, new FakePriceHistoryRepository(), new FakeWatchlist("AAPL"));

        assertThat(service.getDashboard().rows().get(0).isUp()).isFalse();
    }

    @Test
    void summaryCountsBreadthAndTopMovers() {
        var repo = new FakeMarketDataRepository();
        repo.saveQuote(new Quote("AAPL", 100, 5, 5.0, 101, 95, 96, 95,
                Instant.parse("2026-01-02T15:00:00Z")));   // +5%
        repo.saveQuote(new Quote("MSFT", 100, -2, -2.0, 103, 99, 102, 102,
                Instant.parse("2026-01-02T15:00:00Z")));   // -2%
        repo.saveQuote(new Quote("NVDA", 100, -8, -8.0, 110, 99, 108, 108,
                Instant.parse("2026-01-02T15:00:00Z")));   // -8%
        var service = service(repo, new FakePriceHistoryRepository(),
                new FakeWatchlist("AAPL", "MSFT", "NVDA"));

        var summary = service.getDashboard().summary();

        assertThat(summary.up()).isEqualTo(1);
        assertThat(summary.down()).isEqualTo(2);
        assertThat(summary.topGainer()).isEqualTo("AAPL");
        assertThat(summary.topLoser()).isEqualTo("NVDA");
    }
}
