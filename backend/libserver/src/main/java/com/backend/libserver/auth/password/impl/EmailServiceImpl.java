package com.backend.libserver.auth.password.impl;

import com.backend.libserver.auth.password.EmailService;
import com.backend.libserver.auth.password.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Sends one-time-code emails over SMTP, off the caller's thread and after its transaction commits.
 *
 * <p>Delivery must never run inline: callers issue codes from inside a transaction, so a slow SMTP
 * host would pin a database connection for the length of the mail timeouts and exhaust the pool
 * under concurrent signups. Waiting for the commit also means a caller that rolls back after issuing
 * never emails a code whose row no longer exists.
 *
 * <p>The {@link JavaMailSender} is optional: if no SMTP host is configured (no
 * {@code spring.mail.host}), Spring does not create the bean, so we fall back to logging the code to
 * the server console. This keeps the app running in dev / misconfigured environments instead of
 * crashing when someone signs up or requests a reset.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Executor mailExecutor;
    private final String fromAddress;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider,
                            @Qualifier("mailExecutor") Executor mailExecutor,
                            @Value("${app.mail.from:no-reply@linkinbio.app}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailExecutor = mailExecutor;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendOtp(String toEmail, String otp, long expiryMinutes, OtpPurpose purpose) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP is not configured (spring.mail.host is blank). {} code for {} is: {} "
                            + "(valid {} min). Set MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD to send real emails.",
                    purpose, toEmail, otp, expiryMinutes);
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(mailSender, toEmail, otp, expiryMinutes, purpose);
                }
            });
        } else {
            dispatch(mailSender, toEmail, otp, expiryMinutes, purpose);
        }
    }

    /** Hands the send to the mail pool; a saturated queue degrades to logging, never to a failure. */
    private void dispatch(JavaMailSender mailSender, String toEmail, String otp,
                          long expiryMinutes, OtpPurpose purpose) {
        try {
            mailExecutor.execute(() -> deliver(mailSender, toEmail, otp, expiryMinutes, purpose));
        } catch (RejectedExecutionException e) {
            log.error("Mail queue is full; dropping {} email to {}. Code is: {} (valid {} min).",
                    purpose, toEmail, otp, expiryMinutes);
        }
    }

    private void deliver(JavaMailSender mailSender, String toEmail, String otp,
                         long expiryMinutes, OtpPurpose purpose) {
        String subject = subjectFor(purpose);
        String body = bodyFor(purpose, otp, expiryMinutes);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
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

    private String bodyFor(OtpPurpose purpose, String otp, long expiryMinutes) {
        String intro = switch (purpose) {
            case EMAIL_VERIFICATION -> "Welcome! Confirm your email address with this code: ";
            case PASSWORD_RESET -> "Your password reset code is: ";
        };
        String outro = switch (purpose) {
            case EMAIL_VERIFICATION -> "If you didn't create an account, you can safely ignore this email.";
            case PASSWORD_RESET -> "If you didn't request this, you can safely ignore this email.";
        };
        return intro + otp + "\n\nIt expires in " + expiryMinutes + " minutes. " + outro;
    }
}
