package com.amitrangralabs.stockinsights.port;

import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import java.util.List;

/**
 * Outbound port for the local cache of price history.
 *
 * <p>The background refresh writes through this port; the detail page and history API read through
 * it, so page loads never hit the external provider. Implemented by {@code H2PriceHistoryRepository}
 * and wired in {@code OutboundConfig}.
 */
public interface PriceHistoryRepositoryPort {

    /** Insert or update the given daily bars for a ticker (upsert per date). */
    void saveHistory(String ticker, List<PricePoint> history);

    /** Cached daily bars for a ticker, oldest first (empty if none). */
    List<PricePoint> findHistory(String ticker);
}
