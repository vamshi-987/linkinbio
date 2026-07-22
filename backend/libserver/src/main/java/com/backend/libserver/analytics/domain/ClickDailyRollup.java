package com.backend.libserver.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pre-aggregated clicks at (user, link, day, country, device) grain — the read side of the split.
 *
 * <p>Clicks arrive one row at a time and are never read individually; dashboards always ask for
 * grouped counts over a window. Recomputing those groups from {@link ClickEvent} on every dashboard
 * load scales with total traffic, whereas reading here scales with days × links.
 */
@Entity
@Table(name = "click_daily_rollups")
@Getter
@Setter
@NoArgsConstructor
public class ClickDailyRollup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(nullable = false)
    private LocalDate day;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(name = "device_type", nullable = false, length = 16)
    private String deviceType;

    @Column(nullable = false)
    private long clicks;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
