# Stock Insights — SLOs & Observability

This document defines the Service Level Objectives (SLOs) for Stock Insights and the
observability stack that measures them. It is written the way an SRE / production-engineering
team would run this service: explicit SLIs, error budgets, burn-rate alerting, and dashboards.

## 1. Serving model & why it has two reliability planes

Stock Insights deliberately decouples **serving** from **data acquisition**:

```
  Finnhub / Yahoo ──(background scheduler, throttled)──▶  H2 cache  ──(read only)──▶  HTTP responses
        upstream plane                                    freshness plane              serving plane
```

Because every user request reads from the local H2 cache and **never** calls an upstream API
inline, request latency is independent of upstream health. This gives us three distinct planes
to reason about, each with its own SLIs:

| Plane        | What can go wrong                              | Owned by us? |
|--------------|------------------------------------------------|--------------|
| **Serving**  | slow/failed page or API responses, SSE drops   | Yes — hard SLO |
| **Freshness**| cache goes stale (refresh falling behind)      | Yes — hard SLO |
| **Upstream** | Finnhub 429s / Yahoo outages                   | No — measured, feeds freshness |

A staff-level point worth making explicit in interviews: **upstream degradation must not consume
the serving error budget.** A Finnhub outage should show up as a *freshness* SLO burn (stale data,
placeholders) while the serving plane stays green. The architecture makes that separation real.

## 2. Service Level Indicators (SLIs)

All SLIs are defined as `good events / valid events` over a rolling window.

### Serving plane

| SLI | Definition | Metric source |
|-----|-----------|---------------|
| **Availability** | non-5xx responses ÷ all responses for user-facing routes (`/dashboard`, `/stocks/**`, `/api/stocks/**/history`) | `http_server_requests_seconds_count{status,uri}` |
| **Latency** | fraction of requests served under target (p95 / p99) for the same routes | `http_server_requests_seconds_bucket` (histogram) |
| **SSE liveness** | successful SSE connection opens ÷ attempts; and stream uptime | custom counter `sse_connections_total{outcome}` + gauge `sse_active_connections` |
| **Health** | `/health` returns `UP` | probe + `application_ready_time_seconds` |

### Freshness plane

| SLI | Definition | Metric source |
|-----|-----------|---------------|
| **Quote freshness** | fraction of tracked tickers whose cached live-tier data (quote/news) is younger than `2 × live-refresh-interval` **during market hours** | custom gauge `cache_entry_age_seconds{tier="live",ticker}` |
| **Reference freshness** | fraction of tracked tickers whose reference data (profile, fundamentals, ratings, history) is younger than 48h | `cache_entry_age_seconds{tier="reference",ticker}` |
| **Refresh success** | successful refresh cycles ÷ scheduled cycles | custom counter `refresh_cycles_total{tier,outcome}` |

### Upstream plane (measured, not SLO'd)

`upstream_calls_total{provider,endpoint,outcome}` and `upstream_call_duration_seconds` — outcome
in `{success, rate_limited(429), error, throttled_skip}`. Used to explain freshness burns and to
tune the throttle, not to gate releases.

## 3. SLO targets & error budgets

Window: **rolling 30 days**. (Personal-project scale — adjust for real traffic.)

| SLO | Target | 30-day error budget |
|-----|--------|---------------------|
| Serving availability | **99.5%** | 3h 39m of failed requests |
| Serving latency — pages | **95%** of `/dashboard` + `/stocks/**` < **300ms** (server-side), **99%** < 800ms | 5% / 1% of slow requests |
| Serving latency — history API | **99%** of `/api/stocks/**/history` < **150ms** (pure cache read) | 1% |
| SSE liveness | **99%** of connection attempts succeed | 1% |
| Quote freshness (market hours) | **99%** of tracked tickers fresh (≤ 2× interval) | 1% ticker-minutes stale |
| Refresh success | **99.5%** of scheduled cycles complete | 0.5% |

Latency targets are intentionally aggressive because reads are cache-hits from embedded H2 — if
p99 drifts above these, something is wrong (GC pauses, H2 lock contention, N+1 render), which is
exactly the signal an SLO should surface.

## 4. Error-budget burn-rate alerting

Use Google-SRE **multi-window, multi-burn-rate** alerts rather than static thresholds — pages on
fast burn, tickets on slow burn. For the 99.5% availability SLO:

| Severity | Burn rate | Long window | Short window | Budget consumed |
|----------|-----------|-------------|--------------|-----------------|
| **Page** | 14.4×     | 1h          | 5m           | 2% in 1h |
| **Page** | 6×        | 6h          | 30m          | 5% in 6h |
| **Ticket** | 3×      | 24h         | 2h           | 10% in 24h |
| **Ticket** | 1×      | 72h         | 6h           | ~ budget over 3d |

The short window must also be burning before firing, to avoid alerting on a burst that already
recovered.

## 5. Observability stack

All open-source, container-friendly, and a natural fit for Spring Boot:

```
Spring Boot (Micrometer)  ──/actuator/prometheus──▶  Prometheus  ──▶  Grafana (dashboards)
        │                                                 │
        │ (OTLP traces)                                    └──▶  Alertmanager (burn-rate rules)
        ▼
   OpenTelemetry Collector ──▶ Tempo/Jaeger (traces)
```

### 5.1 Wiring it up (minimal, matches the existing stack)

Add to `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
  <scope>runtime</scope>
</dependency>
```

`application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true          # enables latency-SLO histograms
      slo:
        http.server.requests: 150ms,300ms,800ms
  observations:
    key-values:
      application: stock-insights
```

Spring Boot's `http_server_requests` timer already gives per-`uri`/`status` availability and
latency for free — most of the serving-plane SLIs need **zero** custom code.

### 5.2 Custom instrumentation (the parts Spring won't give you)

Keep instrumentation in the adapter layer so the domain stays framework-free (per this repo's
hexagonal rules). Wire a `MeterRegistry` into the outbound cache/scheduler adapters:

- **Cache freshness** — register a `Gauge` per tier that reports the max cache-entry age, or a
  `MultiGauge` tagged by ticker, read from the H2 cache's `updated_at` columns.
- **Refresh cycles** — wrap each scheduled refresh in a `Timer` and increment
  `refresh_cycles_total{tier,outcome}`.
- **Upstream calls** — count `upstream_calls_total{provider,endpoint,outcome}` in the
  `MarketDataPort` / `PriceHistoryPort` adapters, including a `throttled_skip` outcome so the
  ~1.1s throttle is observable.
- **SSE** — increment on connection open/close and keep a gauge of active emitters.

### 5.3 Dashboards (Grafana)

1. **SLO overview** — one stat panel per SLO showing current attainment vs. target and remaining
   error budget; a 30-day budget-burn timeline.
2. **Serving** — request rate, error rate, latency heatmap (p50/p95/p99) by `uri`.
3. **Freshness** — cache age per ticker (table + max-age timeseries), refresh success rate, time
   since last successful cycle per tier.
4. **Upstream** — Finnhub call outcomes, 429 rate vs. the free-tier limit, throttle skips, Yahoo
   latency.

### 5.4 Tracing

Add `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` and trace the refresh path
(scheduler → port adapter → HTTP call → cache write) and the render path (controller → cache
read → Thymeleaf). Traces answer "why was *this* dashboard load slow" and "why is AAPL stale" in
one click.

## 6. Load & failure testing (proving the SLOs hold)

- **Load** — `k6`/`hey` against `/dashboard` and the history API; assert p95/p99 stay under target
  as concurrency climbs. Publish the graph in the README.
- **Freshness under upstream failure** — a chaos toggle that makes the Finnhub adapter return 429s;
  confirm the **serving** SLO stays green while the **freshness** SLO burns and placeholders appear.
  This demonstrates the plane separation — the single most interview-relevant result here.
- **GC / pause injection** — `-XX:+UseSerialGC -Xmx64m` under load to show latency-SLO detection.

## 7. Runbook stubs

- **Serving-availability burn** → check `http_server_requests` by `uri`/`status`; is it one route?
  H2 lock? OOM? recent deploy? Roll back if deploy-correlated.
- **Freshness burn, serving green** → expected during upstream outage; check `upstream_calls_total`
  429 rate. Action: none if upstream; if throttle-skip driven, reduce tracked tickers or widen
  interval. **Do not** treat as a serving incident.
- **SSE liveness burn** → check active-connection gauge and proxy/idle-timeout config.
