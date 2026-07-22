package com.backend.libserver.link;

import com.backend.libserver.link.domain.Link;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LinkVisibilityTest {

    private static final Instant NOW = Instant.parse("2026-07-21T12:00:00Z");

    @Test
    void unscheduledActiveLinkIsVisible() {
        assertThat(link(null, null, true).isVisibleAt(NOW)).isTrue();
    }

    @Test
    void inactiveLinkIsHiddenEvenInsideItsWindow() {
        Link link = link(NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(1)), false);
        assertThat(link.isVisibleAt(NOW)).isFalse();
    }

    @Test
    void linkIsHiddenBeforeItsStart() {
        assertThat(link(NOW.plusSeconds(1), null, true).isVisibleAt(NOW)).isFalse();
    }

    @Test
    void startIsInclusive() {
        assertThat(link(NOW, null, true).isVisibleAt(NOW)).isTrue();
    }

    @Test
    void endIsExclusive() {
        assertThat(link(null, NOW, true).isVisibleAt(NOW)).isFalse();
        assertThat(link(null, NOW.plusSeconds(1), true).isVisibleAt(NOW)).isTrue();
    }

    private Link link(Instant from, Instant until, boolean active) {
        Link link = new Link();
        link.setActive(active);
        link.setVisibleFrom(from);
        link.setVisibleUntil(until);
        return link;
    }
}
