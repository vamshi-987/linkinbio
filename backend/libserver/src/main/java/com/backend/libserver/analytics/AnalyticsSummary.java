package com.backend.libserver.analytics;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsSummary(
        long totalClicks,
        int windowDays,
        LocalDate from,
        LocalDate to,
        List<DailyClick> dailyClicks,
        List<LinkClick> clicksPerLink
) {}
