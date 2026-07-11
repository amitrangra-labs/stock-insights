package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataException;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link MarketDataPort} backed by the Finnhub REST API (free tier).
 *
 * <p>Constructed in {@code OutboundConfig} with a pre-configured {@link RestClient} (base URL set)
 * and the API key. Maps Finnhub's terse JSON into domain objects. Any transport/HTTP error, a
 * missing key, or an empty/unknown-symbol response is surfaced as {@link MarketDataException} so the
 * refresh service can skip that ticker.
 *
 * @see <a href="https://finnhub.io/docs/api/quote">Finnhub quote</a>
 * @see <a href="https://finnhub.io/docs/api/company-profile2">Finnhub company profile</a>
 */
public class FinnhubClient implements MarketDataPort {

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public Quote fetchQuote(String ticker) {
        requireApiKey();
        QuoteResponse r = get("/quote", ticker, QuoteResponse.class);
        // Finnhub returns all-zero fields for an unknown symbol.
        if (r == null || (r.current() == 0 && r.previousClose() == 0)) {
            throw new MarketDataException("No quote data for " + ticker);
        }
        Instant asOf = r.timestamp() > 0 ? Instant.ofEpochSecond(r.timestamp()) : Instant.now();
        return new Quote(
                ticker,
                r.current(),
                r.change(),
                r.percentChange(),
                r.high(),
                r.low(),
                r.open(),
                r.previousClose(),
                asOf);
    }

    @Override
    public CompanyProfile fetchProfile(String ticker) {
        requireApiKey();
        ProfileResponse r = get("/stock/profile2", ticker, ProfileResponse.class);
        if (r == null || r.name() == null || r.name().isBlank()) {
            throw new MarketDataException("No profile data for " + ticker);
        }
        return new CompanyProfile(
                ticker,
                r.name(),
                nullToEmpty(r.exchange()),
                nullToEmpty(r.currency()),
                nullToEmpty(r.industry()),
                r.marketCap(),
                nullToEmpty(r.logo()),
                nullToEmpty(r.webUrl()));
    }

    private static final int NEWS_LOOKBACK_DAYS = 14;
    private static final int NEWS_LIMIT = 12;

    @Override
    public List<NewsItem> fetchNews(String ticker) {
        requireApiKey();
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(NEWS_LOOKBACK_DAYS);
        NewsResponse[] items;
        try {
            items = restClient
                    .get()
                    .uri(b -> b.path("/company-news")
                            .queryParam("symbol", ticker)
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(NewsResponse[].class);
        } catch (RestClientException e) {
            throw new MarketDataException("Finnhub news request failed for " + ticker + ": " + e.getMessage(), e);
        }
        if (items == null) {
            return List.of();
        }
        return Arrays.stream(items)
                .filter(n -> n.headline() != null && !n.headline().isBlank())
                .sorted(Comparator.comparingLong(NewsResponse::datetime).reversed())
                .limit(NEWS_LIMIT)
                .map(n -> new NewsItem(
                        n.id(),
                        n.headline(),
                        nullToEmpty(n.source()),
                        nullToEmpty(n.url()),
                        nullToEmpty(n.summary()),
                        Instant.ofEpochSecond(n.datetime() > 0 ? n.datetime() : 0),
                        nullToEmpty(n.image())))
                .toList();
    }

    @Override
    public AnalystRating fetchRatings(String ticker) {
        requireApiKey();
        RecommendationResponse[] recs = get("/stock/recommendation", ticker, RecommendationResponse[].class);
        if (recs == null || recs.length == 0) {
            throw new MarketDataException("No analyst ratings for " + ticker);
        }
        RecommendationResponse r = recs[0]; // Finnhub returns most-recent period first
        LocalDate period = (r.period() != null && !r.period().isBlank()) ? LocalDate.parse(r.period()) : null;
        return new AnalystRating(period, r.strongBuy(), r.buy(), r.hold(), r.sell(), r.strongSell());
    }

    @Override
    public Fundamentals fetchFundamentals(String ticker) {
        requireApiKey();
        MetricResponse resp;
        try {
            resp = restClient
                    .get()
                    .uri(b -> b.path("/stock/metric")
                            .queryParam("symbol", ticker)
                            .queryParam("metric", "all")
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(MetricResponse.class);
        } catch (RestClientException e) {
            throw new MarketDataException("Finnhub metric request failed for " + ticker + ": " + e.getMessage(), e);
        }
        if (resp == null || resp.metric() == null) {
            throw new MarketDataException("No fundamentals for " + ticker);
        }
        Map<String, Object> m = resp.metric();
        return new Fundamentals(
                ticker,
                asDouble(m.get("peTTM")),
                asDouble(m.get("epsTTM")),
                asDouble(m.get("52WeekHigh")),
                asDouble(m.get("52WeekLow")),
                asDouble(m.get("dividendYieldIndicatedAnnual")),
                asDouble(m.get("beta")));
    }

    private static Double asDouble(Object v) {
        return (v instanceof Number number) ? number.doubleValue() : null;
    }

    private <T> T get(String path, String ticker, Class<T> type) {
        try {
            return restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("symbol", ticker)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(type);
        } catch (RestClientException e) {
            throw new MarketDataException(
                    "Finnhub request failed for " + ticker + " (" + path + "): " + e.getMessage(), e);
        }
    }

    private void requireApiKey() {
        if (apiKey.isBlank()) {
            throw new MarketDataException(
                    "FINNHUB_API_KEY is not configured; set it to enable market-data fetching.");
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // --- Finnhub JSON payloads (single-letter keys mapped explicitly) ---

    private record QuoteResponse(
            @JsonProperty("c") double current,
            @JsonProperty("d") double change,
            @JsonProperty("dp") double percentChange,
            @JsonProperty("h") double high,
            @JsonProperty("l") double low,
            @JsonProperty("o") double open,
            @JsonProperty("pc") double previousClose,
            @JsonProperty("t") long timestamp) {
    }

    private record ProfileResponse(
            @JsonProperty("name") String name,
            @JsonProperty("exchange") String exchange,
            @JsonProperty("currency") String currency,
            @JsonProperty("finnhubIndustry") String industry,
            @JsonProperty("marketCapitalization") double marketCap,
            @JsonProperty("logo") String logo,
            @JsonProperty("weburl") String webUrl) {
    }

    private record NewsResponse(
            @JsonProperty("id") long id,
            @JsonProperty("headline") String headline,
            @JsonProperty("source") String source,
            @JsonProperty("url") String url,
            @JsonProperty("summary") String summary,
            @JsonProperty("datetime") long datetime,
            @JsonProperty("image") String image) {
    }

    private record RecommendationResponse(
            @JsonProperty("period") String period,
            @JsonProperty("strongBuy") int strongBuy,
            @JsonProperty("buy") int buy,
            @JsonProperty("hold") int hold,
            @JsonProperty("sell") int sell,
            @JsonProperty("strongSell") int strongSell) {
    }

    private record MetricResponse(@JsonProperty("metric") Map<String, Object> metric) {
    }
}
