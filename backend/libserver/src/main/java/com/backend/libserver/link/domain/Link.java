package com.backend.libserver.link.domain;


import com.backend.libserver.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Link {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private int position;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** Storage key behind {@link #thumbnailUrl}; kept so a replacement can delete the old object. */
    @Column(name = "thumbnail_key", length = 255)
    private String thumbnailKey;

    /** Inclusive start of the scheduled window; null means "live as soon as it is active". */
    @Column(name = "visible_from")
    private Instant visibleFrom;

    /** Exclusive end of the scheduled window; null means "never expires". */
    @Column(name = "visible_until")
    private Instant visibleUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * Whether a visitor should see this link at {@code now}. Kept next to the fields so the public
     * profile query, the click redirect and the owner's dashboard all agree on one definition of
     * "live" — the bounds are compared in UTC, independent of the viewer's or server's zone.
     */
    public boolean isVisibleAt(Instant now) {
        if (!active) return false;
        if (visibleFrom != null && now.isBefore(visibleFrom)) return false;
        return visibleUntil == null || now.isBefore(visibleUntil);
    }
}
