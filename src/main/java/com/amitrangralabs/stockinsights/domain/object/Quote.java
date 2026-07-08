package com.amitrangralabs.stockinsights.domain.object;

import java.time.Instant;

/**
 * A point-in-time price quote for a single ticker.
 *
 * <p>Field set mirrors what a typical free quote endpoint returns; volume is intentionally absent
 * because it is not part of a basic quote and will be sourced separately when historical/candle
 * data is added in a later milestone.
 *
 * @param ticker        the stock symbol, e.g. {@code AAPL}
 * @param current       latest traded price
 * @param change        absolute change vs. the previous close
 * @param percentChange change vs. the previous close, as a percentage
 * @param high          day high
 * @param low           day low
 * @param open          day open
 * @param previousClose previous session close
 * @param asOf          when the quote was produced by the source
 */
public record Quote(
        String ticker,
        double current,
        double change,
        double percentChange,
        double high,
        double low,
        double open,
        double previousClose,
        Instant asOf) {
}
