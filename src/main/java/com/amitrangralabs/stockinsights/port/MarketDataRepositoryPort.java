package com.amitrangralabs.stockinsights.port;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for the local cache of market data.
 *
 * <p>The background refresh writes through this port; the dashboard and detail pages read through
 * it — so page loads never touch the external provider directly. The concrete implementation
 * ({@code H2RepositoryClient}) lives in {@code adapter.out.client} and is wired in
 * {@code OutboundConfig}.
 *
 * <p>Quotes and profiles are cached "latest wins" (one row per ticker); saving again replaces the
 * previous value.
 */
public interface MarketDataRepositoryPort {

    /** Insert or update the latest quote for its ticker. */
    void saveQuote(Quote quote);

    /** Insert or update the profile for its ticker. */
    void saveProfile(CompanyProfile profile);

    /** The latest cached quote for a ticker, if any. */
    Optional<Quote> findLatestQuote(String ticker);

    /** The cached profile for a ticker, if any. */
    Optional<CompanyProfile> findProfile(String ticker);
}
