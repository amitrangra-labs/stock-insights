package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@link MarketDataRepositoryPort} backed by H2 via Spring's {@link JdbcClient}.
 *
 * <p>Deliberately uses plain SQL rather than Spring Data JPA repositories: every query is visible
 * here and the bean is constructed explicitly in {@code OutboundConfig}, matching this project's
 * "no hidden proxies / explicit wiring" rule. The schema lives in {@code schema.sql}.
 *
 * <p>Caching is "latest wins": each save is an upsert keyed by ticker (H2 {@code MERGE}).
 * {@code as_of} is stored as epoch millis to sidestep timezone mapping quirks.
 */
public class H2RepositoryClient implements MarketDataRepositoryPort {

    private final JdbcClient jdbc;

    public H2RepositoryClient(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveQuote(Quote q) {
        jdbc.sql("""
                MERGE INTO quotes
                    (ticker, current_price, price_change, percent_change,
                     day_high, day_low, day_open, previous_close, as_of)
                KEY (ticker)
                VALUES
                    (:ticker, :current, :change, :percentChange,
                     :high, :low, :open, :previousClose, :asOf)
                """)
                .param("ticker", q.ticker())
                .param("current", q.current())
                .param("change", q.change())
                .param("percentChange", q.percentChange())
                .param("high", q.high())
                .param("low", q.low())
                .param("open", q.open())
                .param("previousClose", q.previousClose())
                .param("asOf", q.asOf().toEpochMilli())
                .update();
    }

    @Override
    public void saveProfile(CompanyProfile p) {
        jdbc.sql("""
                MERGE INTO company_profiles
                    (ticker, name, exchange, currency, industry, market_cap, logo_url, web_url)
                KEY (ticker)
                VALUES
                    (:ticker, :name, :exchange, :currency, :industry, :marketCap, :logoUrl, :webUrl)
                """)
                .param("ticker", p.ticker())
                .param("name", p.name())
                .param("exchange", p.exchange())
                .param("currency", p.currency())
                .param("industry", p.industry())
                .param("marketCap", p.marketCap())
                .param("logoUrl", p.logoUrl())
                .param("webUrl", p.webUrl())
                .update();
    }

    @Override
    public Optional<Quote> findLatestQuote(String ticker) {
        return jdbc.sql("""
                SELECT ticker, current_price, price_change, percent_change,
                       day_high, day_low, day_open, previous_close, as_of
                FROM quotes WHERE ticker = :ticker
                """)
                .param("ticker", ticker)
                .query((rs, rowNum) -> new Quote(
                        rs.getString("ticker"),
                        rs.getDouble("current_price"),
                        rs.getDouble("price_change"),
                        rs.getDouble("percent_change"),
                        rs.getDouble("day_high"),
                        rs.getDouble("day_low"),
                        rs.getDouble("day_open"),
                        rs.getDouble("previous_close"),
                        Instant.ofEpochMilli(rs.getLong("as_of"))))
                .optional();
    }

    @Override
    public Optional<CompanyProfile> findProfile(String ticker) {
        return jdbc.sql("""
                SELECT ticker, name, exchange, currency, industry, market_cap, logo_url, web_url
                FROM company_profiles WHERE ticker = :ticker
                """)
                .param("ticker", ticker)
                .query((rs, rowNum) -> new CompanyProfile(
                        rs.getString("ticker"),
                        rs.getString("name"),
                        rs.getString("exchange"),
                        rs.getString("currency"),
                        rs.getString("industry"),
                        rs.getDouble("market_cap"),
                        rs.getString("logo_url"),
                        rs.getString("web_url")))
                .optional();
    }
}
