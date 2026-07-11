-- Cache tables for market data. Run automatically by Spring Boot on startup.
-- "Latest wins": one row per ticker, upserted via MERGE by the persistence adapter.

-- The watchlist: the set of tracked tickers, editable at runtime. Seeded from
-- stock-insights.tracked-tickers on first start (when empty).
CREATE TABLE IF NOT EXISTS watchlist (
    ticker   VARCHAR(16) PRIMARY KEY,
    added_at BIGINT NOT NULL  -- epoch millis, preserves insertion order
);

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

-- Daily price history, one row per (ticker, trading day). Upserted by the persistence adapter.
CREATE TABLE IF NOT EXISTS price_history (
    ticker      VARCHAR(16) NOT NULL,
    trade_date  DATE        NOT NULL,
    open_price  DOUBLE      NOT NULL,
    high_price  DOUBLE      NOT NULL,
    low_price   DOUBLE      NOT NULL,
    close_price DOUBLE      NOT NULL,
    volume      BIGINT      NOT NULL,
    PRIMARY KEY (ticker, trade_date)
);

-- Recent company news, one row per (ticker, article id).
CREATE TABLE IF NOT EXISTS news (
    ticker       VARCHAR(16)  NOT NULL,
    id           BIGINT       NOT NULL,
    headline     VARCHAR(1024),
    source       VARCHAR(256),
    url          VARCHAR(1024),
    summary      VARCHAR(4096),
    published_at BIGINT       NOT NULL,  -- epoch millis
    image_url    VARCHAR(1024),
    PRIMARY KEY (ticker, id)
);

-- Latest analyst recommendation counts, one row per ticker.
CREATE TABLE IF NOT EXISTS analyst_ratings (
    ticker      VARCHAR(16) PRIMARY KEY,
    period      DATE,
    strong_buy  INT NOT NULL,
    buy         INT NOT NULL,
    hold        INT NOT NULL,
    sell        INT NOT NULL,
    strong_sell INT NOT NULL
);

-- Key fundamental metrics, one row per ticker (nullable metrics).
CREATE TABLE IF NOT EXISTS fundamentals (
    ticker         VARCHAR(16) PRIMARY KEY,
    pe_ratio       DOUBLE,
    eps            DOUBLE,
    high_52w       DOUBLE,
    low_52w        DOUBLE,
    dividend_yield DOUBLE,
    beta           DOUBLE
);
