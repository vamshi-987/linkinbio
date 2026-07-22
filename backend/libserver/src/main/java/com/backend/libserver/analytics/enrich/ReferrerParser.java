package com.backend.libserver.analytics.enrich;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

/**
 * Reduces a Referer header to the host that sent the visitor.
 *
 * <p>Full referrer URLs carry query strings — campaign ids, search terms, occasionally session
 * tokens — which are useless for a "where did my traffic come from" breakdown and are personal data
 * we would rather not store. Grouping by host also keeps the rollup's cardinality sane.
 */
@Component
public class ReferrerParser {

    public static final String DIRECT = "direct";

    private static final int MAX_HOST_LENGTH = 255;

    public String toHost(String referrer) {
        if (referrer == null || referrer.isBlank()) return DIRECT;

        try {
            String host = URI.create(referrer.trim()).getHost();
            if (host == null || host.isBlank()) return DIRECT;

            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            return host.length() > MAX_HOST_LENGTH ? host.substring(0, MAX_HOST_LENGTH) : host;
        } catch (IllegalArgumentException ex) {
            // A malformed Referer is attacker-controlled input, not an error worth failing a click
            // over — bucket it with the unattributable traffic.
            return DIRECT;
        }
    }
}
