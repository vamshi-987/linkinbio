package com.backend.libserver.analytics;

import java.time.LocalDate;

public interface DailyClickProjection {
    LocalDate getDay();
    Long getClicks();
}
