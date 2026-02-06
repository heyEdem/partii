package com.theinside.partii.service.notification;

/**
 * Service interface for sending notifications (email, SMS, push).
 * Implementations can use different providers (Resend, SendGrid, Twilio, etc.)
 */
public interface NotificationService {

    /**
     * Sends an OTP verification email to the specified recipient.
     *
     * @param to        recipient email address
     * @param otpCode   the OTP code to include in the email
     * @param userName  the user's display name for personalization
     */
    void sendOtpEmail(String to, String otpCode, String userName);

    /**
     * Sends a welcome email after successful account verification.
     *
     * @param to       recipient email address
     * @param userName the user's display name
     */
    void sendWelcomeEmail(String to, String userName);

    /**
     * Sends a password reset email with a reset link/token.
     *
     * @param to         recipient email address
     * @param resetToken the password reset token
     * @param userName   the user's display name
     */
    void sendPasswordResetEmail(String to, String resetToken, String userName);
}
