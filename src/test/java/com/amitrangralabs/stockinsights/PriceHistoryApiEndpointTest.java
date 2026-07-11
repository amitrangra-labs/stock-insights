package com.amitrangralabs.stockinsights;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.adapter.in.endpoint.PriceHistoryApiEndpoint;
import com.amitrangralabs.stockinsights.adapter.in.endpoint.StockDetailEndpoint;
import com.amitrangralabs.stockinsights.domain.FakeMarketDataRepository;
import com.amitrangralabs.stockinsights.domain.FakePriceHistoryRepository;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.domain.service.StockDetailService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for the detail/history endpoints, instantiated directly over fakes. */
class PriceHistoryApiEndpointTest {

    private StockDetailService service() {
        var histRepo = new FakePriceHistoryRepository();
        histRepo.saveHistory("AAPL", List.of(
                new PricePoint(LocalDate.of(2026, 1, 2), 179, 182, 178, 180, 1_200_000)));
        return new StockDetailService(new FakeMarketDataRepository(), histRepo, List.of("AAPL"));
    }

    @Test
    void historyApiReturnsCachedPoints() {
        var endpoint = new PriceHistoryApiEndpoint(service());
        List<PricePoint> points = endpoint.history("AAPL");
        assertThat(points).hasSize(1);
        assertThat(points.get(0).close()).isEqualTo(180);
    }

    @Test
    void historyApiReturnsEmptyForUntracked() {
        var endpoint = new PriceHistoryApiEndpoint(service());
        assertThat(endpoint.history("TSLA")).isEmpty();
    }

    @Test
    void detailPageReturns404ForUntracked() {
        var endpoint = new StockDetailEndpoint(service());
        try {
            endpoint.detail("TSLA", new ConcurrentModel());
            org.junit.jupiter.api.Assertions.fail("expected 404");
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void detailPageRendersForTracked() {
        var endpoint = new StockDetailEndpoint(service());
        var model = new ConcurrentModel();
        String view = endpoint.detail("AAPL", model);
        assertThat(view).isEqualTo("detail");
        assertThat(model.getAttribute("detail")).isNotNull();
    }
}
