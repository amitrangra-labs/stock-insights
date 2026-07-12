package com.amitrangralabs.stockinsights.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.amitrangralabs.stockinsights.adapter.out.client.CatalogSymbolSearch;
import com.amitrangralabs.stockinsights.domain.object.SymbolMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies catalog loading, ranking, and matching against the bundled symbols.csv. */
class CatalogSymbolSearchTest {

    private final CatalogSymbolSearch search = new CatalogSymbolSearch();

    @Test
    void exactSymbolMatchRanksFirst() {
        List<SymbolMatch> results = search.search("AAPL", 8);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).symbol()).isEqualTo("AAPL");
    }

    @Test
    void isCaseInsensitiveAndMatchesByName() {
        // "apple" matches by name; the Apple ticker should surface.
        assertThat(search.search("apple", 8))
                .extracting(SymbolMatch::symbol)
                .contains("AAPL");
    }

    @Test
    void symbolPrefixRanksAboveNameContains() {
        // "V" is the Visa symbol (exact) and also a prefix of many names/symbols.
        List<SymbolMatch> results = search.search("V", 8);
        assertThat(results.get(0).symbol()).isEqualTo("V"); // exact symbol wins
    }

    @Test
    void nameKeywordMatches() {
        // "gold" appears in "SPDR Gold Shares" (GLD).
        assertThat(search.search("gold", 8))
                .extracting(SymbolMatch::symbol)
                .contains("GLD");
    }

    @Test
    void respectsLimit() {
        assertThat(search.search("a", 3)).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void blankQueryReturnsEmpty() {
        assertThat(search.search("   ", 8)).isEmpty();
    }

    @Test
    void unknownQueryReturnsEmpty() {
        assertThat(search.search("ZZZZZZ", 8)).isEmpty();
    }
}
