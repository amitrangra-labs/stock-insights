package com.amitrangralabs.stockinsights.domain.object;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Sell-side analyst recommendation counts for one period (the latest available).
 *
 * @param period     the month the recommendations are for
 * @param strongBuy  number of "strong buy" ratings
 * @param buy        number of "buy" ratings
 * @param hold       number of "hold" ratings
 * @param sell       number of "sell" ratings
 * @param strongSell number of "strong sell" ratings
 */
public record AnalystRating(
        LocalDate period,
        int strongBuy,
        int buy,
        int hold,
        int sell,
        int strongSell) {

    public int total() {
        return strongBuy + buy + hold + sell + strongSell;
    }

    /** Percentage (0–100) of a segment within the total, for rendering the bar. */
    public double percent(int count) {
        int t = total();
        return t == 0 ? 0.0 : (count * 100.0) / t;
    }

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("MMM yyyy");

    /** The period formatted for display, or empty if unknown. */
    public String periodDisplay() {
        return period == null ? "" : PERIOD.format(period);
    }
}
