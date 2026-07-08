package com.amitrangralabs.stockinsights.domain;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link MarketDataRepositoryPort} for domain unit tests — no Spring, no DB. */
public class FakeMarketDataRepository implements MarketDataRepositoryPort {

    public final Map<String, Quote> quotes = new HashMap<>();
    public final Map<String, CompanyProfile> profiles = new HashMap<>();

    @Override
    public void saveQuote(Quote quote) {
        quotes.put(quote.ticker(), quote);
    }

    @Override
    public void saveProfile(CompanyProfile profile) {
        profiles.put(profile.ticker(), profile);
    }

    @Override
    public Optional<Quote> findLatestQuote(String ticker) {
        return Optional.ofNullable(quotes.get(ticker));
    }

    @Override
    public Optional<CompanyProfile> findProfile(String ticker) {
        return Optional.ofNullable(profiles.get(ticker));
    }
}
