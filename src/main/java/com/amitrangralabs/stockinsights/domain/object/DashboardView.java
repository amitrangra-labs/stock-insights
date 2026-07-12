package com.amitrangralabs.stockinsights.domain.object;

import java.util.List;

/**
 * The full dashboard view model: the per-ticker rows plus a breadth {@link MarketSummary}.
 *
 * @param rows    one row per tracked ticker, in watchlist order
 * @param summary breadth over the rows that have quotes
 */
public record DashboardView(List<DashboardRow> rows, MarketSummary summary) {

    public DashboardView {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
