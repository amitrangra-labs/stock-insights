package com.amitrangralabs.stockinsights.domain.object;

/**
 * A small set of key fundamental metrics for a ticker.
 *
 * <p>All metric fields are nullable: a provider may not supply every metric for every symbol, and
 * the view renders a dash for missing ones.
 *
 * @param ticker        the stock symbol
 * @param peRatio       trailing P/E ratio
 * @param eps           trailing earnings per share
 * @param high52Week    52-week high price
 * @param low52Week     52-week low price
 * @param dividendYield indicated annual dividend yield (percent)
 * @param beta          beta vs. the market
 */
public record Fundamentals(
        String ticker,
        Double peRatio,
        Double eps,
        Double high52Week,
        Double low52Week,
        Double dividendYield,
        Double beta) {
}
