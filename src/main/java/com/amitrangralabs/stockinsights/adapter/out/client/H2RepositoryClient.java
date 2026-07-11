package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.AnalystRating;
import com.amitrangralabs.stockinsights.domain.object.CompanyProfile;
import com.amitrangralabs.stockinsights.domain.object.Fundamentals;
import com.amitrangralabs.stockinsights.domain.object.NewsItem;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    public void saveNews(String ticker, List<NewsItem> news) {
        // Replace the ticker's news so the cache reflects the latest fetch (bounded size).
        jdbc.sql("DELETE FROM news WHERE ticker = :ticker").param("ticker", ticker).update();
        for (NewsItem n : news) {
            jdbc.sql("""
                    INSERT INTO news
                        (ticker, id, headline, source, url, summary, published_at, image_url)
                    VALUES (:ticker, :id, :headline, :source, :url, :summary, :publishedAt, :imageUrl)
                    """)
                    .param("ticker", ticker)
                    .param("id", n.id())
                    .param("headline", n.headline())
                    .param("source", n.source())
                    .param("url", n.url())
                    .param("summary", n.summary())
                    .param("publishedAt", n.publishedAt().toEpochMilli())
                    .param("imageUrl", n.imageUrl())
                    .update();
        }
    }

    @Override
    public void saveRating(String ticker, AnalystRating r) {
        jdbc.sql("""
                MERGE INTO analyst_ratings
                    (ticker, period, strong_buy, buy, hold, sell, strong_sell)
                KEY (ticker)
                VALUES (:ticker, :period, :strongBuy, :buy, :hold, :sell, :strongSell)
                """)
                .param("ticker", ticker)
                .param("period", r.period())
                .param("strongBuy", r.strongBuy())
                .param("buy", r.buy())
                .param("hold", r.hold())
                .param("sell", r.sell())
                .param("strongSell", r.strongSell())
                .update();
    }

    @Override
    public void saveFundamentals(Fundamentals f) {
        jdbc.sql("""
                MERGE INTO fundamentals
                    (ticker, pe_ratio, eps, high_52w, low_52w, dividend_yield, beta)
                KEY (ticker)
                VALUES (:ticker, :pe, :eps, :high, :low, :dividendYield, :beta)
                """)
                .param("ticker", f.ticker())
                .param("pe", f.peRatio())
                .param("eps", f.eps())
                .param("high", f.high52Week())
                .param("low", f.low52Week())
                .param("dividendYield", f.dividendYield())
                .param("beta", f.beta())
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

    @Override
    public List<NewsItem> findNews(String ticker) {
        return jdbc.sql("""
                SELECT id, headline, source, url, summary, published_at, image_url
                FROM news WHERE ticker = :ticker ORDER BY published_at DESC
                """)
                .param("ticker", ticker)
                .query((rs, rowNum) -> new NewsItem(
                        rs.getLong("id"),
                        rs.getString("headline"),
                        rs.getString("source"),
                        rs.getString("url"),
                        rs.getString("summary"),
                        Instant.ofEpochMilli(rs.getLong("published_at")),
                        rs.getString("image_url")))
                .list();
    }

    @Override
    public Optional<AnalystRating> findRating(String ticker) {
        return jdbc.sql("""
                SELECT period, strong_buy, buy, hold, sell, strong_sell
                FROM analyst_ratings WHERE ticker = :ticker
                """)
                .param("ticker", ticker)
                .query((rs, rowNum) -> new AnalystRating(
                        rs.getObject("period", LocalDate.class),
                        rs.getInt("strong_buy"),
                        rs.getInt("buy"),
                        rs.getInt("hold"),
                        rs.getInt("sell"),
                        rs.getInt("strong_sell")))
                .optional();
    }

    @Override
    public Optional<Fundamentals> findFundamentals(String ticker) {
        return jdbc.sql("""
                SELECT ticker, pe_ratio, eps, high_52w, low_52w, dividend_yield, beta
                FROM fundamentals WHERE ticker = :ticker
                """)
                .param("ticker", ticker)
                .query((rs, rowNum) -> new Fundamentals(
                        rs.getString("ticker"),
                        (Double) rs.getObject("pe_ratio"),
                        (Double) rs.getObject("eps"),
                        (Double) rs.getObject("high_52w"),
                        (Double) rs.getObject("low_52w"),
                        (Double) rs.getObject("dividend_yield"),
                        (Double) rs.getObject("beta")))
                .optional();
    }
}
