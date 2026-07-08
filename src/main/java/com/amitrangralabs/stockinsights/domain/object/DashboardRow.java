package com.amitrangralabs.stockinsights.domain.object;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * One row of the dashboard: a tracked ticker joined with its latest cached quote.
 *
 * <p>Every tracked ticker gets a row even if no quote has been cached yet (e.g. before the first
 * refresh, or when the data provider is unavailable). In that case the numeric fields and
 * {@code asOf} are {@code null} and the view renders a placeholder — so the dashboard always lists
 * the full watchlist.
 *
 * @param ticker        the stock symbol (always present)
 * @param name          company name, or the ticker itself if no profile is cached
 * @param currency      trading currency, may be {@code null}
 * @param price         latest price, or {@code null} if no quote cached
 * @param change        absolute day change, or {@code null}
 * @param percentChange day change percent, or {@code null}
 * @param asOf          when the cached quote was produced, or {@code null}
 */
public record DashboardRow(
        String ticker,
        String name,
        String currency,
        Double price,
        Double change,
        Double percentChange,
        Instant asOf) {

    /** True when a quote has been cached for this ticker. */
    public boolean hasQuote() {
        return price != null;
    }

    /** True when the latest change is flat or positive (used by the view for colour). */
    public boolean isUp() {
        return change != null && change >= 0;
    }

    private static final DateTimeFormatter AS_OF_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    /** The cache timestamp formatted for display, or a dash when no quote is cached. */
    public String asOfDisplay() {
        return asOf == null ? "—" : AS_OF_FORMAT.format(asOf);
    }
}
