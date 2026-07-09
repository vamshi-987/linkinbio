package com.backend.libserver.analytics.service;

import com.backend.libserver.analytics.AnalyticsSummary;

import java.util.UUID;

public interface AnalyticsService {

    AnalyticsSummary getSummary(UUID userId);
}
