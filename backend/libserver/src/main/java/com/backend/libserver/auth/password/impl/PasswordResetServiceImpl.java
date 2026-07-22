package com.backend.libserver.auth.password.impl;

import com.backend.libserver.auth.password.EmailOtpRepository;
import com.backend.libserver.auth.password.OtpPurpose;
import com.backend.libserver.auth.password.OtpService;
import com.backend.libserver.auth.password.PasswordResetService;
import com.backend.libserver.security.JwtService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final EmailOtpRepository otpRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long resetTokenMinutes;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    EmailOtpRepository otpRepository,
                                    OtpService otpService,
                                    PasswordEncoder passwordEncoder,
                                    JwtService jwtService,
                                    @Value("${app.otp.reset-token-minutes:15}") long resetTokenMinutes) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resetTokenMinutes = resetTokenMinutes;
    }

    @Override
    @Transactional
    public void requestReset(String username) {
        userRepository.findByUsername(username.trim()).ifPresentOrElse(
                user -> otpService.issue(user, OtpPurpose.PASSWORD_RESET),
                () -> log.info("Password reset requested for unknown username; ignoring."));
    }

    @Override
    @Transactional
    public String verifyOtp(String username, String otp) {
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        otpService.verify(user, otp, OtpPurpose.PASSWORD_RESET);
        return jwtService.generatePasswordResetToken(user.getId(), resetTokenMinutes);
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        UUID userId;
        try {
            userId = jwtService.extractPasswordResetUserId(resetToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("Your reset session has expired. Please start again.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Your reset session has expired. Please start again."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Belt and braces: drop any leftover reset codes for this user.
        otpRepository.consumeAllForUser(user.getId(), OtpPurpose.PASSWORD_RESET);
    }
}
