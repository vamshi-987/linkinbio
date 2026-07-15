package com.backend.libserver.analytics.service.impl;

import com.backend.libserver.analytics.AnalyticsSummary;
import com.backend.libserver.analytics.DailyClick;
import com.backend.libserver.analytics.DailyClickProjection;
import com.backend.libserver.analytics.LinkClick;
import com.backend.libserver.analytics.repository.ClickEventRepository;
import com.backend.libserver.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int WINDOW_DAYS = 30;

    private final ClickEventRepository clickEventRepository;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummary getSummary(UUID userId) {
        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(WINDOW_DAYS - 1L);
        Instant since = from.atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<LocalDate, Long> clicksByDay = clickEventRepository.getDailyClicks(userId, since).stream()
                .collect(Collectors.toMap(DailyClickProjection::getDay, DailyClickProjection::getClicks));

        // The query returns only the days that had clicks. Emit every day in the window so the
        // chart's time axis stays evenly spaced instead of closing the gaps over quiet days.
        List<DailyClick> dailyClicks = IntStream.range(0, WINDOW_DAYS)
                .mapToObj(from::plusDays)
                .map(day -> new DailyClick(day, clicksByDay.getOrDefault(day, 0L)))
                .toList();

        List<LinkClick> clicksPerLink = clickEventRepository.getClicksPerLink(userId, since).stream()
                .map(p -> new LinkClick(p.getLinkId(), p.getTitle(), p.getClicks()))
                .toList();

        long totalClicks = dailyClicks.stream().mapToLong(DailyClick::clicks).sum();

        return new AnalyticsSummary(totalClicks, WINDOW_DAYS, from, to, dailyClicks, clicksPerLink);
    }
}
