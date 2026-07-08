/**
 * Outbound adapters: concrete clients that implement the {@code port} interfaces.
 *
 * <p>These classes talk to external market-data APIs and the database. They may depend directly on
 * {@code domain.object} types (to build and return them) and implement {@code port} interfaces.
 * They carry no stereotype annotations — they are constructed and exposed as port beans in
 * {@code OutboundConfig}.
 *
 * <p>Planned clients: {@code FinnhubClient} / {@code AlphaVantageClient} (implement
 * {@code MarketDataPort}) and {@code H2RepositoryClient} (implements
 * {@code MarketDataRepositoryPort}).
 */
package com.amitrangralabs.stockinsights.adapter.out.client;
