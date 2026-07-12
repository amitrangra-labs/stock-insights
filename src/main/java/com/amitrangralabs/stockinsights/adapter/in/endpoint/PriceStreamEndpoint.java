package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.object.PriceTick;
import com.amitrangralabs.stockinsights.domain.service.PriceStreamService;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events endpoint that streams live price ticks to the browser: {@code GET
 * /api/stream/prices}. Each connected client subscribes to {@link PriceStreamService}; ticks are
 * forwarded as {@code tick} events (JSON). On connect the current snapshot is replayed so the page
 * fills in immediately. Stereotype-free; wired in {@code InboundConfig}.
 */
@RequestMapping
public class PriceStreamEndpoint {

    private final PriceStreamService priceStream;

    public PriceStreamEndpoint(PriceStreamService priceStream) {
        this.priceStream = priceStream;
    }

    @GetMapping(value = "/api/stream/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L); // no server-side timeout; client also auto-reconnects

        Consumer<PriceTick> listener = tick -> {
            try {
                emitter.send(SseEmitter.event().name("tick").data(tick, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        };
        AutoCloseable handle = priceStream.subscribe(listener);
        Runnable unsubscribe = () -> {
            try {
                handle.close();
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        };
        emitter.onCompletion(unsubscribe);
        emitter.onError(e -> unsubscribe.run());
        emitter.onTimeout(() -> {
            unsubscribe.run();
            emitter.complete();
        });

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
            for (PriceTick tick : priceStream.latestTicks()) {
                emitter.send(SseEmitter.event().name("tick").data(tick, MediaType.APPLICATION_JSON));
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
