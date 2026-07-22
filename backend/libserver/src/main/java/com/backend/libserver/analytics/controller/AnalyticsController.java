package com.backend.libserver.analytics.controller;

import com.backend.libserver.analytics.AnalyticsBreakdown;
import com.backend.libserver.analytics.AnalyticsSummary;
import com.backend.libserver.analytics.service.AnalyticsService;

import com.backend.libserver.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public AnalyticsSummary getSummary(@AuthenticationPrincipal UserPrincipal user) {
        return analyticsService.getSummary(user.getId());
    }

    @GetMapping("/breakdown")
    public AnalyticsBreakdown getBreakdown(@AuthenticationPrincipal UserPrincipal user) {
        return analyticsService.getBreakdown(user.getId());
    }
}
