package com.amitrangralabs.stockinsights.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.amitrangralabs.stockinsights.adapter.out.client.YahooFinanceClient;
import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.port.MarketDataException;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Verifies {@link YahooFinanceClient} maps Yahoo's parallel-array JSON, with a mocked HTTP layer. */
class YahooFinanceClientTest {

    // Two trading days; the middle "null close" slot must be skipped.
    private static final String BODY = """
            {"chart":{"result":[{
              "meta":{"currency":"USD","symbol":"AAPL"},
              "timestamp":[1735826400,1735912800,1735999200],
              "indicators":{"quote":[{
                "open":[186.0,null,188.0],
                "high":[188.0,null,190.0],
                "low":[185.0,null,187.5],
                "close":[187.0,null,189.5],
                "volume":[1000000,null,1200000]
              }]}
            }],"error":null}}
            """;

    private YahooFinanceClient clientBoundTo(MockRestServiceServer[] holder) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://yahoo.test");
        holder[0] = MockRestServiceServer.bindTo(builder).build();
        return new YahooFinanceClient(builder.build(), "3mo", "1d");
    }

    @Test
    void mapsHistoryAndSkipsNullCloses() {
        var holder = new MockRestServiceServer[1];
        YahooFinanceClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/v8/finance/chart/AAPL")))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        List<PricePoint> points = client.fetchDailyHistory("AAPL");

        assertThat(points).hasSize(2); // null-close slot dropped
        assertThat(points.get(0).close()).isEqualTo(187.0);
        assertThat(points.get(0).volume()).isEqualTo(1_000_000L);
        assertThat(points.get(1).close()).isEqualTo(189.5);
        // oldest-first ordering preserved
        assertThat(points.get(0).date()).isBefore(points.get(1).date());
        holder[0].verify();
    }

    @Test
    void emptyResultThrows() {
        var holder = new MockRestServiceServer[1];
        YahooFinanceClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/v8/finance/chart/NOPE")))
                .andRespond(withSuccess("{\"chart\":{\"result\":[],\"error\":null}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchDailyHistory("NOPE"))
                .isInstanceOf(MarketDataException.class);
    }
}
