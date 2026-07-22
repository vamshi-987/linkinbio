package com.backend.libserver.analytics.enrich;

import com.backend.libserver.analytics.ClickContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClickEnrichmentTest {

    private static final String IPHONE_SAFARI =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1";
    private static final String WINDOWS_EDGE =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";

    private final UserAgentParser userAgents = new UserAgentParser();
    private final ReferrerParser referrers = new ReferrerParser();
    private final HeaderGeoResolver geo = new HeaderGeoResolver();

    @Test
    void classifiesMobileSafari() {
        DeviceInfo info = userAgents.parse(IPHONE_SAFARI);

        assertThat(info.deviceType()).isEqualTo("mobile");
        assertThat(info.browser()).isEqualTo("safari");
        assertThat(info.os()).isEqualTo("ios");
    }

    /** Edge advertises both Chrome and Safari, so ordering inside the parser is what is under test. */
    @Test
    void prefersTheMostSpecificBrowserToken() {
        assertThat(userAgents.parse(WINDOWS_EDGE).browser()).isEqualTo("edge");
        assertThat(userAgents.parse(WINDOWS_EDGE).deviceType()).isEqualTo("desktop");
    }

    @Test
    void missingUserAgentIsUnknownRatherThanAGuess() {
        assertThat(userAgents.parse(null)).isEqualTo(DeviceInfo.UNKNOWN);
        assertThat(userAgents.parse("  ")).isEqualTo(DeviceInfo.UNKNOWN);
    }

    @Test
    void reducesReferrerToItsHostAndDropsTheQueryString() {
        assertThat(referrers.toHost("https://www.instagram.com/p/abc?utm_source=bio"))
                .isEqualTo("instagram.com");
    }

    @Test
    void unusableReferrersBucketAsDirect() {
        assertThat(referrers.toHost(null)).isEqualTo(ReferrerParser.DIRECT);
        assertThat(referrers.toHost("not a url")).isEqualTo(ReferrerParser.DIRECT);
    }

    @Test
    void takesCountryFromTheEdgeHeader() {
        assertThat(geo.resolveCountry(new ClickContext(null, null, "1.2.3.4", "in"))).isEqualTo("IN");
    }

    @Test
    void rejectsAnythingThatIsNotACountryCode() {
        assertThat(geo.resolveCountry(new ClickContext(null, null, null, "INDIA")))
                .isEqualTo(GeoResolver.UNKNOWN);
        assertThat(geo.resolveCountry(ClickContext.empty())).isEqualTo(GeoResolver.UNKNOWN);
    }
}
