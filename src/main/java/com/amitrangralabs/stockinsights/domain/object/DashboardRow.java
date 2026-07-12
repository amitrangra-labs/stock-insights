package com.amitrangralabs.stockinsights.domain.object;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * One row of the dashboard: a tracked ticker joined with its latest cached quote, fundamentals,
 * profile, and a short price-history spark.
 *
 * <p>Every tracked ticker gets a row even if nothing is cached yet. Absent values are {@code null}
 * and the view renders a placeholder — so the dashboard always lists the full watchlist. Build via
 * {@link #from} so the extraction from domain objects lives in one place.
 *
 * @param spark recent closing prices (oldest first) for the inline sparkline; never null
 */
public record DashboardRow(
        String ticker,
        String name,
        String currency,
        Double price,
        Double change,
        Double percentChange,
        Double dayHigh,
        Double dayLow,
        Double open,
        Double previousClose,
        Long volume,
        Double high52Week,
        Double low52Week,
        Double marketCapMillions,
        List<Double> spark,
        Instant asOf) {

    public DashboardRow {
        spark = spark == null ? List.of() : List.copyOf(spark);
    }

    /** Assemble a row from cached data. {@code sparkDays} bounds the sparkline length. */
    public static DashboardRow from(
            String ticker,
            Optional<Quote> quote,
            Optional<CompanyProfile> profile,
            Optional<Fundamentals> fundamentals,
            List<PricePoint> history,
            int sparkDays) {

        String name = profile.map(CompanyProfile::name).filter(n -> !n.isBlank()).orElse(ticker);
        String currency = profile.map(CompanyProfile::currency).orElse(null);

        List<Double> spark = history.stream()
                .skip(Math.max(0, history.size() - sparkDays))
                .map(PricePoint::close)
                .toList();
        Long volume = history.isEmpty() ? null : history.get(history.size() - 1).volume();

        return new DashboardRow(
                ticker,
                name,
                currency,
                quote.map(Quote::current).orElse(null),
                quote.map(Quote::change).orElse(null),
                quote.map(Quote::percentChange).orElse(null),
                quote.map(Quote::high).orElse(null),
                quote.map(Quote::low).orElse(null),
                quote.map(Quote::open).orElse(null),
                quote.map(Quote::previousClose).orElse(null),
                volume,
                fundamentals.map(Fundamentals::high52Week).orElse(null),
                fundamentals.map(Fundamentals::low52Week).orElse(null),
                profile.map(CompanyProfile::marketCap).filter(mc -> mc > 0).orElse(null),
                spark,
                quote.map(Quote::asOf).orElse(null));
    }

    // --- state ---

    public boolean hasQuote() {
        return price != null;
    }

    /** True when the latest change is flat or positive (used by the view for colour). */
    public boolean isUp() {
        return change != null && change >= 0;
    }

    public boolean hasDayRange() {
        return price != null && dayHigh != null && dayLow != null && dayHigh > dayLow;
    }

    public boolean has52WeekRange() {
        return price != null && high52Week != null && low52Week != null && high52Week > low52Week;
    }

    public boolean hasSpark() {
        return spark.size() >= 2;
    }

    // --- display helpers (the template lacks the Thymeleaf java8time dialect) ---

    private static final DateTimeFormatter AS_OF_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    public String asOfDisplay() {
        return asOf == null ? "—" : AS_OF_FORMAT.format(asOf);
    }

    /** Position of the current price within the day's range, 0–100 (for a marker). */
    public double dayRangePercent() {
        return hasDayRange() ? clampPercent((price - dayLow) / (dayHigh - dayLow) * 100.0) : 0.0;
    }

    /** Position of the current price within the 52-week range, 0–100. */
    public double week52Percent() {
        return has52WeekRange()
                ? clampPercent((price - low52Week) / (high52Week - low52Week) * 100.0)
                : 0.0;
    }

    /** Human-readable share volume, e.g. {@code 12.3M}. */
    public String volumeDisplay() {
        return volume == null ? "—" : compact(volume.doubleValue());
    }

    /** Human-readable market cap (input is millions of USD), e.g. {@code 3.01T}. */
    public String marketCapDisplay() {
        return marketCapMillions == null ? "—" : compact(marketCapMillions * 1_000_000.0);
    }

    /** SVG polyline points for the sparkline in a 100×28 viewBox, or empty when no spark. */
    public String sparkPoints() {
        if (!hasSpark()) {
            return "";
        }
        double min = spark.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = spark.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double range = max - min == 0 ? 1 : max - min;
        double w = 100.0;
        double h = 28.0;
        double pad = 2.0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spark.size(); i++) {
            double x = spark.size() == 1 ? w / 2 : (i / (double) (spark.size() - 1)) * w;
            double y = pad + (h - 2 * pad) * (1 - (spark.get(i) - min) / range);
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(round(x)).append(',').append(round(y));
        }
        return sb.toString();
    }

    /** Sparkline direction over the window (last vs first close), for colour. */
    public boolean sparkUp() {
        return hasSpark() && spark.get(spark.size() - 1) >= spark.get(0);
    }

    /**
     * A heatmap background for the % change cell: green (up) or red (down) with opacity scaled by
     * magnitude (saturating around ±3%). Empty when there is no quote.
     */
    public String heatStyle() {
        if (percentChange == null) {
            return "";
        }
        double alpha = Math.min(Math.abs(percentChange) / 3.0, 1.0) * 0.32;
        String rgb = percentChange >= 0 ? "22,163,74" : "220,38,38";
        return "background-color: rgba(" + rgb + "," + String.format("%.2f", alpha) + ")";
    }

    private static double clampPercent(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 10.0) / 10.0);
    }

    private static String compact(double v) {
        double abs = Math.abs(v);
        if (abs >= 1e12) {
            return trim(v / 1e12) + "T";
        }
        if (abs >= 1e9) {
            return trim(v / 1e9) + "B";
        }
        if (abs >= 1e6) {
            return trim(v / 1e6) + "M";
        }
        if (abs >= 1e3) {
            return trim(v / 1e3) + "K";
        }
        return trim(v);
    }

    private static String trim(double v) {
        return String.format("%.2f", v);
    }
}
