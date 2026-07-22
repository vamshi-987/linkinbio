package com.backend.libserver.auth.password.impl;

import com.backend.libserver.auth.password.EmailService;
import com.backend.libserver.auth.password.OtpPurpose;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Sends one-time-code emails through Gmail's SMTP host (JavaMailSender), off the caller's thread and
 * after its transaction commits.
 *
 * <p>Delivery must never run inline: callers issue codes from inside a transaction, so a slow send
 * would pin a database connection for the length of the SMTP connect+send timeout and exhaust the
 * pool under concurrent signups. Waiting for the commit also means a caller that rolls back after
 * issuing a code never emails one whose row no longer exists.
 *
 * <p>SMTP credentials are optional: with no app password configured ({@code spring.mail.password} /
 * {@code MAIL_PASSWORD} blank) we fall back to logging the code to the server console, so the app
 * still runs in local dev instead of failing every signup and reset. Delivery failures are logged,
 * never thrown — the request that issued the code has already returned.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final Executor mailExecutor;
    private final String from;
    private final boolean configured; // false when no app password is set — see fallback in sendOtp

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Qualifier("mailExecutor") Executor mailExecutor,
                            @Value("${spring.mail.username:}") String username,
                            @Value("${spring.mail.password:}") String password,
                            @Value("${app.mail.from:}") String fromAddress,
                            @Value("${app.mail.from-name:}") String fromName) {
        this.mailSender = mailSender;
        this.mailExecutor = mailExecutor;
        this.configured = !username.isBlank() && !password.isBlank();
        // Gmail rewrites a From it does not own to the authenticated account, so the account address
        // is the only sensible default. RFC 822 allows "Display Name <email>"; use it when a name is set.
        String address = fromAddress.isBlank() ? username : fromAddress;
        this.from = fromName.isBlank() ? address : fromName + " <" + address + ">";
    }

    @Override
    public void sendOtp(String toEmail, String otp, long expiryMinutes, OtpPurpose purpose) {
        if (!configured) {
            log.warn("SMTP is not configured (spring.mail.username/password are blank). {} code for {} is: {} "
                            + "(valid {} min). Set MAIL_USERNAME and MAIL_PASSWORD to send real emails.",
                    purpose, toEmail, otp, expiryMinutes);
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(toEmail, otp, expiryMinutes, purpose);
                }
            });
        } else {
            dispatch(toEmail, otp, expiryMinutes, purpose);
        }
    }

    /** Hands the send to the mail pool; a saturated queue degrades to logging, never to a failure. */
    private void dispatch(String toEmail, String otp, long expiryMinutes, OtpPurpose purpose) {
        try {
            mailExecutor.execute(() -> deliver(toEmail, otp, expiryMinutes, purpose));
        } catch (RejectedExecutionException e) {
            log.error("Mail queue is full; dropping {} email to {}. Code is: {} (valid {} min).",
                    purpose, toEmail, otp, expiryMinutes);
        }
    }

    private void deliver(String toEmail, String otp, long expiryMinutes, OtpPurpose purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart=true so the plain-text and HTML bodies go out as alternatives of one message,
            // letting a client that refuses HTML still show the code.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subjectFor(purpose));
            helper.setText(textBody(purpose, otp, expiryMinutes), htmlBody(purpose, otp, expiryMinutes));

            mailSender.send(message);
            log.info("{} code sent to {}", purpose, toEmail);
        } catch (Exception e) {
            // Nothing is waiting on this thread to report to, and an escaping exception would only
            // kill the pool worker; log the code so the flow remains usable while mail is fixed.
            log.error("Failed to send {} email to {}. Code is: {} (valid {} min). Error: {}",
                    purpose, toEmail, otp, expiryMinutes, e.getMessage());
        }
    }

    private String subjectFor(OtpPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> "Verify your email";
            case PASSWORD_RESET -> "Your password reset code";
        };
    }

    private String textBody(OtpPurpose purpose, String otp, long expiryMinutes) {
        String intro = switch (purpose) {
            case EMAIL_VERIFICATION -> "Welcome! Confirm your email address with this code: ";
            case PASSWORD_RESET -> "Your password reset code is: ";
        };
        return intro + otp + "\n\nIt expires in " + expiryMinutes + " minutes. " + outro(purpose);
    }

    private String htmlBody(OtpPurpose purpose, String otp, long expiryMinutes) {
        String intro = switch (purpose) {
            case EMAIL_VERIFICATION -> "Welcome! Confirm your email address with this code:";
            case PASSWORD_RESET -> "Your password reset code is:";
        };
        return """
                <div style="font-family:system-ui,Segoe UI,Roboto,sans-serif;font-size:16px;color:#24262d">
                  <p>%s</p>
                  <p style="font-size:32px;font-weight:700;letter-spacing:6px;margin:16px 0">%s</p>
                  <p style="color:#6b7280">It expires in %d minutes. %s</p>
                </div>""".formatted(intro, otp, expiryMinutes, outro(purpose));
    }

    private String outro(OtpPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> "If you didn't create an account, you can safely ignore this email.";
            case PASSWORD_RESET -> "If you didn't request this, you can safely ignore this email.";
        };
    }
}
