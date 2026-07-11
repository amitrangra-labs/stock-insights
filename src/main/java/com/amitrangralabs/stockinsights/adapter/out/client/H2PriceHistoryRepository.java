package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.PricePoint;
import com.amitrangralabs.stockinsights.port.PriceHistoryRepositoryPort;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@link PriceHistoryRepositoryPort} backed by H2 via {@link JdbcClient}.
 *
 * <p>Plain SQL, constructed explicitly in {@code OutboundConfig}. History is upserted per
 * {@code (ticker, trade_date)} so re-fetching overlapping ranges is idempotent and old bars are
 * retained. Reads return bars oldest-first, ready for charting.
 */
public class H2PriceHistoryRepository implements PriceHistoryRepositoryPort {

    private final JdbcClient jdbc;

    public H2PriceHistoryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveHistory(String ticker, List<PricePoint> history) {
        for (PricePoint p : history) {
            jdbc.sql("""
                    MERGE INTO price_history
                        (ticker, trade_date, open_price, high_price, low_price, close_price, volume)
                    KEY (ticker, trade_date)
                    VALUES (:ticker, :date, :open, :high, :low, :close, :volume)
                    """)
                    .param("ticker", ticker)
                    .param("date", p.date())
                    .param("open", p.open())
                    .param("high", p.high())
                    .param("low", p.low())
                    .param("close", p.close())
                    .param("volume", p.volume())
                    .update();
        }
    }

    @Override
    public List<PricePoint> findHistory(String ticker) {
        return jdbc.sql("""
                SELECT trade_date, open_price, high_price, low_price, close_price, volume
                FROM price_history WHERE ticker = :ticker ORDER BY trade_date ASC
                """)
                .param("ticker", ticker)
                .query((rs, rowNum) -> new PricePoint(
                        rs.getObject("trade_date", java.time.LocalDate.class),
                        rs.getDouble("open_price"),
                        rs.getDouble("high_price"),
                        rs.getDouble("low_price"),
                        rs.getDouble("close_price"),
                        rs.getLong("volume")))
                .list();
    }
}
