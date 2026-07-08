package com.amitrangralabs.stockinsights.port;

/**
 * Thrown by a {@link MarketDataPort} implementation when data cannot be fetched — the provider is
 * unreachable, rate-limited, misconfigured (e.g. missing API key), or returned nothing usable for
 * the requested ticker.
 *
 * <p>Unchecked so the port signatures stay clean; callers (domain services) catch it per-ticker so
 * one failing symbol never aborts a whole refresh cycle.
 */
public class MarketDataException extends RuntimeException {

    public MarketDataException(String message) {
        super(message);
    }

    public MarketDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
