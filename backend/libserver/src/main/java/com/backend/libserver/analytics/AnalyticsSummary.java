package com.backend.libserver.analytics;

import java.util.List;

public record AnalyticsSummary(
        long totalClicks,
        List<DailyClickProjection> dailyClicks,
        List<LinkClickProjection> clicksPerLink
) {}


