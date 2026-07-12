package com.amitrangralabs.stockinsights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.domain.object.SymbolMatch;
import com.amitrangralabs.stockinsights.domain.service.SymbolSearchService;
import com.amitrangralabs.stockinsights.port.SymbolSearchPort;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for query normalisation / result capping (matching is delegated to the port). */
class SymbolSearchServiceTest {

    @Test
    void blankOrNullQueryReturnsEmptyWithoutCallingThePort() {
        var service = new SymbolSearchService((q, limit) -> {
            throw new AssertionError("port should not be called for blank query");
        });
        assertThat(service.search(null)).isEmpty();
        assertThat(service.search("   ")).isEmpty();
    }

    @Test
    void delegatesToPortWithACappedLimit() {
        int[] capturedLimit = new int[1];
        SymbolSearchPort port = (q, limit) -> {
            capturedLimit[0] = limit;
            return List.of(new SymbolMatch("AAPL", "Apple Inc."));
        };
        var service = new SymbolSearchService(port);

        List<SymbolMatch> results = service.search("aapl");

        assertThat(results).extracting(SymbolMatch::symbol).containsExactly("AAPL");
        assertThat(capturedLimit[0]).isPositive();
    }
}
