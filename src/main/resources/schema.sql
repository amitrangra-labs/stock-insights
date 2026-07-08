-- Cache tables for market data. Run automatically by Spring Boot on startup.
-- "Latest wins": one row per ticker, upserted via MERGE by the persistence adapter.

CREATE TABLE IF NOT EXISTS quotes (
    ticker         VARCHAR(16) PRIMARY KEY,
    current_price  DOUBLE      NOT NULL,
    price_change   DOUBLE      NOT NULL,
    percent_change DOUBLE      NOT NULL,
    day_high       DOUBLE      NOT NULL,
    day_low        DOUBLE      NOT NULL,
    day_open       DOUBLE      NOT NULL,
    previous_close DOUBLE      NOT NULL,
    as_of          BIGINT      NOT NULL  -- epoch millis
);

CREATE TABLE IF NOT EXISTS company_profiles (
    ticker     VARCHAR(16) PRIMARY KEY,
    name       VARCHAR(256),
    exchange   VARCHAR(128),
    currency   VARCHAR(16),
    industry   VARCHAR(128),
    market_cap DOUBLE,
    logo_url   VARCHAR(512),
    web_url    VARCHAR(512)
);
