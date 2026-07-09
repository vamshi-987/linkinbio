package com.backend.libserver.analytics.service.impl;

import com.backend.libserver.analytics.AnalyticsSummary;
import com.backend.libserver.analytics.DailyClickProjection;
import com.backend.libserver.analytics.LinkClickProjection;
import com.backend.libserver.analytics.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl {

    private final ClickEventRepository clickEventRepository;

    public AnalyticsSummary getSummary(UUID userId) {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        List<DailyClickProjection> daily = clickEventRepository.getDailyClicks(userId, since);
        List<LinkClickProjection> perLink = clickEventRepository.getClicksPerLink(userId);

        long total = perLink.stream().mapToLong(LinkClickProjection::getClicks).sum();

        return new AnalyticsSummary(total, daily, perLink);
    }

}
