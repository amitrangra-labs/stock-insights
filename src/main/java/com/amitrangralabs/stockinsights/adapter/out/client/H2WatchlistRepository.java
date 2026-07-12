package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@link WatchlistPort} backed by H2 via {@link JdbcClient}.
 *
 * <p>Plain SQL, constructed explicitly in {@code OutboundConfig}. Insertion order is preserved via
 * an {@code added_at} epoch-millis column so the dashboard lists tickers in the order they were
 * added (and seeded).
 */
public class H2WatchlistRepository implements WatchlistPort {

    private final JdbcClient jdbc;

    public H2WatchlistRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> list() {
        return jdbc.sql("SELECT ticker FROM watchlist ORDER BY added_at ASC, ticker ASC")
                .query(String.class)
                .list();
    }

    @Override
    public boolean add(String ticker) {
        if (contains(ticker)) {
            return false;
        }
        // Monotonic sequence = strict insertion order, even for a fast batch (e.g. seeding),
        // where wall-clock millis would tie. contains() above guards against duplicates.
        jdbc.sql("""
                INSERT INTO watchlist (ticker, added_at)
                SELECT :ticker, COALESCE(MAX(added_at), 0) + 1 FROM watchlist
                """)
                .param("ticker", ticker)
                .update();
        return true;
    }

    @Override
    public boolean remove(String ticker) {
        int rows = jdbc.sql("DELETE FROM watchlist WHERE ticker = :ticker")
                .param("ticker", ticker)
                .update();
        return rows > 0;
    }

    @Override
    public void clear() {
        jdbc.sql("DELETE FROM watchlist").update();
    }

    @Override
    public boolean contains(String ticker) {
        return jdbc.sql("SELECT COUNT(*) FROM watchlist WHERE ticker = :ticker")
                .param("ticker", ticker)
                .query(Integer.class)
                .single() > 0;
    }
}
