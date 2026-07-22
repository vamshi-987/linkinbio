package com.backend.libserver.analytics.scheduler;

import com.backend.libserver.analytics.repository.ClickDailyRollupRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Keeps the read-side rollups in step with the raw click stream.
 *
 * <p>Rebuilds the last few UTC days rather than only today: clicks are written asynchronously, so an
 * event can land a moment after midnight and belong to the previous day, and a run that is missed
 * while the app is down is picked up on the next tick. Since {@code rebuildDay} replaces a day's
 * counts instead of adding to them, re-covering days already processed costs a little work and
 * changes nothing — which is the property that makes the job safe to run anywhere, any number of
 * times.
 */
@Component
@RequiredArgsConstructor
public class ClickRollupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClickRollupScheduler.class);

    private final ClickDailyRollupRepository rollupRepository;

    /** How many days back each run rebuilds, counting today. */
    @Value("${app.analytics.rollup-lookback-days:2}")
    private int lookbackDays;

    @Scheduled(
            fixedDelayString = "${app.analytics.rollup-interval-ms:900000}",
            initialDelayString = "${app.analytics.rollup-initial-delay-ms:30000}")
    @Transactional
    public void rollUpRecentDays() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        try {
            int rows = 0;
            for (int i = 0; i < Math.max(1, lookbackDays); i++) {
                rows += rollupRepository.rebuildDay(today.minusDays(i));
            }
            log.debug("Click rollup refreshed {} row(s) across {} day(s)", rows, lookbackDays);
        } catch (RuntimeException ex) {
            // Dashboards keep serving the previous rollup; the next tick recomputes the same days.
            log.warn("Click rollup failed, will retry on next tick: {}", ex.getMessage());
        }
    }
}
