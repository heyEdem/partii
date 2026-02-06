package com.theinside.partii.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of NotificationService for development and testing.
 * Logs messages instead of sending actual emails.
 */
@Service
@Slf4j
@Profile("!prod")
public class NoOpNotificationService implements NotificationService {

    @Override
    public void sendOtpEmail(String to, String otpCode, String userName) {
        log.info("[NO-OP] Would send OTP email to: {}, OTP: {}, User: {}", to, otpCode, userName);
    }

    @Override
    public void sendWelcomeEmail(String to, String userName) {
        log.info("[NO-OP] Would send welcome email to: {}, User: {}", to, userName);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetToken, String userName) {
        log.info("[NO-OP] Would send password reset email to: {}, Token: {}, User: {}", to, resetToken, userName);
    }
}
