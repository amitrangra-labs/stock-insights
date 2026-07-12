package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.object.PriceTick;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.PriceStreamService;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically broadcasts the latest cached quote for each tracked ticker onto the
 * {@link PriceStreamService}. This keeps the dashboard auto-updating from the cache even without the
 * real-time WebSocket (e.g. no API key), and de-duplication in the hub means unchanged prices don't
 * cause spurious flashes. Wired in {@code InboundConfig}.
 */
public class LiveSnapshotScheduler {

    private final MarketDataRepositoryPort repository;
    private final WatchlistPort watchlist;
    private final PriceStreamService priceStream;

    public LiveSnapshotScheduler(
            MarketDataRepositoryPort repository,
            WatchlistPort watchlist,
            PriceStreamService priceStream) {
        this.repository = repository;
        this.watchlist = watchlist;
        this.priceStream = priceStream;
    }

    @Scheduled(
            initialDelayString = "${stock-insights.snapshot-broadcast-initial-delay-ms:15000}",
            fixedDelayString = "${stock-insights.snapshot-broadcast-interval-ms:12000}")
    public void broadcast() {
        for (String ticker : watchlist.list()) {
            Optional<Quote> quote = repository.findLatestQuote(ticker);
            if (quote.isPresent()) {
                Quote q = quote.get();
                priceStream.publish(new PriceTick(
                        ticker, q.current(), q.change(), q.percentChange(), q.asOf()));
            }
        }
    }
}
