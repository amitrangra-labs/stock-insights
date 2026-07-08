package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataException;
import com.amitrangralabs.stockinsights.port.MarketDataPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
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
}
