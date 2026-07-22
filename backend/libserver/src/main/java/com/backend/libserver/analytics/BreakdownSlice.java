package com.backend.libserver.analytics;

/** One row of a grouped breakdown, e.g. ("mobile", 412) or ("IN", 89). */
public record BreakdownSlice(String label, long clicks) {}
