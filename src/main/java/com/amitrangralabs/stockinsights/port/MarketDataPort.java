package com.amitrangralabs.stockinsights.port;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;

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
}
