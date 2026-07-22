package com.backend.libserver.link.controller;


import com.backend.libserver.link.dto.CreateLinkRequest;
import com.backend.libserver.link.dto.LinkResponse;
import com.backend.libserver.link.dto.UpdateLinkRequest;
import com.backend.libserver.link.service.LinkService;
import com.backend.libserver.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping
    public List<LinkResponse> getMyLinks(@AuthenticationPrincipal UserPrincipal user) {
        return linkService.getLinksForUser(user.getId());
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                               @Valid @RequestBody CreateLinkRequest req) {
        return ResponseEntity.ok(linkService.create(user.getId(), req));
    }

    @PutMapping("/{id}")
    public LinkResponse update(@AuthenticationPrincipal UserPrincipal user,
                               @PathVariable UUID id,
                               @Valid @RequestBody UpdateLinkRequest req) {
        return linkService.update(user.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable UUID id) {
        linkService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Multipart rather than a JSON body with base64: base64 inflates the payload by a third and
     * would have to be buffered as a String before it could be validated.
     */
    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LinkResponse uploadThumbnail(@AuthenticationPrincipal UserPrincipal user,
                                        @PathVariable UUID id,
                                        @RequestParam("file") MultipartFile file) {
        return linkService.uploadThumbnail(user.getId(), id, file);
    }

    @DeleteMapping("/{id}/thumbnail")
    public LinkResponse deleteThumbnail(@AuthenticationPrincipal UserPrincipal user, @PathVariable UUID id) {
        return linkService.deleteThumbnail(user.getId(), id);
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(@AuthenticationPrincipal UserPrincipal user,
                                        @RequestBody List<UUID> orderedLinkIds) {
        linkService.reorder(user.getId(), orderedLinkIds);
        return ResponseEntity.noContent().build();
    }
}
