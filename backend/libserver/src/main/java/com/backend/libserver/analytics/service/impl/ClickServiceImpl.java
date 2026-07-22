package com.backend.libserver.analytics.service.impl;

import com.backend.libserver.analytics.ClickContext;
import com.backend.libserver.analytics.domain.ClickEvent;
import com.backend.libserver.analytics.enrich.DeviceInfo;
import com.backend.libserver.analytics.enrich.GeoResolver;
import com.backend.libserver.analytics.enrich.ReferrerParser;
import com.backend.libserver.analytics.enrich.UserAgentParser;
import com.backend.libserver.analytics.repository.ClickEventRepository;
import com.backend.libserver.analytics.service.ClickService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClickServiceImpl implements ClickService {

    private static final Logger log = LoggerFactory.getLogger(ClickServiceImpl.class);

    /** Matches the click_events.referrer column width. */
    private static final int MAX_REFERRER_LENGTH = 255;

    private final ClickEventRepository clickEventRepository;
    private final UserAgentParser userAgentParser;
    private final ReferrerParser referrerParser;
    private final GeoResolver geoResolver;

    @Async("clickExecutor")
    public void recordClickAsync(UUID linkId, ClickContext context) {
        try {
            ClickContext ctx = context == null ? ClickContext.empty() : context;
            DeviceInfo device = userAgentParser.parse(ctx.userAgent());

            ClickEvent event = new ClickEvent();
            event.setLinkId(linkId);
            event.setClickedAt(Instant.now());
            event.setReferrer(truncate(ctx.referrer()));
            event.setReferrerHost(referrerParser.toHost(ctx.referrer()));
            event.setDeviceType(device.deviceType());
            event.setBrowser(device.browser());
            event.setOs(device.os());
            event.setCountry(geoResolver.resolveCountry(ctx));

            clickEventRepository.save(event);
        } catch (RuntimeException ex) {
            // The visitor has already been redirected, so there is nobody left to report this to.
            // Analytics are best-effort by design: losing a click must never look like an outage.
            log.warn("Failed to record click for link {}: {}", linkId, ex.getMessage());
        }
    }

    private String truncate(String referrer) {
        if (referrer == null) return null;
        return referrer.length() > MAX_REFERRER_LENGTH ? referrer.substring(0, MAX_REFERRER_LENGTH) : referrer;
    }
}
