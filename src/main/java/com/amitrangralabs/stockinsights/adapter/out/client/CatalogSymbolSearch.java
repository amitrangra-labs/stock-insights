package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.SymbolMatch;
import com.amitrangralabs.stockinsights.port.SymbolSearchPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link SymbolSearchPort} backed by a bundled CSV catalog ({@code symbols.csv} on the classpath).
 *
 * <p>Keyless: search works with no API key. The catalog is loaded once at construction into memory
 * (~100 entries), and queries are ranked so the most relevant matches surface first:
 * exact symbol → symbol prefix → name prefix → name-word prefix → symbol contains → name contains.
 *
 * <p>Wired in {@code OutboundConfig}. Swap this implementation to search a broader/provider-backed
 * symbol universe without touching the domain.
 */
public class CatalogSymbolSearch implements SymbolSearchPort {

    private static final String CATALOG = "/symbols.csv";

    private final List<SymbolMatch> catalog;

    public CatalogSymbolSearch() {
        this.catalog = load();
    }

    @Override
    public List<SymbolMatch> search(String query, int limit) {
        String q = query.trim().toUpperCase();
        if (q.isEmpty() || limit <= 0) {
            return List.of();
        }
        return catalog.stream()
                .map(m -> new Scored(m, score(m, q)))
                .filter(s -> s.score >= 0)
                .sorted(Comparator.comparingInt((Scored s) -> s.score)
                        .thenComparing(s -> s.match.symbol()))
                .limit(limit)
                .map(s -> s.match)
                .toList();
    }

    /** Lower is better; -1 means no match. */
    private static int score(SymbolMatch m, String q) {
        String symbol = m.symbol().toUpperCase();
        String name = m.name().toUpperCase();
        if (symbol.equals(q)) {
            return 0;
        }
        if (symbol.startsWith(q)) {
            return 1;
        }
        if (name.startsWith(q)) {
            return 2;
        }
        for (String word : name.split("[^A-Z0-9]+")) {
            if (!word.isEmpty() && word.startsWith(q)) {
                return 3;
            }
        }
        if (symbol.contains(q)) {
            return 4;
        }
        if (name.contains(q)) {
            return 5;
        }
        return -1;
    }

    private static List<SymbolMatch> load() {
        List<SymbolMatch> entries = new ArrayList<>();
        InputStream in = CatalogSymbolSearch.class.getResourceAsStream(CATALOG);
        if (in == null) {
            throw new IllegalStateException("Symbol catalog not found on classpath: " + CATALOG);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int comma = trimmed.indexOf(',');
                if (comma <= 0) {
                    continue;
                }
                String symbol = trimmed.substring(0, comma).trim();
                String name = trimmed.substring(comma + 1).trim();
                if (!symbol.isEmpty()) {
                    entries.add(new SymbolMatch(symbol, name));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read symbol catalog " + CATALOG, e);
        }
        return List.copyOf(entries);
    }

    private record Scored(SymbolMatch match, int score) {
    }
}
