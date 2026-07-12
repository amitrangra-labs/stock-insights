package com.amitrangralabs.stockinsights.domain.object;

import java.time.Instant;

/**
 * A single live price update for a ticker, pushed to browsers over SSE.
 *
 * <p>{@code change}/{@code percentChange} are present for cache "snapshot" ticks (which carry the
 * full quote) and {@code null} for raw trade ticks from the WebSocket (which carry only a price).
 *
 * @param symbol        the ticker
 * @param price         latest price
 * @param change        absolute day change, or {@code null}
 * @param percentChange day change percent, or {@code null}
 * @param at            when the update occurred
 */
public record PriceTick(
        String symbol, double price, Double change, Double percentChange, Instant at) {

    /** A price-only tick (e.g. a raw trade), with no change fields. */
    public static PriceTick trade(String symbol, double price, Instant at) {
        return new PriceTick(symbol, price, null, null, at);
    }
}
