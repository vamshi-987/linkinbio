package com.backend.libserver.analytics.enrich;

import com.backend.libserver.analytics.ClickContext;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Takes the country from the edge that already resolved it — Cloudflare's {@code CF-IPCountry},
 * Vercel's {@code X-Vercel-IP-Country} and similar headers are set by the proxy in front of the app.
 *
 * <p>This avoids shipping and refreshing a GeoIP database for what the platform computes for free.
 * Behind no such proxy the header is absent and the click is recorded as {@link #UNKNOWN} rather
 * than guessed: an "unknown" bucket in the dashboard is honest, an invented country is not.
 *
 * <p>The header is only trusted because it is stripped and re-set by the proxy. If the app is ever
 * exposed directly, a client could forge it — which is exactly why this is an interface: swap in a
 * resolver that reads {@link ClickContext#clientIp()} against a GeoIP database instead.
 */
@Component
public class HeaderGeoResolver implements GeoResolver {

    @Override
    public String resolveCountry(ClickContext context) {
        String hint = context == null ? null : context.countryHint();
        if (hint == null) return UNKNOWN;

        String code = hint.trim().toUpperCase(Locale.ROOT);
        // Cloudflare sends "T1" for Tor and "XX" when it cannot tell; both are already non-countries.
        if (code.length() != 2 || !code.chars().allMatch(Character::isLetter)) return UNKNOWN;
        return code;
    }
}
