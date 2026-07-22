package com.backend.libserver.link.scheduler;

import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Publishes and retires scheduled links on time.
 *
 * <p>Visibility itself is decided at query time, so nothing here has to flip a flag — the only thing
 * that would otherwise lag behind the schedule is the cached public profile, which can be up to its
 * TTL out of date. This sweep evicts exactly the profiles whose links crossed a boundary.
 *
 * <p>The job is deliberately idempotent and stateless: it re-checks a window that overlaps the
 * previous run instead of remembering a cursor, so a missed tick, a restart mid-run, or two app
 * instances running it at once all converge on the same result — an eviction that has already
 * happened simply evicts nothing.
 */
@Component
@RequiredArgsConstructor
public class LinkVisibilityScheduler {

    private static final Logger log = LoggerFactory.getLogger(LinkVisibilityScheduler.class);

    private final LinkRepository linkRepository;
    private final ProfileService profileService;

    @Value("${app.scheduling.link-visibility-ms:60000}")
    private long intervalMs;

    @Scheduled(
            fixedDelayString = "${app.scheduling.link-visibility-ms:60000}",
            initialDelayString = "${app.scheduling.link-visibility-initial-delay-ms:15000}")
    @Transactional(readOnly = true)
    public void evictProfilesCrossingVisibilityBoundary() {
        Instant now = Instant.now();
        // Look back further than the interval: a run delayed by GC or a slow query must not leave a
        // boundary in the gap between windows unhandled.
        Instant from = now.minus(Duration.ofMillis(intervalMs).multipliedBy(2));

        try {
            List<String> usernames = linkRepository.findUsernamesWithBoundaryBetween(from, now);
            if (usernames.isEmpty()) return;

            usernames.forEach(profileService::evictProfileCache);
            log.info("Link visibility sweep evicted {} profile(s) for boundaries in ({}, {}]",
                    usernames.size(), from, now);
        } catch (RuntimeException ex) {
            // Never let a transient database or cache failure kill the scheduled task; the next tick
            // covers the same window again.
            log.warn("Link visibility sweep failed, will retry on next tick: {}", ex.getMessage());
        }
    }
}
