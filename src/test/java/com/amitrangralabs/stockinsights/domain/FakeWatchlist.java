package com.amitrangralabs.stockinsights.domain;

import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.util.ArrayList;
import java.util.List;

/** In-memory {@link WatchlistPort} for unit tests, preserving insertion order. */
public class FakeWatchlist implements WatchlistPort {

    private final List<String> tickers = new ArrayList<>();

    public FakeWatchlist(String... seed) {
        for (String t : seed) {
            tickers.add(t);
        }
    }

    @Override
    public List<String> list() {
        return List.copyOf(tickers);
    }

    @Override
    public boolean add(String ticker) {
        if (tickers.contains(ticker)) {
            return false;
        }
        return tickers.add(ticker);
    }

    @Override
    public boolean remove(String ticker) {
        return tickers.remove(ticker);
    }

    @Override
    public void clear() {
        tickers.clear();
    }

    @Override
    public boolean contains(String ticker) {
        return tickers.contains(ticker);
    }
}
