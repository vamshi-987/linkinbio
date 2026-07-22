package com.backend.libserver.auth.password.impl;

import com.backend.libserver.auth.AuthResponse;
import com.backend.libserver.auth.PendingSignup;
import com.backend.libserver.auth.PendingSignupStore;
import com.backend.libserver.auth.password.EmailService;
import com.backend.libserver.auth.password.EmailVerificationService;
import com.backend.libserver.auth.password.OtpPurpose;
import com.backend.libserver.security.JwtService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Signup email verification backed by {@link PendingSignupStore}: the account exists only in Redis
 * until the code is confirmed, so no unverified row is ever written to the database.
 *
 * <p>The OTP protections here mirror {@code OtpServiceImpl} (which serves real users' resets):
 * a per-code attempt cap, a resend cooldown, and an hourly ceiling on codes per email.
 */
@Service
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final PendingSignupStore pendingStore;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final long expiryMinutes;
    private final int maxAttempts;
    private final long cooldownSeconds;
    private final int maxPerHour;

    private final SecureRandom random = new SecureRandom();

    public EmailVerificationServiceImpl(PendingSignupStore pendingStore,
                                        PasswordEncoder passwordEncoder,
                                        EmailService emailService,
                                        UserRepository userRepository,
                                        JwtService jwtService,
                                        @Value("${app.otp.expiry-minutes:10}") long expiryMinutes,
                                        @Value("${app.otp.max-attempts:5}") int maxAttempts,
                                        @Value("${app.otp.resend-cooldown-seconds:60}") long cooldownSeconds,
                                        @Value("${app.otp.max-per-hour:5}") int maxPerHour) {
        this.pendingStore = pendingStore;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.cooldownSeconds = cooldownSeconds;
        this.maxPerHour = maxPerHour;
    }

    @Override
    public void startSignup(String username, String email, String rawPassword) {
        // A throttled request must leave any existing pending signup (and its code) untouched.
        if (isThrottled(email, pendingStore.find(email).orElse(null))) {
            return;
        }
        issueCode(username, email, passwordEncoder.encode(rawPassword));
    }

    @Override
    public void resendCode(String email) {
        pendingStore.find(email).ifPresentOrElse(existing -> {
            if (isThrottled(email, existing)) {
                return;
            }
            issueCode(existing.username(), existing.email(), existing.passwordHash());
        }, () -> log.info("Verification code requested for an address with no pending signup; ignoring."));
    }

    /** Generates and stores a fresh code for the pending signup, then emails it. */
    private void issueCode(String username, String email, String passwordHash) {
        String otp = generateOtp();
        PendingSignup pending = new PendingSignup(
                username, email, passwordHash, passwordEncoder.encode(otp),
                Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES), 0, Instant.now());

        pendingStore.save(pending);
        pendingStore.incrementHourlyCount(email);
        emailService.sendOtp(email, otp, expiryMinutes, OtpPurpose.EMAIL_VERIFICATION);
    }

    @Override
    @Transactional
    public AuthResponse verify(String email, String otp) {
        PendingSignup pending = pendingStore.find(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        if (pending.otpExpiresAt().isBefore(Instant.now())) {
            pendingStore.delete(pending);
            throw new IllegalArgumentException("Invalid or expired code");
        }
        if (pending.attempts() >= maxAttempts) {
            pendingStore.delete(pending);
            throw new IllegalArgumentException("Too many incorrect attempts. Please sign up again.");
        }
        if (!passwordEncoder.matches(otp, pending.otpHash())) {
            pendingStore.save(pending.withFailedAttempt());
            throw new IllegalArgumentException("Invalid or expired code");
        }

        User user = createVerifiedUser(pending);
        pendingStore.delete(pending);
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername()),
                user.getUsername());
    }

    /**
     * Inserts the now-verified user. The username/email were free when the signup started, but could
     * have been taken by another signup that verified first — the unique constraints are the source
     * of truth, so a collision here becomes a clean "please sign up again" rather than a 500.
     */
    private User createVerifiedUser(PendingSignup pending) {
        if (userRepository.existsByUsername(pending.username())
                || userRepository.existsByEmailIgnoreCase(pending.email())) {
            pendingStore.delete(pending);
            throw new IllegalArgumentException("That username or email was just registered. Please sign up again.");
        }

        User user = new User();
        user.setUsername(pending.username());
        user.setEmail(pending.email());
        user.setPasswordHash(pending.passwordHash());
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            pendingStore.delete(pending);
            throw new IllegalArgumentException("That username or email was just registered. Please sign up again.");
        }
    }

    @Override
    public Optional<PendingSignup> pendingSignupByUsername(String username) {
        return pendingStore.findByUsername(username);
    }

    /**
     * True when a fresh code must be suppressed: within the resend cooldown of the last one, or past
     * the hourly ceiling for this email. Silent — callers behave identically either way so this
     * cannot reveal whether an address has a pending signup.
     */
    private boolean isThrottled(String email, PendingSignup existing) {
        Instant now = Instant.now();

        if (existing != null && existing.issuedAt().isAfter(now.minusSeconds(cooldownSeconds))) {
            log.info("Suppressed verification code: another was issued less than {}s ago.", cooldownSeconds);
            return true;
        }
        if (pendingStore.hourlyCount(email) >= maxPerHour) {
            log.warn("Suppressed verification code: hourly ceiling of {} reached.", maxPerHour);
            return true;
        }
        return false;
    }

    private String generateOtp() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
