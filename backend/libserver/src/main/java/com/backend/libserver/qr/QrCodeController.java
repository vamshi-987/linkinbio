package com.backend.libserver.qr;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * QR codes for a profile page and for individual links. Public by design — a QR code carries no more
 * information than the URL it encodes, all of which is already public.
 */
@RestController
@RequestMapping("/api/public/qr")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping(value = "/profile/{username}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> profileQr(@PathVariable String username,
                                            @RequestParam(defaultValue = "512") int size) {
        String normalised = username.toLowerCase(Locale.ROOT);
        if (!userRepository.existsByUsername(normalised)) {
            throw new NoSuchElementException("Profile not found");
        }

        String target = frontendUrl.replaceAll("/+$", "") + "/" + normalised;
        return png(qrCodeService.pngFor("profile:" + normalised, target, size), normalised);
    }

    @GetMapping(value = "/link/{linkId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> linkQr(@PathVariable UUID linkId,
                                         @RequestParam(defaultValue = "512") int size) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NoSuchElementException("Link not found"));
        if (!link.isVisibleAt(Instant.now())) {
            throw new NoSuchElementException("Link not found");
        }

        // Encode the tracking redirect, not the destination, so scans are counted like any other
        // click and the owner can change the destination without reprinting the code.
        String target = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/public/click/{id}")
                .buildAndExpand(linkId)
                .toUriString();

        return png(qrCodeService.pngFor("link:" + linkId, target, size), "link-" + linkId);
    }

    private ResponseEntity<byte[]> png(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                // The code only changes if the underlying URL does, so let browsers and any CDN in
                // front of the app hold onto it rather than round-tripping for identical bytes.
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                .header("Content-Disposition", "inline; filename=\"" + filename + "-qr.png\"")
                .body(body);
    }
}
