package com.backend.libserver.analytics;

import java.util.UUID;

public interface LinkClickProjection {
    UUID getLinkId();
    String getTitle();
    Long getClicks();
}
