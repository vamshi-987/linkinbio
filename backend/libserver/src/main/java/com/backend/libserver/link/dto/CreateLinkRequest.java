package com.backend.libserver.link.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;


/**
 * Scheduling bounds are optional and sent as ISO-8601 instants (e.g. 2026-08-01T09:00:00Z), so the
 * viewer's local time zone is resolved in the browser and the server only ever stores UTC.
 */
public record CreateLinkRequest(
        @NotBlank String title,
        @NotBlank @URL String url,
        Instant visibleFrom,
        Instant visibleUntil
) {
    @JsonIgnore
    @AssertTrue(message = "visibleUntil must be after visibleFrom")
    public boolean isWindowOrdered() {
        return visibleFrom == null || visibleUntil == null || visibleUntil.isAfter(visibleFrom);
    }
}
