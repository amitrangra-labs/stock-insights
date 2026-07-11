package com.amitrangralabs.stockinsights.domain;

import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link MarketDataRepositoryPort} for domain unit tests — no Spring, no DB. */
public class FakeMarketDataRepository implements MarketDataRepositoryPort {

    public final Map<String, Quote> quotes = new HashMap<>();
    public final Map<String, CompanyProfile> profiles = new HashMap<>();
    public final Map<String, List<NewsItem>> news = new HashMap<>();
    public final Map<String, AnalystRating> ratings = new HashMap<>();
    public final Map<String, Fundamentals> fundamentals = new HashMap<>();

    @Override
    public void saveQuote(Quote quote) {
        quotes.put(quote.ticker(), quote);
    }

    @Override
    public void saveProfile(CompanyProfile profile) {
        profiles.put(profile.ticker(), profile);
    }

    @Override
    public void saveNews(String ticker, List<NewsItem> items) {
        news.put(ticker, List.copyOf(items));
    }

    @Override
    public void saveRating(String ticker, AnalystRating rating) {
        ratings.put(ticker, rating);
    }

    @Override
    public void saveFundamentals(Fundamentals f) {
        fundamentals.put(f.ticker(), f);
    }

    @Override
    public Optional<Quote> findLatestQuote(String ticker) {
        return Optional.ofNullable(quotes.get(ticker));
    }

    @Override
    public Optional<CompanyProfile> findProfile(String ticker) {
        return Optional.ofNullable(profiles.get(ticker));
    }

    @Override
    public List<NewsItem> findNews(String ticker) {
        return news.getOrDefault(ticker, List.of());
    }

    @Override
    public Optional<AnalystRating> findRating(String ticker) {
        return Optional.ofNullable(ratings.get(ticker));
    }

    @Override
    public Optional<Fundamentals> findFundamentals(String ticker) {
        return Optional.ofNullable(fundamentals.get(ticker));
    }
}
