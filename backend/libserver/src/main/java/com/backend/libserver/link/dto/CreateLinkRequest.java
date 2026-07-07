package com.backend.libserver.link.dto;


import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;


public record CreateLinkRequest(
        @NotBlank String title,
        @NotBlank @URL String url
) {}
