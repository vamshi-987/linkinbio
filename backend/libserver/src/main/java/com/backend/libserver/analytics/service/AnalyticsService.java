package com.backend.libserver.analytics.service;

import com.backend.libserver.analytics.AnalyticsBreakdown;
import com.backend.libserver.analytics.AnalyticsSummary;

import java.util.UUID;

public interface AnalyticsService {

    AnalyticsSummary getSummary(UUID userId);

    /** Audience breakdown (country, device, referrer) served from the pre-aggregated rollups. */
    AnalyticsBreakdown getBreakdown(UUID userId);
}
