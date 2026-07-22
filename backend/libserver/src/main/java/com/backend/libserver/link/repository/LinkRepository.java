package com.backend.libserver.link.repository;

import com.backend.libserver.link.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {

    List<Link> findAllByUserIdOrderByPositionAsc(UUID userId);

    /**
     * Visibility is resolved in the query rather than filtered in Java afterwards, so a scheduled or
     * expired link never travels further than the database.
     */
    @Query("""
            SELECT l FROM Link l
            WHERE l.user.username = :username
              AND l.active = true
              AND (l.visibleFrom IS NULL OR l.visibleFrom <= :now)
              AND (l.visibleUntil IS NULL OR l.visibleUntil > :now)
            ORDER BY l.position ASC
            """)
    List<Link> findVisibleLinksByUsername(@Param("username") String username, @Param("now") Instant now);

    /**
     * Usernames whose links cross a scheduling boundary inside the window. The public profile is
     * cached, so nothing would change on screen at the scheduled minute unless these entries are
     * evicted.
     */
    @Query("""
            SELECT DISTINCT l.user.username FROM Link l
            WHERE (l.visibleFrom  > :from AND l.visibleFrom  <= :to)
               OR (l.visibleUntil > :from AND l.visibleUntil <= :to)
            """)
    List<String> findUsernamesWithBoundaryBetween(@Param("from") Instant from, @Param("to") Instant to);
}
