package com.amitrangralabs.stockinsights.domain;

import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory {@link PriceHistoryRepositoryPort} for domain unit tests. */
public class FakePriceHistoryRepository implements PriceHistoryRepositoryPort {

    public final Map<String, List<PricePoint>> history = new HashMap<>();

    @Override
    public void saveHistory(String ticker, List<PricePoint> points) {
        history.put(ticker, List.copyOf(points));
    }

    @Override
    public List<PricePoint> findHistory(String ticker) {
        return history.getOrDefault(ticker, List.of());
    }
}
