package com.amitrangralabs.stockinsights.domain.service;

import com.amitrangralabs.stockinsights.domain.object.SymbolMatch;
import com.amitrangralabs.stockinsights.port.SymbolSearchPort;
import java.util.List;

/**
 * Backs the add-ticker autocomplete: normalises the query and caps the number of suggestions.
 *
 * <p>Plain Java, framework-free. Delegates matching/ranking to the {@link SymbolSearchPort}.
 */
public class SymbolSearchService {

    private static final int MAX_RESULTS = 8;

    private final SymbolSearchPort symbolSearch;

    public SymbolSearchService(SymbolSearchPort symbolSearch) {
        this.symbolSearch = symbolSearch;
    }

    public List<SymbolMatch> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return symbolSearch.search(query, MAX_RESULTS);
    }
}
