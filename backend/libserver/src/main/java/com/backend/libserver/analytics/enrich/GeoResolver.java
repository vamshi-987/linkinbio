package com.backend.libserver.analytics.enrich;

import com.backend.libserver.analytics.ClickContext;

/**
 * Resolves a click to an ISO-3166 alpha-2 country code, or {@link #UNKNOWN} when it cannot be
 * determined. Kept as an interface so a MaxMind/IP2Location lookup can replace the header-based
 * implementation without touching the ingestion path.
 */
public interface GeoResolver {

    String UNKNOWN = "XX";

    String resolveCountry(ClickContext context);
}
