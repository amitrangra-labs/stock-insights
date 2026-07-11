package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.port.MarketDataException;
import com.amitrangralabs.stockinsights.port.PriceHistoryPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link PriceHistoryPort} backed by Yahoo Finance's keyless chart endpoint
 * ({@code /v8/finance/chart/{symbol}}). No API key required, which keeps history working
 * out-of-the-box.
 *
 * <p>Constructed in {@code OutboundConfig} with a {@link RestClient} whose base URL and
 * {@code User-Agent} are set, plus the {@code range}/{@code interval} query values. Yahoo returns
 * parallel arrays (timestamps + OHLC); this maps them into {@link PricePoint}s, skipping any bar
 * whose close is null (a gap/holiday).
 *
 * <p>This is an <em>unofficial</em> endpoint; behind the {@link PriceHistoryPort} it can be swapped
 * for a licensed provider without touching the domain.
 */
public class YahooFinanceClient implements PriceHistoryPort {

    // US markets; daily bar timestamps are resolved to a calendar date in this zone.
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

    private final RestClient restClient;
    private final String range;
    private final String interval;

    public YahooFinanceClient(RestClient restClient, String range, String interval) {
        this.restClient = restClient;
        this.range = range;
        this.interval = interval;
    }

    @Override
    public List<PricePoint> fetchDailyHistory(String ticker) {
        ChartResponse response;
        try {
            response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v8/finance/chart/{symbol}")
                            .queryParam("range", range)
                            .queryParam("interval", interval)
                            .build(ticker))
                    .retrieve()
                    .body(ChartResponse.class);
        } catch (RestClientException e) {
            throw new MarketDataException(
                    "Yahoo history request failed for " + ticker + ": " + e.getMessage(), e);
        }

        Result result = firstResult(response, ticker);
        List<Long> timestamps = result.timestamp();
        Quote quote = firstQuote(result, ticker);

        List<PricePoint> points = new ArrayList<>(timestamps.size());
        for (int i = 0; i < timestamps.size(); i++) {
            Double close = at(quote.close(), i);
            if (close == null) {
                continue; // gap / non-trading slot
            }
            LocalDate date = Instant.ofEpochSecond(timestamps.get(i)).atZone(MARKET_ZONE).toLocalDate();
            points.add(new PricePoint(
                    date,
                    orElse(at(quote.open(), i), close),
                    orElse(at(quote.high(), i), close),
                    orElse(at(quote.low(), i), close),
                    close,
                    orElseLong(at(quote.volume(), i))));
        }
        if (points.isEmpty()) {
            throw new MarketDataException("No history data for " + ticker);
        }
        return points;
    }

    private static Result firstResult(ChartResponse response, String ticker) {
        if (response == null || response.chart() == null
                || response.chart().result() == null || response.chart().result().isEmpty()) {
            throw new MarketDataException("No history data for " + ticker);
        }
        Result result = response.chart().result().get(0);
        if (result.timestamp() == null || result.timestamp().isEmpty()) {
            throw new MarketDataException("No history data for " + ticker);
        }
        return result;
    }

    private static Quote firstQuote(Result result, String ticker) {
        if (result.indicators() == null || result.indicators().quote() == null
                || result.indicators().quote().isEmpty()) {
            throw new MarketDataException("No history data for " + ticker);
        }
        return result.indicators().quote().get(0);
    }

    private static <T> T at(List<T> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }

    private static double orElse(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private static long orElseLong(Long value) {
        return value == null ? 0L : value;
    }

    // --- Yahoo chart JSON (unknown fields ignored via Boot's Jackson config) ---

    private record ChartResponse(@JsonProperty("chart") Chart chart) {
    }

    private record Chart(@JsonProperty("result") List<Result> result) {
    }

    private record Result(
            @JsonProperty("timestamp") List<Long> timestamp,
            @JsonProperty("indicators") Indicators indicators) {
    }

    private record Indicators(@JsonProperty("quote") List<Quote> quote) {
    }

    private record Quote(
            @JsonProperty("open") List<Double> open,
            @JsonProperty("high") List<Double> high,
            @JsonProperty("low") List<Double> low,
            @JsonProperty("close") List<Double> close,
            @JsonProperty("volume") List<Long> volume) {
    }
}
