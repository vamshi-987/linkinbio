package com.backend.libserver.auth;


import com.backend.libserver.auth.password.EmailVerificationService;
import com.backend.libserver.security.JwtService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Creates the account in an unverified state and emails a code. No token is issued here — the
     * caller must confirm the code via verify-email before the account can be used.
     */
    @Transactional
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

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setEmailVerified(false);
        user = userRepository.save(user);

        emailVerificationService.sendCode(user);
    }

    // Rejecting an unverified account throws *after* a fresh code has been issued; without this the
    // rollback would delete that code and the email would contain a code that could never work.
    @Transactional(noRollbackFor = EmailNotVerifiedException.class)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Credentials are good but the address was never confirmed: send a fresh code and tell the
        // client to route the user to the verification step.
        if (!user.isEmailVerified()) {
            emailVerificationService.sendCode(user);
            throw new EmailNotVerifiedException(user.getEmail());
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }
}
