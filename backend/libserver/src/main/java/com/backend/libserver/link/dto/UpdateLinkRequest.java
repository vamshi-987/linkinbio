package com.backend.libserver.link.dto;


import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;


public record UpdateLinkRequest(
        @NotBlank String title,
        @NotBlank @URL String url,
        boolean active
) {}
