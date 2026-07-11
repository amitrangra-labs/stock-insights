package com.amitrangralabs.stockinsights.domain.object;

import java.util.List;

/**
 * View model for the stock detail page: a tracked ticker's cached profile, latest quote, recent
 * price history, fundamentals, analyst rating, and news, assembled by {@code StockDetailService}.
 *
 * <p>{@code profile}, {@code quote}, {@code fundamentals}, and {@code rating} may be {@code null}
 * (nothing cached yet); {@code history} and {@code news} are never null but may be empty. The
 * template renders placeholders accordingly.
 *
 * @param ticker       the stock symbol (always present)
 * @param profile      cached company profile, or {@code null}
 * @param quote        latest cached quote, or {@code null}
 * @param history      recent daily bars, oldest first (possibly empty)
 * @param fundamentals cached fundamental metrics, or {@code null}
 * @param rating       latest analyst rating, or {@code null}
 * @param news         recent news, most recent first (possibly empty)
 */
public record StockDetail(
        String ticker,
        CompanyProfile profile,
        Quote quote,
        List<PricePoint> history,
        Fundamentals fundamentals,
        AnalystRating rating,
        List<NewsItem> news) {

    public StockDetail {
        history = history == null ? List.of() : List.copyOf(history);
        news = news == null ? List.of() : List.copyOf(news);
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

    public boolean hasFundamentals() {
        return fundamentals != null;
    }

    public boolean hasRating() {
        return rating != null && rating.total() > 0;
    }

    public boolean hasNews() {
        return !news.isEmpty();
    }

    /** Display name: the profile name if cached, otherwise the ticker itself. */
    public String displayName() {
        return (profile != null && !profile.name().isBlank()) ? profile.name() : ticker;
    }
}
