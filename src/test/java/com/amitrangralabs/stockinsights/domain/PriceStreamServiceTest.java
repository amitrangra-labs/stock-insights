package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.PriceTick;
import com.amitrangralabs.stockinsights.domain.service.PriceStreamService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the price-stream fan-out hub. */
class PriceStreamServiceTest {

    private static PriceTick tick(String symbol, double price) {
        return PriceTick.trade(symbol, price, Instant.parse("2026-01-02T15:00:00Z"));
    }

    @Test
    void publishesToAllSubscribers() {
        var hub = new PriceStreamService();
        List<PriceTick> a = new ArrayList<>();
        List<PriceTick> b = new ArrayList<>();
        hub.subscribe(a::add);
        hub.subscribe(b::add);

        hub.publish(tick("AAPL", 190));

        assertThat(a).hasSize(1);
        assertThat(b).hasSize(1);
        assertThat(a.get(0).symbol()).isEqualTo("AAPL");
    }

    @Test
    void deduplicatesUnchangedPrices() {
        var hub = new PriceStreamService();
        List<PriceTick> got = new ArrayList<>();
        hub.subscribe(got::add);

        hub.publish(tick("AAPL", 190));
        hub.publish(tick("AAPL", 190)); // unchanged -> dropped
        hub.publish(tick("AAPL", 191)); // changed -> delivered

        assertThat(got).extracting(PriceTick::price).containsExactly(190.0, 191.0);
    }

    @Test
    void unsubscribeStopsDelivery() throws Exception {
        var hub = new PriceStreamService();
        List<PriceTick> got = new ArrayList<>();
        AutoCloseable handle = hub.subscribe(got::add);

        hub.publish(tick("AAPL", 1));
        handle.close();
        hub.publish(tick("AAPL", 2));

        assertThat(got).hasSize(1);
    }

    @Test
    void aFailingListenerDoesNotBreakOthers() {
        var hub = new PriceStreamService();
        List<PriceTick> good = new ArrayList<>();
        hub.subscribe(t -> { throw new RuntimeException("boom"); });
        hub.subscribe(good::add);

        hub.publish(tick("AAPL", 5));

        assertThat(good).hasSize(1);
    }

    @Test
    void latestTicksReturnsMostRecentPerSymbol() {
        var hub = new PriceStreamService();
        hub.publish(tick("AAPL", 1));
        hub.publish(tick("AAPL", 2));
        hub.publish(tick("MSFT", 9));

        assertThat(hub.latestTicks())
                .extracting(PriceTick::symbol, PriceTick::price)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("AAPL", 2.0),
                        org.assertj.core.groups.Tuple.tuple("MSFT", 9.0));
    }
}
