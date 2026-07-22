package com.backend.libserver.analytics.repository;

import com.backend.libserver.analytics.BreakdownProjection;
import com.backend.libserver.analytics.domain.ClickDailyRollup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClickDailyRollupRepository extends JpaRepository<ClickDailyRollup, Long> {

    /**
     * Recomputes one UTC day from the raw events and upserts it.
     *
     * <p>Written as a single INSERT … ON CONFLICT DO UPDATE deliberately: the count is replaced, not
     * incremented, so running this twice for the same day — a retry, a redeploy mid-run, two app
     * instances, a manual backfill — produces exactly the same rows. Aggregating in the database
     * also means the events never travel to the app just to be counted.
     */
    @Modifying
    @Query(value = """
            INSERT INTO click_daily_rollups (user_id, link_id, day, country, device_type, clicks, updated_at)
            SELECT l.user_id, c.link_id, DATE(c.clicked_at), c.country, c.device_type, COUNT(*), now()
            FROM click_events c
            JOIN links l ON l.id = c.link_id
            WHERE c.clicked_at >= CAST(:day AS date)
              AND c.clicked_at <  CAST(:day AS date) + INTERVAL '1 day'
            GROUP BY l.user_id, c.link_id, DATE(c.clicked_at), c.country, c.device_type
            ON CONFLICT (user_id, link_id, day, country, device_type)
            DO UPDATE SET clicks = EXCLUDED.clicks, updated_at = now()
            """, nativeQuery = true)
    int rebuildDay(@Param("day") LocalDate day);

    @Query(value = """
            SELECT country AS label, SUM(clicks) AS clicks
            FROM click_daily_rollups
            WHERE user_id = :userId AND day >= :from
            GROUP BY country
            ORDER BY clicks DESC, label ASC
            """, nativeQuery = true)
    List<BreakdownProjection> countryBreakdown(@Param("userId") UUID userId, @Param("from") LocalDate from);

    @Query(value = """
            SELECT device_type AS label, SUM(clicks) AS clicks
            FROM click_daily_rollups
            WHERE user_id = :userId AND day >= :from
            GROUP BY device_type
            ORDER BY clicks DESC, label ASC
            """, nativeQuery = true)
    List<BreakdownProjection> deviceBreakdown(@Param("userId") UUID userId, @Param("from") LocalDate from);

    @Query(value = """
            SELECT COALESCE(SUM(clicks), 0)
            FROM click_daily_rollups
            WHERE user_id = :userId AND day >= :from
            """, nativeQuery = true)
    long totalClicksSince(@Param("userId") UUID userId, @Param("from") LocalDate from);

    @Query(value = "SELECT MAX(day) FROM click_daily_rollups WHERE user_id = :userId", nativeQuery = true)
    LocalDate lastRolledUpDay(@Param("userId") UUID userId);
}
