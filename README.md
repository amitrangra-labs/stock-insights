# Stock Insights

An open-source web app that surfaces **richer per-stock data than a simple price ticker** —
fundamentals, earnings estimates, analyst ratings, news, and price history — for a small,
curated set of tickers.

Built with **Spring Boot + Thymeleaf** using a **Domain / Port / Adapter** (hexagonal)
architecture with **explicit Spring wiring** (no `@Autowired`, no stereotype scanning).

> Status: early scaffolding (M0). Dashboard and stock-detail pages land in the next milestones.

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
- <http://localhost:8080/health> — health check (`{"status":"UP",...}`)

Run the tests:

```bash
./mvnw test
```

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
