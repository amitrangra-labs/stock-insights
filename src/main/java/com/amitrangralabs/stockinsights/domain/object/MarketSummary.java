package com.amitrangralabs.stockinsights.domain.object;

import java.util.List;

/**
 * A breadth summary of the dashboard: how many tracked tickers are up/down/flat, plus the biggest
 * mover in each direction. Derived from the {@link DashboardRow}s that have a cached quote.
 *
 * @param up            number of quoted tickers with a positive day change
 * @param down          number of quoted tickers with a negative day change
 * @param unchanged     number of quoted tickers with no change
 * @param quoted        number of tickers that have a cached quote at all
 * @param topGainer     ticker with the largest positive % change, or {@code null}
 * @param topGainerPct  that ticker's % change, or {@code null}
 * @param topLoser      ticker with the largest negative % change, or {@code null}
 * @param topLoserPct   that ticker's % change, or {@code null}
 */
public record MarketSummary(
        int up,
        int down,
        int unchanged,
        int quoted,
        String topGainer,
        Double topGainerPct,
        String topLoser,
        Double topLoserPct) {

    public static MarketSummary from(List<DashboardRow> rows) {
        int up = 0;
        int down = 0;
        int unchanged = 0;
        int quoted = 0;
        DashboardRow gainer = null;
        DashboardRow loser = null;

        for (DashboardRow row : rows) {
            if (!row.hasQuote() || row.percentChange() == null) {
                continue;
            }
            quoted++;
            double pct = row.percentChange();
            if (pct > 0) {
                up++;
            } else if (pct < 0) {
                down++;
            } else {
                unchanged++;
            }
            if (gainer == null || pct > gainer.percentChange()) {
                gainer = row;
            }
            if (loser == null || pct < loser.percentChange()) {
                loser = row;
            }
        }

        return new MarketSummary(
                up, down, unchanged, quoted,
                gainer == null ? null : gainer.ticker(),
                gainer == null ? null : gainer.percentChange(),
                loser == null ? null : loser.ticker(),
                loser == null ? null : loser.percentChange());
    }

    public boolean hasData() {
        return quoted > 0;
    }
}
