package com.backend.libserver.analytics;

/**
 * Everything about a click that only exists on the request thread. It is captured synchronously and
 * handed to the async recorder as an immutable value — reading headers off the servlet request from
 * a worker thread would race with the container recycling it.
 */
public record ClickContext(
        String referrer,
        String userAgent,
        String clientIp,
        /** Country supplied by the CDN/edge, if the deployment sits behind one. */
        String countryHint
) {
    public static ClickContext empty() {
        return new ClickContext(null, null, null, null);
    }
}
