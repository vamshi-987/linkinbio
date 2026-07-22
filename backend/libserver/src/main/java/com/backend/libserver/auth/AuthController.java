package com.backend.libserver.auth;

import com.backend.libserver.auth.password.EmailVerificationService;
import com.backend.libserver.auth.password.PasswordResetService;
import com.backend.libserver.auth.password.dto.ForgotPasswordRequest;
import com.backend.libserver.auth.password.dto.MessageResponse;
import com.backend.libserver.auth.password.dto.ResendCodeRequest;
import com.backend.libserver.auth.password.dto.ResetPasswordRequest;
import com.backend.libserver.auth.password.dto.VerifyEmailRequest;
import com.backend.libserver.auth.password.dto.VerifyOtpRequest;
import com.backend.libserver.auth.password.dto.VerifyOtpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest req) {
        authService.signup(req);
        return ResponseEntity.ok(new MessageResponse(
                "We've sent a verification code to your email. Enter it to finish signing up."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        return ResponseEntity.ok(emailVerificationService.verify(req.email(), req.otp()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendCodeRequest req) {
        emailVerificationService.resendCode(req.email());
        return ResponseEntity.ok(new MessageResponse(
                "If that account still needs verifying, a new code has been sent."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.requestReset(req.username());
        return ResponseEntity.ok(new MessageResponse(
                "If an account exists for that username, a reset code has been sent to its registered email."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        String resetToken = passwordResetService.verifyOtp(req.username(), req.otp());
        return ResponseEntity.ok(new VerifyOtpResponse(resetToken));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.resetToken(), req.newPassword());
        return ResponseEntity.ok(new MessageResponse("Your password has been reset. You can now log in."));
    }
}
