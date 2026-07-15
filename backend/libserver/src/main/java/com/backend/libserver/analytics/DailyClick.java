package com.backend.libserver.analytics;

import java.time.LocalDate;

public record DailyClick(LocalDate day, long clicks) {}
