package com.amitrangralabs.stockinsights;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.adapter.in.endpoint.DashboardEndpoint;
import com.amitrangralabs.stockinsights.domain.FakeMarketDataRepository;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.DashboardRow;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.DashboardService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * Unit test for the endpoint's controller logic — instantiated directly with a real
 * {@link DashboardService} over a fake repository, no Spring context needed.
 */
class DashboardEndpointTest {

    @Test
    void returnsDashboardViewWithRowsInModel() {
        var repo = new FakeMarketDataRepository();
        repo.saveQuote(new Quote("AAPL", 180, 1, 0.5, 181, 179, 180,
                179, Instant.parse("2026-01-02T15:00:00Z")));
        repo.saveProfile(new CompanyProfile("AAPL", "Apple Inc", "NASDAQ", "USD",
                "Tech", 3_000_000, "", ""));
        var endpoint = new DashboardEndpoint(new DashboardService(repo, List.of("AAPL")));

        Model model = new ConcurrentModel();
        String view = endpoint.dashboard(model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.getAttribute("rows")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<DashboardRow> rows = (List<DashboardRow>) model.getAttribute("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).name()).isEqualTo("Apple Inc");
    }
}
