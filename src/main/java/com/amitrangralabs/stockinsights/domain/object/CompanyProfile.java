package com.amitrangralabs.stockinsights.domain.object;

/**
 * Static-ish descriptive information about a company behind a ticker.
 *
 * @param ticker    the stock symbol
 * @param name      company name
 * @param exchange  listing exchange
 * @param currency  trading currency
 * @param industry  industry classification
 * @param marketCap market capitalisation (in the source's units, typically millions)
 * @param logoUrl   URL to a logo image, may be blank
 * @param webUrl    company website, may be blank
 */
public record CompanyProfile(
        String ticker,
        String name,
        String exchange,
        String currency,
        String industry,
        double marketCap,
        String logoUrl,
        String webUrl) {
}
