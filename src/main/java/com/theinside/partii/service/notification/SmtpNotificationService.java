package com.theinside.partii.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SMTP-based implementation of NotificationService using Spring's JavaMailSender.
 * Active when 'smtp' profile is enabled - ideal for development without a custom domain.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile("prod")
public class SmtpNotificationService implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${partii.mail.from-name:Partii}")
    private String fromName;

    @Override
    @Async
    public void sendOtpEmail(String to, String otpCode, String userName) {
        String subject = "Your Partii verification code";
        String html = buildOtpEmailHtml(otpCode, userName);

        sendEmailWithRetry(to, subject, html, "OTP");
    }

    @Override
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        String subject = "Welcome to Partii!";
        String html = buildWelcomeEmailHtml(userName);

        sendEmailWithRetry(to, subject, html, "Welcome");
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetToken, String userName) {
        String subject = "Reset your Partii password";
        String html = buildPasswordResetEmailHtml(resetToken, userName);

        sendEmailWithRetry(to, subject, html, "PasswordReset");
    }

    public void sendEmailWithRetry(String to, String subject, String html, String emailType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("{} email sent to {}", emailType, to);

        } catch (MessagingException | MailException e) {
            log.warn("Attempt to send {} email to {} failed: {}", emailType, to, e.getMessage());
            throw new EmailSendException("Failed to send " + emailType + " email to " + to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending {} email to {}: {}", emailType, to, e.getMessage());
            throw new EmailSendException("Failed to send " + emailType + " email to " + to, e);
        }
    }

    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private String buildOtpEmailHtml(String otpCode, String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 40px 20px; background-color: #f5f5f5;">
                <div style="max-width: 480px; margin: 0 auto; background: white; border-radius: 12px; padding: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
                    <h1 style="margin: 0 0 8px; font-size: 24px; color: #111;">Verify your email</h1>
                    <p style="margin: 0 0 32px; color: #666; font-size: 16px;">Hi %s, use this code to verify your Partii account:</p>

                    <div style="background: #f8f9fa; border-radius: 8px; padding: 24px; text-align: center; margin-bottom: 32px;">
                        <span style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #111;">%s</span>
                    </div>

                    <p style="margin: 0; color: #999; font-size: 14px;">This code expires in 10 minutes. If you didn't request this, you can safely ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, otpCode);
    }

    private String buildWelcomeEmailHtml(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 40px 20px; background-color: #f5f5f5;">
                <div style="max-width: 480px; margin: 0 auto; background: white; border-radius: 12px; padding: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
                    <h1 style="margin: 0 0 8px; font-size: 24px; color: #111;">Welcome to Partii!</h1>
                    <p style="margin: 0 0 24px; color: #666; font-size: 16px;">Hi %s, your account is now verified and ready to go.</p>

                    <p style="margin: 0 0 24px; color: #666; font-size: 16px;">You can now:</p>
                    <ul style="margin: 0 0 32px; padding-left: 20px; color: #666;">
                        <li style="margin-bottom: 8px;">Discover and join events near you</li>
                        <li style="margin-bottom: 8px;">Create and organize your own events</li>
                        <li style="margin-bottom: 8px;">Connect with other attendees</li>
                    </ul>

                    <a href="https://partii.com" style="display: inline-block; background: #111; color: white; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: 500;">Get Started</a>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildPasswordResetEmailHtml(String resetToken, String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 40px 20px; background-color: #f5f5f5;">
                <div style="max-width: 480px; margin: 0 auto; background: white; border-radius: 12px; padding: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
                    <h1 style="margin: 0 0 8px; font-size: 24px; color: #111;">Reset your password</h1>
                    <p style="margin: 0 0 32px; color: #666; font-size: 16px;">Hi %s, we received a request to reset your password. Click the button below to choose a new one:</p>

                    <a href="https://partii.com/reset-password?token=%s" style="display: inline-block; background: #111; color: white; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: 500; margin-bottom: 32px;">Reset Password</a>

                    <p style="margin: 24px 0 0; color: #999; font-size: 14px;">This link expires in 1 hour. If you didn't request this, you can safely ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, resetToken);
    }
}
