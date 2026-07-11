package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Manages the watchlist: normalises and validates ticker symbols, then adds/removes them.
 *
 * <p>Plain Java, framework-free. Symbols are canonicalised to uppercase and validated against a
 * conservative pattern (letters/digits, plus {@code . -} as used by some symbols) so junk input
 * never reaches the data providers.
 */
public class WatchlistService {

    private static final Pattern VALID = Pattern.compile("^[A-Z][A-Z0-9.\\-]{0,15}$");

    private final WatchlistPort watchlist;

    public WatchlistService(WatchlistPort watchlist) {
        this.watchlist = watchlist;
    }

    public List<String> tickers() {
        return watchlist.list();
    }

    public boolean isTracked(String ticker) {
        String c = canonical(ticker);
        return c != null && watchlist.contains(c);
    }

    /**
     * Add a ticker after normalising/validating it.
     *
     * @return the canonical symbol if it is valid (whether or not it was already present);
     *     {@link Optional#empty()} if the input is not a valid symbol
     */
    public Optional<String> add(String rawTicker) {
        String c = canonical(rawTicker);
        if (c == null) {
            return Optional.empty();
        }
        watchlist.add(c);
        return Optional.of(c);
    }

    /** Remove a ticker (no-op if absent or invalid). */
    public void remove(String rawTicker) {
        String c = canonical(rawTicker);
        if (c != null) {
            watchlist.remove(c);
        }
    }

    /** Uppercase, trimmed symbol if valid; otherwise {@code null}. */
    private static String canonical(String ticker) {
        if (ticker == null) {
            return null;
        }
        String c = ticker.trim().toUpperCase();
        return VALID.matcher(c).matches() ? c : null;
    }
}
