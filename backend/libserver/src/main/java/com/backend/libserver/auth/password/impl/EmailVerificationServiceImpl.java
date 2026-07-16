package com.backend.libserver.auth.password.impl;

import com.backend.libserver.auth.AuthResponse;
import com.backend.libserver.auth.password.EmailVerificationService;
import com.backend.libserver.auth.password.OtpPurpose;
import com.backend.libserver.auth.password.OtpService;
import com.backend.libserver.security.JwtService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtService jwtService;

    @Override
    @Transactional
    public void sendCode(User user) {
        otpService.issue(user, OtpPurpose.EMAIL_VERIFICATION);
    }

    @Override
    @Transactional
    public void resendCode(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresentOrElse(user -> {
            if (user.isEmailVerified()) {
                log.info("Verification code requested for an already-verified account; ignoring.");
                return;
            }
            otpService.issue(user, OtpPurpose.EMAIL_VERIFICATION);
        }, () -> log.info("Verification code requested for unknown email; ignoring."));
    }

    @Override
    @Transactional
    public AuthResponse verify(String email, String otp) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        // Never short-circuit for an already-verified account: doing so would hand a token to
        // anyone who simply knows the address. A verified user logs in with their password.
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("This email is already verified. Please log in.");
        }

        otpService.verify(user, otp, OtpPurpose.EMAIL_VERIFICATION);

        user.setEmailVerified(true);
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername()),
                user.getUsername());
    }
}
