# Stock Insights — SLO Dashboard (merged product + ops view)

**Status: DRAFT (design only).** Companion to [SLO.md](SLO.md), which defines the SLIs/SLOs. This
doc covers *storing* the metrics in a containerised time-series database and *plotting* them on a
single dashboard that serves both audiences.

## 1. Decision: merge at the definition layer, not the presentation layer

We do **not** build two dashboards. We build:

1. **One time-series database** (Prometheus) storing all metrics.
2. **One source of truth for SLO math** — Prometheus **recording rules**. Attainment, error-budget
   remaining, and burn rate are each computed once, in PromQL, and every consumer reads them.
3. **One dashboard** (Grafana) with two zones: a product-facing **SLO summary** on top, **ops
   detail** below.
4. **One in-app page** — stock-insights exposes `/observability`, a thin route that **embeds** that
   same Grafana dashboard. It is a *view* of the single dashboard, not a second implementation.

What this deliberately avoids: a bespoke Java `SloService` computing SLOs from the Prometheus HTTP
API in parallel with Grafana computing them a different way. See §8 for why that variant is dropped.

## 2. Architecture

```
                        ┌──────────────────────── docker compose network ─────────────────────────┐
                        │                                                                          │
  browser ──▶ /observability (stock-insights) ──iframe embed──┐                                    │
                        │                                      ▼                                    │
                        │   stock-insights:8080          Grafana:3000  ──(PromQL)──▶ Prometheus:9090
                        │   /actuator/prometheus  ◀──scrape──────────────────────────────┘ │       │
                        │        (Micrometer)                                    TSDB + recording   │
                        │                                                        rules + alerting   │
                        └──────────────────────────────────────────────────────────────────────────┘
                                                                                     │
                                                                              Alertmanager (burn-rate pages)
```

- stock-insights exposes metrics at `/actuator/prometheus` (per SLO.md §5).
- Prometheus **pulls** every 15s, stores them, and evaluates **recording + alerting rules**.
- Grafana reads Prometheus and renders the one dashboard.
- The app's `/observability` page embeds that dashboard so it lives "inside the product" too.

## 3. Time-series database choice

| Option | Fit here | Notes |
|--------|----------|-------|
| **Prometheus** ✅ recommended | native Micrometer target, pull-based, PromQL, recording rules, huge Grafana support | local TSDB; single container; perfect for this scale |
| **VictoriaMetrics** | drop-in Prometheus-compatible, lower RAM/disk, single binary | swap later if retention/cardinality grows; same PromQL & dashboards |
| **InfluxDB / TimescaleDB** | push-based / SQL | more setup, weaker Micrometer-scrape story; skip unless you need SQL |

Recommendation: **Prometheus now**, with VictoriaMetrics as a documented "if it grows" swap — the
dashboards and rules carry over unchanged because both speak PromQL.

**Retention:** `--storage.tsdb.retention.time=90d` covers a 30-day SLO window with headroom on a
mounted volume. For longer history, Prometheus `remote_write` to VictoriaMetrics/Thanos — out of
scope for the portfolio version.

## 4. SLO math as code — Prometheus recording rules (the single source of truth)

Define each SLO once. Front-end and alerting both read these series. Route filter reused throughout:
`uri=~"/dashboard|/stocks/.*|/api/stocks/.*/history"`.

```yaml
# prometheus/rules/slo.rules.yml   (DRAFT)
groups:
- name: stock-insights-slo
  interval: 30s
  rules:

  # ---- Serving availability (target 99.5% over 30d) ----
  - record: si:serving_availability:ratio_30d
    expr: |
      1 - (
        sum(increase(http_server_requests_seconds_count{status=~"5..",uri=~"/dashboard|/stocks/.*|/api/stocks/.*/history"}[30d]))
        /
        sum(increase(http_server_requests_seconds_count{uri=~"/dashboard|/stocks/.*|/api/stocks/.*/history"}[30d]))
      )

  # error-budget remaining as a fraction of the 0.5% budget (1.0 = full, 0 = exhausted)
  - record: si:serving_availability:budget_remaining_30d
    expr: |
      1 - (
        (1 - si:serving_availability:ratio_30d) / (1 - 0.995)
      )

  # fast burn-rate signal (1h) — used by alerting; >14.4 pages
  - record: si:serving_availability:burnrate_1h
    expr: |
      (
        sum(rate(http_server_requests_seconds_count{status=~"5..",uri=~"/dashboard|/stocks/.*|/api/stocks/.*/history"}[1h]))
        /
        sum(rate(http_server_requests_seconds_count{uri=~"/dashboard|/stocks/.*|/api/stocks/.*/history"}[1h]))
      ) / (1 - 0.995)

  # ---- Serving latency (95% of page loads < 300ms) ----
  - record: si:page_latency:under_300ms_ratio_30d
    expr: |
      sum(rate(http_server_requests_seconds_bucket{le="0.3",uri=~"/dashboard|/stocks/.*"}[30d]))
      /
      sum(rate(http_server_requests_seconds_count{uri=~"/dashboard|/stocks/.*"}[30d]))

  # ---- Quote freshness (99% of tickers fresh, ≤ 600s) ----
  # cache_entry_age_seconds is the custom gauge from SLO.md §5.2
  - record: si:quote_freshness:fresh_ratio
    expr: |
      count(cache_entry_age_seconds{tier="live"} < 600)
      /
      count(cache_entry_age_seconds{tier="live"})
```

The SLO *targets* (0.995, 300ms, 600s) live here in one file — change a target in one place and
every panel and alert follows. Alerting rules (burn-rate pages) reference `...:burnrate_1h` etc.,
exactly the multi-window scheme in SLO.md §4.

## 5. The one Grafana dashboard — two zones, provisioned as code

A single dashboard JSON, version-controlled, provisioned on startup (no click-ops):

**Zone A — SLO summary (product-facing, top of the dashboard):**
- One **gauge/stat per SLO**: current attainment vs. target (`si:serving_availability:ratio_30d`,
  `si:page_latency:under_300ms_ratio_30d`, `si:quote_freshness:fresh_ratio`), green/amber/red by
  threshold.
- **Error-budget remaining** bar (`si:...:budget_remaining_30d`) + a 30-day budget-burn timeline.

**Zone B — ops detail (below):**
- Request rate, error rate, latency heatmap (p50/p95/p99) by `uri`.
- Freshness: cache age per ticker (table) + max-age timeseries; time since last successful refresh.
- Upstream: Finnhub outcomes, 429 rate vs. the free-tier limit, throttle skips.

Provisioned via files (this is the "observability-as-code" portfolio signal):

```
grafana/provisioning/datasources/prometheus.yml     # points at http://prometheus:9090
grafana/provisioning/dashboards/dashboards.yml       # loads JSON from a folder
grafana/dashboards/stock-insights-slo.json           # the dashboard model, in git
```

## 6. The in-app `/observability` page (embed, don't reimplement)

A thin inbound endpoint + Thymeleaf shell that embeds the Grafana dashboard, so the single dashboard
also appears inside the product — no second SLO implementation.

- New inbound endpoint `ObservabilityEndpoint` (wired by hand in `InboundConfig`, per repo rules)
  serving `GET /observability` → `observability.html`.
- The template embeds Grafana. Two embed styles:
  - **Whole dashboard**: `<iframe src="http://localhost:3000/d/<uid>/stock-insights-slo?kiosk">`.
  - **Individual panels**: Grafana per-panel `/d-solo/...&panelId=N` iframes, so you can lay the SLO
    gauges into the app's own page chrome and match the site style.
- Grafana config to allow embedding (compose env): `GF_SECURITY_ALLOW_EMBEDDING=true`,
  `GF_AUTH_ANONYMOUS_ENABLED=true`, `GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer` — anonymous **read-only** for
  the demo.

Because the endpoint only returns an HTML shell pointing at Grafana, the SLO logic stays entirely in
Prometheus rules + the Grafana model. Nothing to keep in sync.

## 7. docker-compose additions (DRAFT)

```yaml
# extends the existing app service
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./prometheus/rules:/etc/prometheus/rules:ro
      - prometheus-data:/prometheus
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.retention.time=90d
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:latest
    environment:
      GF_SECURITY_ALLOW_EMBEDDING: "true"
      GF_AUTH_ANONYMOUS_ENABLED: "true"
      GF_AUTH_ANONYMOUS_ORG_ROLE: "Viewer"
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
      - grafana-data:/var/lib/grafana
    ports: ["3000:3000"]
    depends_on: [prometheus]

volumes:
  prometheus-data:
  grafana-data:
```

```yaml
# prometheus/prometheus.yml   (DRAFT)
global:
  scrape_interval: 15s
rule_files:
  - /etc/prometheus/rules/*.rules.yml
scrape_configs:
  - job_name: stock-insights
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["app:8080"]     # 'app' = the compose service name
```

## 8. Trade-offs & what we deliberately dropped

- **Availability of the view.** The `/observability` page is app-hosted, so it dies with the app.
  That's acceptable *because it's only an embed* — Grafana (separate container) still shows the SLOs
  at `:3000`, and that URL is the real monitoring surface. Bookmark Grafana; treat the in-app page as
  a convenience/demo.
- **Auth.** Anonymous read-only Grafana is fine for a local portfolio demo; note in the README that a
  real deployment would put it behind SSO / a reverse proxy and not enable anonymous embedding.
- **Dropped: bespoke Java `SloService` + `MetricsQueryPort`.** Having the app query the Prometheus
  HTTP API and recompute SLOs in Java would duplicate the recording-rule math and let the two drift —
  the exact anti-pattern "merge" is meant to kill. If you specifically want the hexagonal exercise,
  the *only* thing worth building natively is a **read-through JSON proxy** (`GET /api/slo` that
  forwards a fixed PromQL to Prometheus and returns the series) so a custom in-app widget can reuse
  the repo's dependency-free chart — but it must read the **recording-rule series**, never recompute
  the SLO. Keep the math in Prometheus.
- **Cardinality.** `cache_entry_age_seconds{ticker=...}` is per-ticker; fine at ~10 tickers, but note
  it as the first thing to bound if the watchlist grows (drop to a `MultiGauge` max, or aggregate).

## 9. Suggested build order

1. Actuator + Micrometer Prometheus registry (SLO.md §5.1) → `/actuator/prometheus` live.
2. Add `prometheus` + `grafana` to compose; confirm the target scrapes green.
3. Land `slo.rules.yml` recording rules; verify the `si:*` series in Prometheus.
4. Provision the Grafana dashboard JSON (Zone A + Zone B).
5. Add the `/observability` embed page.
6. Add Alertmanager + burn-rate alerting rules referencing the `si:*:burnrate_*` series.
7. Load test (k6) to populate real latency/availability data and screenshot the dashboard for the
   README.
