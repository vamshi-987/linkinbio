package com.backend.libserver.analytics.enrich;

/** Device facts derived from a User-Agent string. Values are lower-case and column-width bounded. */
public record DeviceInfo(String deviceType, String browser, String os) {

    public static final DeviceInfo UNKNOWN = new DeviceInfo("unknown", "unknown", "unknown");
}
