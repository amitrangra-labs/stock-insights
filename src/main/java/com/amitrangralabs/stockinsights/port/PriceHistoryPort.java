package com.amitrangralabs.stockinsights.port;

import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import java.util.List;

/**
 * Outbound port for fetching daily price history from an external provider.
 *
 * <p>Separate from {@link MarketDataPort} because history is a distinct concern with a different
 * shape (a time series) and, in this project, a different provider — quotes/profiles come from
 * Finnhub, history from a keyless source. The concrete implementation ({@code YahooFinanceClient})
 * lives in {@code adapter.out.client} and is wired in {@code OutboundConfig}.
 *
 * @throws MarketDataException if history cannot be retrieved
 */
public interface PriceHistoryPort {

    /** Fetch recent daily bars for a ticker, oldest first. */
    List<PricePoint> fetchDailyHistory(String ticker);
}
