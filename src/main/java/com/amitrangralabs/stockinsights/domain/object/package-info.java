/**
 * Domain objects: immutable value types passed across the app.
 *
 * <p>Plain Java records with no framework annotations. Planned types: {@code Quote},
 * {@code CompanyProfile}, {@code Financials}, {@code EarningsEstimate}, {@code NewsItem},
 * {@code AnalystRating}. Outbound adapters map external JSON into these; domain services aggregate
 * them into view models for the inbound adapters.
 */
package com.amitrangralabs.stockinsights.domain.object;
