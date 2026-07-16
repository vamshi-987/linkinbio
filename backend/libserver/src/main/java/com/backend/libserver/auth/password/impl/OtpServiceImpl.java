package com.backend.libserver.auth.password.impl;

import com.backend.libserver.auth.password.EmailOtp;
import com.backend.libserver.auth.password.EmailOtpRepository;
import com.backend.libserver.auth.password.EmailService;
import com.backend.libserver.auth.password.OtpPurpose;
import com.backend.libserver.auth.password.OtpService;
import com.backend.libserver.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final EmailOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final long expiryMinutes;
    private final int maxAttempts;
    private final long cooldownSeconds;
    private final int maxPerHour;

    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(EmailOtpRepository otpRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          @Value("${app.otp.expiry-minutes:10}") long expiryMinutes,
                          @Value("${app.otp.max-attempts:5}") int maxAttempts,
                          @Value("${app.otp.resend-cooldown-seconds:60}") long cooldownSeconds,
                          @Value("${app.otp.max-per-hour:5}") int maxPerHour) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.cooldownSeconds = cooldownSeconds;
        this.maxPerHour = maxPerHour;
    }

    /**
     * Deliberately joins the caller's transaction rather than starting its own: signup issues a code
     * in the same transaction that inserts the user, and a separate transaction could not see that
     * not-yet-committed row to satisfy the email_otps -> users foreign key. Callers that throw after
     * issuing must declare {@code noRollbackFor} so the emailed code survives (see AuthService#login).
     */
    @Override
    @Transactional
    public void issue(User user, OtpPurpose purpose) {
        // Before consuming anything: a throttled request must leave the user's existing code intact,
        // otherwise an attacker could invalidate codes on demand without ever receiving one.
        if (isThrottled(user, purpose)) {
            return;
        }

        otpRepository.consumeAllForUser(user.getId(), purpose);

        String otp = generateOtp();
        EmailOtp record = new EmailOtp();
        record.setUserId(user.getId());
        record.setPurpose(purpose);
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        otpRepository.save(record);

        emailService.sendOtp(user.getEmail(), otp, expiryMinutes, purpose);
    }

    /**
     * Runs in its own transaction and does not roll back on the exceptions it throws: the attempt
     * counter and the consumed flag must survive a rejection, otherwise the attempt limit below could
     * never be reached and codes would be brute-forceable. Safe to start a new transaction here
     * because this only touches rows that were committed by an earlier request.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = IllegalArgumentException.class)
    public void verify(User user, String otp, OtpPurpose purpose) {
        EmailOtp record = otpRepository
                .findTopByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        if (record.getExpiresAt().isBefore(Instant.now())) {
            record.setConsumed(true);
            otpRepository.save(record);
            throw new IllegalArgumentException("Invalid or expired code");
        }

        if (record.getAttempts() >= maxAttempts) {
            record.setConsumed(true);
            otpRepository.save(record);
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code.");
        }

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            otpRepository.save(record);
            throw new IllegalArgumentException("Invalid or expired code");
        }

        record.setConsumed(true);
        otpRepository.save(record);
    }

    /**
     * Caps how many codes a single address can be sent, independently of the per-IP filter — that
     * one is defeated by rotating IPs, and this is what actually stops someone looping
     * forgot-password to flood a victim's inbox from our mail account.
     *
     * <p>It is also what makes a 6-digit code safe: {@link #verify} allows {@code maxAttempts}
     * guesses per code, so without a ceiling on fresh codes an attacker could keep requesting new
     * ones and work through the keyspace. The two limits together cap guesses per hour.
     *
     * <p>Throttling is silent — callers return the same generic "if an account exists…" message
     * either way, so a 429 here would tell an attacker which addresses are registered.
     */
    private boolean isThrottled(User user, OtpPurpose purpose) {
        Instant now = Instant.now();

        boolean withinCooldown = otpRepository
                .findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(last -> last.getCreatedAt().isAfter(now.minusSeconds(cooldownSeconds)))
                .isPresent();
        if (withinCooldown) {
            log.info("Suppressed {} code: another was issued less than {}s ago.", purpose, cooldownSeconds);
            return true;
        }

        long lastHour = otpRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                user.getId(), purpose, now.minus(1, ChronoUnit.HOURS));
        if (lastHour >= maxPerHour) {
            log.warn("Suppressed {} code: {} already issued in the last hour.", purpose, lastHour);
            return true;
        }

        return false;
    }

    private String generateOtp() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
