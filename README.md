# Stock Insights

An open-source web app that surfaces **richer per-stock data than a simple price ticker** —
fundamentals, earnings estimates, analyst ratings, news, and price history — for a small,
curated set of tickers.

Built with **Spring Boot + Thymeleaf** using a **Domain / Port / Adapter** (hexagonal)
architecture with **explicit Spring wiring** (no `@Autowired`, no stereotype scanning).

> Status: M1 — a live dashboard of tracked tickers backed by a background refresh.
> Stock-detail pages land in the next milestone.

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
- <http://localhost:8080/dashboard> — tracked-ticker dashboard
- <http://localhost:8080/health> — health check (`{"status":"UP",...}`)

Without an API key the app still runs — the dashboard lists the tracked tickers
with placeholder (`—`) values and logs a warning each refresh. Add a free
`FINNHUB_API_KEY` (below) to populate live-ish prices. Edit the tracked tickers
under `stock-insights.tracked-tickers` in
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
with a Spring env var, e.g. `-e STOCK_INSIGHTS_REFRESH_INTERVAL_MS=60000`.

## Data sources (free tiers)

Stock data comes from free/freemium APIs behind a `MarketDataPort` interface, so providers
can be swapped without touching the domain:

- **[Finnhub](https://finnhub.io/register)** — quotes, company profile, news, analyst
  recommendation trends.
- **[Alpha Vantage](https://www.alphavantage.co/support/#api-key)** — fundamentals and
  earnings estimates.

Free tiers are rate-limited, so a background scheduler refreshes the tracked tickers
periodically and pages always read from a local cache (H2) — never calling the APIs directly.

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
