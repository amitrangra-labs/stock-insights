# Stock Insights

An open-source web app that surfaces **richer per-stock data than a simple price ticker** —
fundamentals, earnings estimates, analyst ratings, news, and price history — for a small,
curated set of tickers.

Built with **Spring Boot + Thymeleaf** using a **Domain / Port / Adapter** (hexagonal)
architecture with **explicit Spring wiring** (no `@Autowired`, no stereotype scanning).

> Status: M3 — an editable watchlist dashboard and rich per-stock detail pages
> (interactive price chart, fundamentals, analyst ratings, news). Everything is
> served from a background-refreshed local cache.

## Features

- **Dashboard** of tracked tickers, seeded with a default set (8 large-caps + SPY/QQQ)
  on first run; **add/remove tickers from the UI** with **search autocomplete**
  (keyless — a bundled symbol catalog, so it works with no API key), plus a
  **Reset to defaults** button that clears the watchlist and restores the seed. Adding
  a ticker fetches its data immediately; the watchlist is persisted in H2.
  - **Rich columns**: day range, volume, market cap and 52-week range (as position
    bars), plus a 30-day **sparkline** per row.
  - A **market-breadth strip** (advancing/declining + top movers), a **heatmap**-tinted
    % column, and **sortable** columns.
  - **Live updates** over **Server-Sent Events** that flash cells green/red on change —
    real-time trades from Finnhub's WebSocket when the API key is set, and periodic cache
    snapshots otherwise.
- **Stock detail page** per ticker:
  - **Interactive price chart** — hover for a date/price tooltip, zoom the range
    from 1 month out to 5 years. Dependency-free (no charting library, works offline).
  - **Fundamentals** (P/E, EPS, 52-week high/low, dividend yield, beta).
  - **Analyst ratings** distribution (strong buy → strong sell).
  - **Recent company news**.
- **Background refresh** into a local cache, so pages never call the APIs directly.
  Two cadences keep usage under Finnhub's free tier (60 calls/min): a **live** tier
  (quote + news) every 5 min and a **reference** tier (profile, fundamentals, ratings,
  history) daily. Calls are also **throttled** (~1.1s apart) so bursts never trip the
  limit, and adding a ticker refreshes it on a background thread. Missing data degrades
  gracefully to placeholders. See the `stock-insights.*` refresh settings in
  [`application.yml`](src/main/resources/application.yml). Rule of thumb: **~10 tickers**
  is comfortable with a Finnhub key; history (Yahoo, keyless) has no such limit.

## Quick start

Prerequisites: JDK 21+ (tested on Corretto 26).

```bash
git clone https://github.com/amitrangra-labs/stock-insights.git
cd stock-insights

# (optional for M0) add free API keys for later milestones
cp .env.example .env   # then fill in the keys
set -a && source .env && set +a

./mvnw spring-boot:run
```

Then open:

- <http://localhost:8080/> — landing page
- <http://localhost:8080/dashboard> — tracked-ticker dashboard (each ticker links to its detail page)
- <http://localhost:8080/stocks/AAPL> — stock detail: latest quote, company profile, and a price-history chart
- <http://localhost:8080/api/stocks/AAPL/history> — JSON price history (backs the chart)
- <http://localhost:8080/health> — health check (`{"status":"UP",...}`)
- <http://localhost:8080/observability> — SLO & metrics dashboard (embeds Grafana; see [Observability](#observability-metrics-slos--dashboards))
- <http://localhost:8080/actuator/prometheus> — Prometheus metrics scrape endpoint

Quotes and company profiles come from Finnhub (needs a free key); **price history
comes from a keyless source, so the chart works with no key at all.** Without a
`FINNHUB_API_KEY` the app still runs — the dashboard and detail pages show
placeholder (`—`) quote values and log a warning each refresh, while the history
chart still populates. Add a free `FINNHUB_API_KEY` (below) for live-ish quotes.
Edit the tracked tickers under `stock-insights.tracked-tickers` in
[`application.yml`](src/main/resources/application.yml).

Run the tests:

```bash
./mvnw test
```

## Run with Docker

The app ships as a self-contained container — no local JDK or Maven needed, just
Docker. The image is a multi-stage build (Maven builds the jar, a slim JRE runs it).

**Build the image:**

```bash
docker build -t stock-insights .
```

**Run it** (pass your free API key as an environment variable):

```bash
docker run --rm -p 8080:8080 \
  -e FINNHUB_API_KEY=your_key_here \
  stock-insights
```

Then open <http://localhost:8080/dashboard>. Omit `-e FINNHUB_API_KEY=...` to run
without live data (placeholder rows).

**Persist the cache** across restarts by mounting a volume onto `/app/data`
(where the H2 database lives):

```bash
docker run --rm -p 8080:8080 \
  -e FINNHUB_API_KEY=your_key_here \
  -v stock-insights-data:/app/data \
  stock-insights
```

**Or use Docker Compose** (builds, runs, and creates the data volume for you):

```bash
# reads FINNHUB_API_KEY from your shell or a local .env file
export FINNHUB_API_KEY=your_key_here
docker compose up --build
```

Tune the JVM via `JAVA_OPTS`, e.g. `-e JAVA_OPTS="-Xmx256m"`. Override any setting
with a Spring env var, e.g. `-e STOCK_INSIGHTS_REFRESH_INTERVAL_MS=60000`. The image
sets a container-appropriate H2 URL (no `AUTO_SERVER`) and a `/health` healthcheck, so
`docker compose ps` shows the app's health.

### Troubleshooting

- **"Cannot remove Docker Compose application … Max retries reached: connect ECONNREFUSED"** —
  this is Docker Desktop losing its backend connection, usually because a container is
  stuck/crash-looping. Recover with:

  ```bash
  docker compose kill && docker compose rm -f     # force-remove the stack
  docker compose down -v                           # also drop the data volume (clears any stale H2 lock)
  ```

  If it persists, **restart Docker Desktop**, then `docker compose up --build`. Dropping the
  volume (`-v`) is safe — it only holds the local cache, which is re-fetched on next start.

## Observability (metrics, SLOs & dashboards)

Stock Insights ships a full metrics + SLO stack you can bring up with one command. The design
principle: **serving** (request availability & latency) and **data freshness** (cache staleness) are
tracked as two independent reliability planes, so an upstream (Finnhub) outage burns the *freshness*
budget while the *serving* SLO stays green. The SLO maths lives once, as Prometheus recording rules,
and is presented once, in Grafana. Full design: [docs/SLO.md](docs/SLO.md) and
[docs/SLO-DASHBOARD.md](docs/SLO-DASHBOARD.md).

### What the app exposes

Spring Boot Actuator + Micrometer expose these endpoints on the app (port 8080):

| Endpoint | What it is |
|----------|-----------|
| `/actuator/prometheus` | Prometheus scrape endpoint (all metrics, text format) |
| `/actuator/health` | Actuator health (the simple public `/health` is still there too) |
| `/observability` | In-app page that embeds the Grafana SLO dashboard |

Metrics exposed include:

- **`http_server_requests_seconds`** (histogram) — per-route request rate, error rate, and latency
  quantiles; SLO histogram buckets at 150ms/300ms/800ms. Backs the serving availability & latency SLOs.
- **`cache_quote_fresh_ratio`** (gauge) — fraction of watchlist tickers whose cached quote is within
  the freshness threshold (2× the live-refresh interval). The freshness SLI.
- **`cache_quote_oldest_age_seconds`** (gauge) — age of the oldest cached quote; drives a staleness alert.
- **`refresh_cycle_duration_seconds`** (timer, tagged by `tier`) — duration and cadence-liveness of the
  background live/reference refresh loops.

### Run the whole stack

`docker compose up --build` starts three containers:

| Service | URL | Role |
|---------|-----|------|
| `app` | <http://localhost:8080> | the application (scraped at `/actuator/prometheus`) |
| `prometheus` | <http://localhost:9090> | time-series database + scraper + SLO recording/alerting rules |
| `grafana` | <http://localhost:3000> | dashboards (Prometheus datasource + SLO dashboard auto-provisioned) |

```bash
export FINNHUB_API_KEY=your_key_here   # optional; without it quotes stay as placeholders
docker compose up --build
```

Then open:

- **<http://localhost:8080/observability>** — the SLO dashboard embedded inside the app, **or**
- **<http://localhost:3000>** — Grafana directly (dashboard **Stock Insights → Stock Insights SLO**).
  Grafana is a separate container reading Prometheus, so this view still works even if the app is down —
  the in-app page is a convenience embed. Anonymous read-only access is enabled for local use; put
  Grafana behind SSO / a reverse proxy for any real deployment.

Everything is provisioned as code — no click-ops:

```
prometheus/prometheus.yml              scrape config
prometheus/rules/slo.rules.yml         SLO recording + alerting rules (single source of truth)
grafana/provisioning/                  datasource + dashboard providers
grafana/dashboards/stock-insights-slo.json   the SLO dashboard model
```

Point the in-app page at a Grafana elsewhere with
`STOCK_INSIGHTS_OBSERVABILITY_GRAFANA_URL=https://grafana.example.com`.

## Data sources (free tiers)

Stock data comes from free APIs behind port interfaces, so providers can be swapped
without touching the domain:

- **[Finnhub](https://finnhub.io/register)** (`MarketDataPort`) — quotes, company
  profiles, fundamentals, analyst ratings, and company news. Needs a free API key.
- **Yahoo Finance chart API** (`PriceHistoryPort`) — daily price history. Keyless, so the
  chart works out of the box. Unofficial endpoint; swap behind the port for a licensed
  feed if needed.

A background scheduler refreshes the tracked tickers periodically and pages always read
from a local cache (H2) — never calling the APIs directly, which keeps the UI fast and
within free-tier rate limits.

## Architecture

```
com.amitrangralabs.stockinsights
├── Application.java          # @SpringBootApplication entry point
├── domain/                   # business core — no framework imports
│   ├── service/              #   plain-Java services (constructor-injected with ports)
│   ├── object/               #   immutable records (Quote, CompanyProfile, ...)
│   └── config/DomainConfig   #   @Bean wiring for domain services
├── port/                     # outbound interfaces (domain -> outside world)
└── adapter/
    ├── in/
    │   ├── endpoint/         #   web + REST + scheduler entry points
    │   └── config/InboundConfig
    └── out/
        ├── client/           #   MarketDataPort / repository implementations
        └── config/OutboundConfig
```

Three `@Configuration` classes (`DomainConfig`, `InboundConfig`, `OutboundConfig`) construct
**every** bean by hand — reading them tells you the entire object graph. See
[CONTRIBUTING.md](CONTRIBUTING.md) for the rules that keep the layering honest.

## License

[MIT](LICENSE)
