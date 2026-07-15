package com.backend.libserver.analytics;

import java.util.UUID;

public record LinkClick(UUID linkId, String title, long clicks) {}
