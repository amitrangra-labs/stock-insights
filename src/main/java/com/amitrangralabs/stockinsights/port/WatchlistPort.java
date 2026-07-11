package com.amitrangralabs.stockinsights.port;

import java.util.List;

/**
 * Outbound port for the persisted watchlist — the set of tickers the app tracks.
 *
 * <p>Replaces the previously static {@code stock-insights.tracked-tickers} config as the source of
 * truth, so tickers can be added/removed at runtime from the UI. Seeded from config on first start.
 * Implemented by {@code H2WatchlistRepository} and wired in {@code OutboundConfig}.
 *
 * <p>Tickers are stored canonicalised (uppercase); callers pass canonical symbols.
 */
public interface WatchlistPort {

    /** All tracked tickers, in insertion order. */
    List<String> list();

    /** Add a ticker; returns {@code true} if it was newly added, {@code false} if already present. */
    boolean add(String ticker);

    /** Remove a ticker; returns {@code true} if it was present. */
    boolean remove(String ticker);

    /** Whether the ticker is currently tracked. */
    boolean contains(String ticker);
}
