package com.amitrangralabs.stockinsights.domain.object;

/**
 * A symbol-search suggestion: a ticker and its company/fund name.
 *
 * @param symbol the ticker symbol, e.g. {@code AAPL}
 * @param name   the company or fund name
 */
public record SymbolMatch(String symbol, String name) {
}
