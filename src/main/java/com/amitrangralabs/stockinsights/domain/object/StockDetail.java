package com.amitrangralabs.stockinsights.domain.object;

import java.util.List;

/**
 * View model for the stock detail page: a tracked ticker's cached profile, latest quote, and
 * recent price history, assembled by {@code StockDetailService}.
 *
 * <p>{@code profile} and {@code quote} may be {@code null} (nothing cached yet); {@code history}
 * is never null but may be empty. The template renders placeholders accordingly.
 *
 * @param ticker  the stock symbol (always present)
 * @param profile cached company profile, or {@code null}
 * @param quote   latest cached quote, or {@code null}
 * @param history recent daily bars, oldest first (possibly empty)
 */
public record StockDetail(
        String ticker,
        CompanyProfile profile,
        Quote quote,
        List<PricePoint> history) {

    public StockDetail {
        history = history == null ? List.of() : List.copyOf(history);
    }

    public boolean hasProfile() {
        return profile != null;
    }

    public boolean hasQuote() {
        return quote != null;
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    /** Display name: the profile name if cached, otherwise the ticker itself. */
    public String displayName() {
        return (profile != null && !profile.name().isBlank()) ? profile.name() : ticker;
    }
}
