package com.backend.libserver.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per redirect. Append-only and never updated — dashboards read the rolled-up table instead
 * (see {@link ClickDailyRollup}), which is what keeps this table free to stay write-optimised.
 */
@Entity
@Table(name = "click_events")
@Getter
@Setter
@NoArgsConstructor
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    /** Raw Referer header, truncated to the column width; kept for spot-checking attribution. */
    @Column(length = 255)
    private String referrer;

    @Column(name = "referrer_host", nullable = false, length = 255)
    private String referrerHost;

    @Column(name = "device_type", nullable = false, length = 16)
    private String deviceType;

    @Column(nullable = false, length = 32)
    private String browser;

    @Column(nullable = false, length = 32)
    private String os;

    /** ISO-3166 alpha-2, or "XX" when the edge did not report one. */
    @Column(nullable = false, length = 2)
    private String country;
}
