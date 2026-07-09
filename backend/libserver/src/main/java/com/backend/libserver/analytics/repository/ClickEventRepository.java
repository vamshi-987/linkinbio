package com.backend.libserver.analytics.repository;

import com.backend.libserver.analytics.DailyClickProjection;
import com.backend.libserver.analytics.LinkClickProjection;
import com.backend.libserver.analytics.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent,Long> {

    @Query(value = """
        SELECT DATE(clicked_at) as day, COUNT(*) as clicks
        FROM click_events
        WHERE link_id IN (SELECT id FROM links WHERE user_id = :userId)
        AND clicked_at >= :since
        GROUP BY DATE(clicked_at)
        ORDER BY day
        """, nativeQuery = true)
    List<DailyClickProjection> getDailyClicks(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query(value = """
        SELECT l.id as linkId, l.title as title, COUNT(c.id) as clicks
        FROM links l
        LEFT JOIN click_events c ON c.link_id = l.id
        WHERE l.user_id = :userId
        GROUP BY l.id, l.title
        ORDER BY clicks DESC
        """, nativeQuery = true)
    List<LinkClickProjection> getClicksPerLink(@Param("userId") UUID userId);

}
