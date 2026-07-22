package com.backend.libserver.analytics;

import java.time.LocalDate;
import java.util.List;

/**
 * Audience breakdown over a window. {@code rolledUpThrough} is the last day the rollup job has
 * covered, so the client can say "as of …" instead of implying the numbers are live to the second.
 */
public record AnalyticsBreakdown(
        int windowDays,
        LocalDate from,
        LocalDate to,
        long totalClicks,
        List<BreakdownSlice> byCountry,
        List<BreakdownSlice> byDevice,
        List<BreakdownSlice> byReferrer,
        LocalDate rolledUpThrough
) {}
