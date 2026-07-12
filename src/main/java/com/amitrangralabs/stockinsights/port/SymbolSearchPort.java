package com.amitrangralabs.stockinsights.port;

import com.amitrangralabs.stockinsights.domain.object.SymbolMatch;
import java.util.List;

/**
 * Outbound port for searching known ticker symbols (for add-ticker autocomplete).
 *
 * <p>The default implementation ({@code CatalogSymbolSearch}) searches a bundled catalog, so search
 * works with no API key. It could be swapped for a provider-backed symbol search behind this port.
 */
public interface SymbolSearchPort {

    /** Best matches for the query (symbol/name), most relevant first, capped at {@code limit}. */
    List<SymbolMatch> search(String query, int limit);
}
