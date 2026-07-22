package com.backend.libserver.link.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;


/**
 * A full replacement of the editable fields: omitting a scheduling bound clears it, which is how the
 * dashboard expresses "no start" / "no end".
 */
public record UpdateLinkRequest(
        @NotBlank String title,
        @NotBlank @URL String url,
        boolean active,
        Instant visibleFrom,
        Instant visibleUntil
) {
    @JsonIgnore
    @AssertTrue(message = "visibleUntil must be after visibleFrom")
    public boolean isWindowOrdered() {
        return visibleFrom == null || visibleUntil == null || visibleUntil.isAfter(visibleFrom);
    }
}
