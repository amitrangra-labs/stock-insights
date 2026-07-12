package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.object.SymbolMatch;
import com.amitrangralabs.stockinsights.domain.service.SymbolSearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * JSON API backing the add-ticker autocomplete: {@code GET /api/symbols?q=...}.
 *
 * <p>Stereotype-free; wired in {@code InboundConfig}. Returns an empty array for a blank query.
 */
@RequestMapping
public class SymbolSearchEndpoint {

    private final SymbolSearchService symbolSearchService;

    public SymbolSearchEndpoint(SymbolSearchService symbolSearchService) {
        this.symbolSearchService = symbolSearchService;
    }

    @GetMapping("/api/symbols")
    @ResponseBody
    public List<SymbolMatch> search(@RequestParam(value = "q", required = false) String q) {
        return symbolSearchService.search(q);
    }
}
