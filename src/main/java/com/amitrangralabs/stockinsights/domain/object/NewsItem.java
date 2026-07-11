package com.amitrangralabs.stockinsights.domain.object;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * A single company-news article.
 *
 * @param id          provider article id (used to de-duplicate)
 * @param headline    article headline
 * @param source      publisher name
 * @param url         link to the full article
 * @param summary     short summary (may be blank)
 * @param publishedAt publication time
 * @param imageUrl    thumbnail URL (may be blank)
 */
public record NewsItem(
        long id,
        String headline,
        String source,
        String url,
        String summary,
        Instant publishedAt,
        String imageUrl) {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    /** Publication date formatted for display (the template lacks the java8time dialect). */
    public String publishedDisplay() {
        return publishedAt == null ? "" : DATE.format(publishedAt);
    }
}
