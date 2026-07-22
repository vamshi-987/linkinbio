package com.backend.libserver.analytics.enrich;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Minimal User-Agent classification.
 *
 * <p>A full UA database is a large dependency that needs regular updates; the dashboard only reports
 * broad buckets (mobile vs desktop, top browsers), so substring matching in a deliberate order is
 * enough. Order matters: Edge and Opera both claim "Chrome", and Chrome claims "Safari", so the more
 * specific token has to be tested first.
 */
@Component
public class UserAgentParser {

    public DeviceInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return DeviceInfo.UNKNOWN;

        String ua = userAgent.toLowerCase(Locale.ROOT);
        return new DeviceInfo(deviceType(ua), browser(ua), os(ua));
    }

    private String deviceType(String ua) {
        // Crawlers are classified rather than dropped: a spike of bot traffic is worth seeing, and
        // silently discarding rows would make the raw and rolled-up totals disagree.
        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")
                || ua.contains("preview") || ua.contains("headlesschrome")) {
            return "bot";
        }
        if (ua.contains("ipad") || ua.contains("tablet")
                || (ua.contains("android") && !ua.contains("mobile"))) {
            return "tablet";
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("ipod")
                || ua.contains("android") || ua.contains("windows phone")) {
            return "mobile";
        }
        return "desktop";
    }

    private String browser(String ua) {
        if (ua.contains("edg/") || ua.contains("edga") || ua.contains("edgios")) return "edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "opera";
        if (ua.contains("samsungbrowser")) return "samsung";
        if (ua.contains("firefox") || ua.contains("fxios")) return "firefox";
        if (ua.contains("chrome") || ua.contains("crios")) return "chrome";
        if (ua.contains("safari")) return "safari";
        return "other";
    }

    private String os(String ua) {
        if (ua.contains("windows")) return "windows";
        if (ua.contains("android")) return "android";
        // "like Mac OS X" appears on iOS too, so the iOS devices have to be matched first.
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) return "ios";
        if (ua.contains("mac os")) return "macos";
        if (ua.contains("cros")) return "chromeos";
        if (ua.contains("linux")) return "linux";
        return "other";
    }
}
