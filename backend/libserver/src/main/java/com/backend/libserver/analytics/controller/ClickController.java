package com.backend.libserver.analytics.controller;


import com.backend.libserver.analytics.service.ClickService;
import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/click")
@RequiredArgsConstructor
public class ClickController {

    private final ClickService clickService;
    private final LinkRepository linkRepository;

    @GetMapping("/{linkId}")
    public ResponseEntity<Void> click(@PathVariable UUID linkId,
                                      @RequestHeader(value = "Referer", required = false) String referrer) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NoSuchElementException("Link not found"));

        clickService.recordClickAsync(linkId, referrer);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getUrl()))
                .build();
    }
}
