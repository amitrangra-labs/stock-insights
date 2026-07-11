package com.amitrangralabs.stockinsights.port;

import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import java.util.List;

/**
 * Outbound port for reaching an external market-data provider.
 *
 * <p>The domain depends only on this interface; concrete implementations (e.g. {@code FinnhubClient})
 * live in {@code adapter.out.client} and are wired in {@code OutboundConfig}. Swapping or adding a
 * provider means adding a new implementation — no domain change.
 *
 * <p>Implementations throw {@link MarketDataException} when a fetch fails for any reason.
 */
public interface MarketDataPort {

    /**
     * Fetch the latest quote for a ticker.
     *
     * @throws MarketDataException if the quote cannot be retrieved
     */
    Quote fetchQuote(String ticker);

    /**
     * Fetch the company profile for a ticker.
     *
     * @throws MarketDataException if the profile cannot be retrieved
     */
    CompanyProfile fetchProfile(String ticker);

    /**
     * Fetch recent company news for a ticker (most recent first).
     *
     * @throws MarketDataException if news cannot be retrieved
     */
    List<NewsItem> fetchNews(String ticker);

    /**
     * Fetch the latest analyst recommendation counts for a ticker.
     *
     * @throws MarketDataException if ratings cannot be retrieved
     */
    AnalystRating fetchRatings(String ticker);

    /**
     * Fetch key fundamental metrics for a ticker.
     *
     * @throws MarketDataException if fundamentals cannot be retrieved
     */
    Fundamentals fetchFundamentals(String ticker);
}
