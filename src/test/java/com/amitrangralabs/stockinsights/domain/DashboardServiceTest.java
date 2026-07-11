package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.DashboardRow;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.DashboardService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure unit tests for Journey B — reads cached data through a fake repository. */
class DashboardServiceTest {

    @Test
    void buildsOneRowPerTrackedTickerInOrder() {
        var repo = new FakeMarketDataRepository();
        repo.saveQuote(new Quote("MSFT", 420.5, 2.5, 0.6, 421, 418, 419,
                418, Instant.parse("2026-01-02T15:00:00Z")));
        repo.saveProfile(new CompanyProfile("MSFT", "Microsoft Corp", "NASDAQ", "USD",
                "Tech", 3_000_000, "", ""));

        var service = new DashboardService(repo, new FakeWatchlist("AAPL", "MSFT"));
        List<DashboardRow> rows = service.getDashboard();

        assertThat(rows).extracting(DashboardRow::ticker).containsExactly("AAPL", "MSFT");

        DashboardRow msft = rows.get(1);
        assertThat(msft.name()).isEqualTo("Microsoft Corp");
        assertThat(msft.currency()).isEqualTo("USD");
        assertThat(msft.price()).isEqualTo(420.5);
        assertThat(msft.hasQuote()).isTrue();
        assertThat(msft.isUp()).isTrue();
        assertThat(msft.asOfDisplay()).isEqualTo("2026-01-02 15:00 UTC");
    }

    @Test
    void tickerWithNoCachedDataStillGetsAPlaceholderRow() {
        var repo = new FakeMarketDataRepository();
        var service = new DashboardService(repo, new FakeWatchlist("AAPL"));

        DashboardRow row = service.getDashboard().get(0);

        assertThat(row.ticker()).isEqualTo("AAPL");
        assertThat(row.name()).isEqualTo("AAPL"); // falls back to the ticker
        assertThat(row.hasQuote()).isFalse();
        assertThat(row.price()).isNull();
        assertThat(row.asOfDisplay()).isEqualTo("—");
    }

    @Test
    void negativeChangeIsNotMarkedUp() {
        var repo = new FakeMarketDataRepository();
        repo.saveQuote(new Quote("AAPL", 180, -1.5, -0.8, 182, 179, 181,
                181.5, Instant.parse("2026-01-02T15:00:00Z")));
        var service = new DashboardService(repo, new FakeWatchlist("AAPL"));

        assertThat(service.getDashboard().get(0).isUp()).isFalse();
    }
}
