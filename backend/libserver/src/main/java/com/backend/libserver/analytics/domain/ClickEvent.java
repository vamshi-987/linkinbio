package com.backend.libserver.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

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

    private String referrer;

    public ClickEvent(UUID linkId, Instant clickedAt, String referrer) {
        this.linkId = linkId;
        this.clickedAt = clickedAt;
        this.referrer = referrer;
    }
}
