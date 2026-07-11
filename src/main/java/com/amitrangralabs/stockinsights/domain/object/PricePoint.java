package com.amitrangralabs.stockinsights.domain.object;

import java.time.LocalDate;

/**
 * One daily OHLC bar for a ticker's price history.
 *
 * @param date   trading day
 * @param open   opening price
 * @param high   day high
 * @param low    day low
 * @param close  closing price (what the chart plots)
 * @param volume shares traded
 */
public record PricePoint(
        LocalDate date,
        double open,
        double high,
        double low,
        double close,
        long volume) {
}
