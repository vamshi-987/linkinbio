package com.backend.libserver.analytics.controller;


import com.backend.libserver.analytics.ClickContext;
import com.backend.libserver.analytics.service.ClickService;
import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.repository.LinkRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/click")
@RequiredArgsConstructor
public class ClickController {

    /**
     * Country headers set by the CDN in front of the app, in preference order. Whichever platform is
     * deployed to, one of these is already populated on every request.
     */
    private static final List<String> COUNTRY_HEADERS = List.of(
            "CF-IPCountry",              // Cloudflare
            "X-Vercel-IP-Country",       // Vercel
            "Fastly-Client-Country",     // Fastly
            "X-Geo-Country");            // generic / self-set

    private final ClickService clickService;
    private final LinkRepository linkRepository;

    @GetMapping("/{linkId}")
    public ResponseEntity<Void> click(@PathVariable UUID linkId, HttpServletRequest request) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NoSuchElementException("Link not found"));

        // A shared or bookmarked click URL outlives the page it came from, so the schedule has to be
        // enforced here too — the same 404 as an unknown id, so an expired link is not distinguishable
        // from one that never existed.
        if (!link.isVisibleAt(Instant.now())) {
            throw new NoSuchElementException("Link not found");
        }

        clickService.recordClickAsync(linkId, contextFrom(request));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getUrl()))
                .build();
    }

    /** Reads request-scoped data up front: the async recorder cannot touch the servlet request. */
    private ClickContext contextFrom(HttpServletRequest request) {
        return new ClickContext(
                request.getHeader("Referer"),
                request.getHeader("User-Agent"),
                clientIp(request),
                countryHint(request));
    }

    private String countryHint(HttpServletRequest request) {
        for (String header : COUNTRY_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return request.getRemoteAddr();
        // X-Forwarded-For is a chain: the left-most entry is the original client, the rest are proxies.
        return forwarded.split(",")[0].trim();
    }
}
