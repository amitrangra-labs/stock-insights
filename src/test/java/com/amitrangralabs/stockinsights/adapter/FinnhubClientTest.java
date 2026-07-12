package com.amitrangralabs.stockinsights.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.amitrangralabs.stockinsights.adapter.out.client.FinnhubClient;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Verifies {@link FinnhubClient} maps Finnhub's terse JSON into domain objects, using a mocked
 * HTTP layer — no network, no API key needed.
 */
class FinnhubClientTest {

    private FinnhubClient clientBoundTo(MockRestServiceServer[] serverHolder) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://finnhub.test");
        serverHolder[0] = MockRestServiceServer.bindTo(builder).build();
        return new FinnhubClient(builder.build(), "test-key", 0); // no throttle in tests
    }

    @Test
    void mapsQuoteJson() {
        var holder = new MockRestServiceServer[1];
        FinnhubClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/quote?symbol=AAPL&token=test-key")))
                .andRespond(withSuccess(
                        "{\"c\":190.5,\"d\":2.5,\"dp\":1.33,\"h\":191,\"l\":188,\"o\":189,\"pc\":188.0,\"t\":1767366000}",
                        MediaType.APPLICATION_JSON));

        Quote quote = client.fetchQuote("AAPL");

        assertThat(quote.ticker()).isEqualTo("AAPL");
        assertThat(quote.current()).isEqualTo(190.5);
        assertThat(quote.percentChange()).isEqualTo(1.33);
        assertThat(quote.previousClose()).isEqualTo(188.0);
        assertThat(quote.asOf().getEpochSecond()).isEqualTo(1767366000L);
        holder[0].verify();
    }

    @Test
    void mapsProfileJson() {
        var holder = new MockRestServiceServer[1];
        FinnhubClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/stock/profile2?symbol=AAPL&token=test-key")))
                .andRespond(withSuccess(
                        "{\"name\":\"Apple Inc\",\"exchange\":\"NASDAQ\",\"currency\":\"USD\","
                                + "\"finnhubIndustry\":\"Technology\",\"marketCapitalization\":3000000,"
                                + "\"logo\":\"https://logo\",\"weburl\":\"https://apple.com\"}",
                        MediaType.APPLICATION_JSON));

        CompanyProfile profile = client.fetchProfile("AAPL");

        assertThat(profile.name()).isEqualTo("Apple Inc");
        assertThat(profile.industry()).isEqualTo("Technology");
        assertThat(profile.marketCap()).isEqualTo(3_000_000);
        assertThat(profile.webUrl()).isEqualTo("https://apple.com");
        holder[0].verify();
    }

    @Test
    void allZeroQuoteMeansUnknownSymbol() {
        var holder = new MockRestServiceServer[1];
        FinnhubClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/quote")))
                .andRespond(withSuccess(
                        "{\"c\":0,\"d\":0,\"dp\":0,\"h\":0,\"l\":0,\"o\":0,\"pc\":0,\"t\":0}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchQuote("NOPE"))
                .isInstanceOf(MarketDataException.class);
    }

    @Test
    void blankApiKeyFailsFastWithoutCallingTheNetwork() {
        FinnhubClient client = new FinnhubClient(RestClient.builder().build(), "  ", 0);

        assertThatThrownBy(() -> client.fetchQuote("AAPL"))
                .isInstanceOf(MarketDataException.class)
                .hasMessageContaining("FINNHUB_API_KEY");
    }

    @Test
    void mapsNewsSortedNewestFirstSkippingBlankHeadlines() {
        var holder = new MockRestServiceServer[1];
        FinnhubClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/company-news?symbol=AAPL")))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"headline\":\"Older\",\"source\":\"Reuters\",\"url\":\"u1\","
                                + "\"summary\":\"s1\",\"datetime\":1700000000,\"image\":\"i1\"},"
                                + "{\"id\":2,\"headline\":\"Newer\",\"source\":\"WSJ\",\"url\":\"u2\","
                                + "\"summary\":\"s2\",\"datetime\":1800000000,\"image\":\"\"},"
                                + "{\"id\":3,\"headline\":\"\",\"source\":\"X\",\"url\":\"u3\","
                                + "\"summary\":\"\",\"datetime\":1900000000,\"image\":\"\"}]",
                        MediaType.APPLICATION_JSON));

        var news = client.fetchNews("AAPL");

        assertThat(news).hasSize(2); // blank headline dropped
        assertThat(news.get(0).headline()).isEqualTo("Newer"); // newest first
        assertThat(news.get(1).headline()).isEqualTo("Older");
        assertThat(news.get(0).source()).isEqualTo("WSJ");
    }

    @Test
    void mapsLatestAnalystRating() {
        var holder = new MockRestServiceServer[1];
        FinnhubClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/stock/recommendation?symbol=AAPL")))
                .andRespond(withSuccess(
                        "[{\"period\":\"2026-06-01\",\"strongBuy\":10,\"buy\":15,\"hold\":5,\"sell\":2,\"strongSell\":1},"
                                + "{\"period\":\"2026-05-01\",\"strongBuy\":8,\"buy\":12,\"hold\":6,\"sell\":3,\"strongSell\":1}]",
                        MediaType.APPLICATION_JSON));

        var rating = client.fetchRatings("AAPL");

        assertThat(rating.period().toString()).isEqualTo("2026-06-01"); // most-recent element
        assertThat(rating.strongBuy()).isEqualTo(10);
        assertThat(rating.total()).isEqualTo(33);
    }

    @Test
    void mapsFundamentalsAndToleratesMissingMetrics() {
        var holder = new MockRestServiceServer[1];
        FinnhubClient client = clientBoundTo(holder);
        holder[0].expect(requestTo(Matchers.containsString("/stock/metric?symbol=AAPL&metric=all")))
                .andRespond(withSuccess(
                        "{\"metric\":{\"peTTM\":28.5,\"epsTTM\":6.2,\"52WeekHigh\":320.1,"
                                + "\"52WeekLow\":210.0,\"beta\":1.2}}", // dividendYield absent
                        MediaType.APPLICATION_JSON));

        var f = client.fetchFundamentals("AAPL");

        assertThat(f.peRatio()).isEqualTo(28.5);
        assertThat(f.high52Week()).isEqualTo(320.1);
        assertThat(f.beta()).isEqualTo(1.2);
        assertThat(f.dividendYield()).isNull(); // missing metric -> null, not a crash
    }
}
