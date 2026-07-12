package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.domain.object.PriceTick;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-memory hub for live price updates: the fan-out point between the market-data stream (WebSocket
 * trades and periodic cache snapshots publish here) and connected browsers (SSE endpoints subscribe
 * here). Plain Java, framework-free and thread-safe.
 *
 * <p>Publishing de-duplicates: a tick whose price is unchanged from the last published price for
 * that symbol is dropped, so browsers only get (and flash on) real changes.
 */
public class PriceStreamService {

    private final Map<String, PriceTick> latest = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<PriceTick>> listeners = new CopyOnWriteArrayList<>();

    /** Register a listener; returns a handle that removes it when closed. */
    public AutoCloseable subscribe(Consumer<PriceTick> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** Publish a tick to all listeners, unless the price is unchanged since the last one. */
    public void publish(PriceTick tick) {
        PriceTick previous = latest.get(tick.symbol());
        if (previous != null && previous.price() == tick.price()) {
            return;
        }
        latest.put(tick.symbol(), tick);
        for (Consumer<PriceTick> listener : listeners) {
            try {
                listener.accept(tick);
            } catch (RuntimeException ignored) {
                // a failing listener (e.g. a disconnected SSE client) must not affect the others
            }
        }
    }

    /** The most recent tick per symbol, for sending an initial snapshot to a new subscriber. */
    public Collection<PriceTick> latestTicks() {
        return latest.values();
    }

    int listenerCount() {
        return listeners.size();
    }
}
