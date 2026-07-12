package com.amitrangralabs.stockinsights;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.adapter.in.endpoint.LiveSnapshotScheduler;
import com.amitrangralabs.stockinsights.domain.object.PriceTick;
import com.amitrangralabs.stockinsights.domain.object.Quote;
import com.amitrangralabs.stockinsights.domain.service.PriceStreamService;
import com.amitrangralabs.stockinsights.port.MarketDataRepositoryPort;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end (server side): a cached quote broadcast by {@link LiveSnapshotScheduler} reaches a
 * subscriber of {@link PriceStreamService}. Proves the cache -&gt; hub -&gt; SSE path without a key
 * or the WebSocket.
 */
@SpringBootTest
class LiveSnapshotSchedulerTest {

    @Autowired
    private LiveSnapshotScheduler scheduler;

    @Autowired
    private MarketDataRepositoryPort repository;

    @Autowired
    private WatchlistPort watchlist;

    @Autowired
    private PriceStreamService priceStream;

    @Test
    void broadcastsCachedQuoteToSubscribers() {
        watchlist.add("ZLIV");
        repository.saveQuote(new Quote("ZLIV", 123.45, 1.0, 0.8, 124, 122, 123, 122.45,
                Instant.parse("2026-06-30T20:00:00Z")));

        List<PriceTick> received = new ArrayList<>();
        priceStream.subscribe(received::add);

        scheduler.broadcast();

        assertThat(received)
                .anySatisfy(t -> {
                    assertThat(t.symbol()).isEqualTo("ZLIV");
                    assertThat(t.price()).isEqualTo(123.45);
                    assertThat(t.percentChange()).isEqualTo(0.8);
                });
    }
}
