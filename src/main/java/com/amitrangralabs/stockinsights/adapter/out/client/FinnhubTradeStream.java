package com.amitrangralabs.stockinsights.adapter.out.client;

import com.amitrangralabs.stockinsights.domain.object.PriceTick;
import com.amitrangralabs.stockinsights.domain.service.PriceStreamService;
import com.amitrangralabs.stockinsights.port.WatchlistPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streams real-time trades from Finnhub's WebSocket and publishes them to {@link PriceStreamService}.
 *
 * <p>Uses the JDK's {@link WebSocket} client (no extra dependency). Subscribes to the current
 * watchlist symbols on connect, parses {@code trade} messages into {@link PriceTick}s, and
 * reconnects with a fixed backoff on failure. Does nothing when no API key is configured — the app
 * still works (the dashboard falls back to periodic cache snapshots).
 *
 * <p>Wired in {@code OutboundConfig}; {@link #start()} is called on app startup and {@link #close()}
 * on shutdown.
 */
public class FinnhubTradeStream implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FinnhubTradeStream.class);
    private static final long RECONNECT_DELAY_SECONDS = 5;

    private final String wsUrl;
    private final String apiKey;
    private final WatchlistPort watchlist;
    private final PriceStreamService priceStream;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "finnhub-ws");
                t.setDaemon(true);
                return t;
            });
    private volatile WebSocket webSocket;

    public FinnhubTradeStream(
            String wsUrl,
            String apiKey,
            WatchlistPort watchlist,
            PriceStreamService priceStream,
            ObjectMapper objectMapper) {
        this.wsUrl = wsUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.watchlist = watchlist;
        this.priceStream = priceStream;
        this.objectMapper = objectMapper;
    }

    /** Begin connecting (no-op if already running or no API key). */
    public void start() {
        if (apiKey.isBlank()) {
            log.info("No FINNHUB_API_KEY: real-time trade stream disabled (dashboard uses cache snapshots).");
            return;
        }
        if (running.compareAndSet(false, true)) {
            connect();
        }
    }

    private void connect() {
        if (!running.get()) {
            return;
        }
        try {
            HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl + "?token=" + apiKey), new Listener())
                    .whenComplete((ws, error) -> {
                        if (error != null) {
                            log.warn("Finnhub WebSocket connect failed: {}", error.getMessage());
                            scheduleReconnect();
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Finnhub WebSocket connect error: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (running.get()) {
            scheduler.schedule(this::connect, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public void close() {
        running.set(false);
        WebSocket ws = this.webSocket;
        if (ws != null) {
            ws.abort();
        }
        scheduler.shutdownNow();
    }

    /**
     * Parse a Finnhub WebSocket message into price ticks. Non-trade messages (e.g. {@code ping})
     * yield an empty list. Package-visible for testing.
     */
    static List<PriceTick> parseMessage(String message, ObjectMapper mapper) {
        List<PriceTick> ticks = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(message);
            if (!"trade".equals(root.path("type").asText())) {
                return ticks;
            }
            for (JsonNode trade : root.path("data")) {
                String symbol = trade.path("s").asText(null);
                if (symbol == null || symbol.isBlank()) {
                    continue;
                }
                double price = trade.path("p").asDouble();
                long epochMs = trade.path("t").asLong(System.currentTimeMillis());
                ticks.add(PriceTick.trade(symbol, price, Instant.ofEpochMilli(epochMs)));
            }
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.debug("Ignoring unparseable stream message: {}", e.getMessage());
        }
        return ticks;
    }

    /** JDK WebSocket listener: subscribes on open, forwards trades, reconnects on close/error. */
    private final class Listener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            webSocket = ws;
            List<String> symbols = watchlist.list();
            log.info("Finnhub WebSocket connected; subscribing to {} symbol(s)", symbols.size());
            for (String symbol : symbols) {
                ws.sendText("{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}", true);
            }
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                for (PriceTick tick : parseMessage(message, objectMapper)) {
                    priceStream.publish(tick);
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.info("Finnhub WebSocket closed ({}): {}", statusCode, reason);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.warn("Finnhub WebSocket error: {}", error.getMessage());
            scheduleReconnect();
        }
    }
}
