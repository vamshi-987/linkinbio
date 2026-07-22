package com.backend.libserver.auth;


import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * Credentials for login. The identifier is either a username or an email address: usernames are
 * restricted to letters, digits and underscores at signup, so an "@" tells the two apart with no
 * ambiguity and neither can be mistaken for the other.
 *
 * <p>The former field name is still accepted, so a client built before this change keeps working
 * against a newer server.
 */
public record LoginRequest(
        @JsonAlias("username")
        @NotBlank(message = "Enter your username or email")
        String identifier,

        @NotBlank String password
) {}
