package com.amitrangralabs.stockinsights.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.PriceTick;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies parsing of Finnhub WebSocket trade messages. */
class FinnhubTradeStreamTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesTradeMessageIntoTicks() {
        String msg = "{\"type\":\"trade\",\"data\":["
                + "{\"s\":\"AAPL\",\"p\":190.5,\"t\":1735826400000,\"v\":10},"
                + "{\"s\":\"MSFT\",\"p\":420.25,\"t\":1735826400100,\"v\":5}]}";

        List<PriceTick> ticks = FinnhubTradeStream.parseMessage(msg, mapper);

        assertThat(ticks).extracting(PriceTick::symbol).containsExactly("AAPL", "MSFT");
        assertThat(ticks.get(0).price()).isEqualTo(190.5);
        assertThat(ticks.get(0).at().toEpochMilli()).isEqualTo(1735826400000L);
    }

    @Test
    void ignoresNonTradeMessages() {
        assertThat(FinnhubTradeStream.parseMessage("{\"type\":\"ping\"}", mapper)).isEmpty();
    }

    @Test
    void toleratesGarbage() {
        assertThat(FinnhubTradeStream.parseMessage("not json", mapper)).isEmpty();
    }
}
