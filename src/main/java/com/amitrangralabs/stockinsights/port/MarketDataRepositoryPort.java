package com.amitrangralabs.stockinsights.port;

import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
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

    /** Replace the cached news for a ticker with the given items. */
    void saveNews(String ticker, List<NewsItem> news);

    /** Insert or update the latest analyst rating for a ticker. */
    void saveRating(String ticker, AnalystRating rating);

    /** Insert or update the fundamentals for its ticker. */
    void saveFundamentals(Fundamentals fundamentals);

    /** The latest cached quote for a ticker, if any. */
    Optional<Quote> findLatestQuote(String ticker);

    /** The cached profile for a ticker, if any. */
    Optional<CompanyProfile> findProfile(String ticker);

    /** Cached news for a ticker, most recent first (empty if none). */
    List<NewsItem> findNews(String ticker);

    /** The cached analyst rating for a ticker, if any. */
    Optional<AnalystRating> findRating(String ticker);

    /** The cached fundamentals for a ticker, if any. */
    Optional<Fundamentals> findFundamentals(String ticker);
}
