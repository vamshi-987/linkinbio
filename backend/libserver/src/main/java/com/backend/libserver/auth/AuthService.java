package com.backend.libserver.auth;


import com.backend.libserver.auth.password.EmailVerificationService;
import com.backend.libserver.security.JwtService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "api", "login", "signup", "dashboard", "settings", "public", "click", "analytics"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Stages the signup and emails a code. Nothing is written to the users table here — the account
     * is only created once the code is confirmed via verify-email (see EmailVerificationService), so
     * an unverified account never exists in the database. No token is issued yet.
     */
    public void signup(SignupRequest req) {
        String username = req.username().toLowerCase();

        if (RESERVED_USERNAMES.contains(username)) {
            throw new IllegalArgumentException("This username is reserved");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        String email = req.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        emailVerificationService.startSignup(username, email, req.password());
    }

    public AuthResponse login(LoginRequest req) {
        String identifier = req.identifier().trim();

        User user = findByIdentifier(identifier).orElse(null);
        if (user == null) {
            // Nothing registered under that identifier — but there may be a pending (unverified)
            // signup. Only reveal that, and resend its code, once the password matches, so this
            // never leaks who has a signup in flight. Otherwise it is indistinguishable from wrong
            // credentials.
            pendingSignupFor(identifier)
                    .filter(pending -> passwordEncoder.matches(req.password(), pending.passwordHash()))
                    .ifPresent(pending -> {
                        emailVerificationService.resendCode(pending.email());
                        throw new EmailNotVerifiedException(pending.email());
                    });
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }

    /**
     * Usernames cannot contain "@" (enforced by the signup pattern), so the symbol is a reliable
     * discriminator: no input can ever match both a username and an email address.
     */
    private boolean looksLikeEmail(String identifier) {
        return identifier.indexOf('@') >= 0;
    }

    private Optional<User> findByIdentifier(String identifier) {
        return looksLikeEmail(identifier)
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByUsername(identifier.toLowerCase());
    }

    private Optional<PendingSignup> pendingSignupFor(String identifier) {
        return looksLikeEmail(identifier)
                ? emailVerificationService.pendingSignupByEmail(identifier)
                : emailVerificationService.pendingSignupByUsername(identifier.toLowerCase());
    }
}
