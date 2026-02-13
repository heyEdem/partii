package com.theinside.partii.service.notification;

import com.theinside.partii.config.AppProperties;
import com.theinside.partii.config.ResendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Resend-based implementation of NotificationService.
 * Active when 'resend' profile is enabled - ideal for production with custom domain.
 */
@Service
@RequiredArgsConstructor
@Slf4j
//@Profile("dev")
public class ResendNotificationService implements NotificationService {

    private final ResendProperties resendProperties;
    private final AppProperties appProperties;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Override
    @Async
    public void sendOtpEmail(String to, String otpCode, String userName) {
        String subject = "Your Partii verification code";
        String htmlContent = buildOtpEmailHtml(otpCode, userName);

        sendEmail(to, subject, htmlContent);
        log.info("OTP email sent to: {}", to);
    }

    @Override
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        String subject = "Welcome to Partii!";
        String htmlContent = buildWelcomeEmailHtml(userName);

        sendEmail(to, subject, htmlContent);
        log.info("Welcome email sent to: {}", to);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetToken, String userName) {
        String resetLink = appProperties.getPasswordResetUrl() + "?token=" + resetToken;

        String subject = "Reset your Partii password";
        String htmlContent = buildPasswordResetEmailHtml(userName, resetLink);

        sendEmail(to, subject, htmlContent);
        log.info("Password reset email sent to: {}", to);
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            RestClient restClient = RestClient.create();

            Map<String, Object> requestBody = Map.of(
                    "from", resendProperties.fromEmail(),
                    "to", toEmail,
                    "subject", subject,
                    "html", htmlContent
            );

            restClient.post()
                    .uri(RESEND_API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            // Don't throw - email sending should not block the main flow
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

                    <p style="margin: 0; color: #999; font-size: 14px;">Welcome to the Partii community!</p>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildPasswordResetEmailHtml(String userName, String resetLink) {
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

                    <a href="%s" style="display: inline-block; background: #111; color: white; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: 500; margin-bottom: 32px;">Reset Password</a>

                    <p style="margin: 0 0 24px; color: #666; font-size: 14px;">Or copy and paste this link into your browser:</p>
                    <p style="margin: 0 0 32px; word-break: break-all; color: #999; font-size: 12px;">%s</p>

                    <p style="margin: 0; color: #999; font-size: 14px;">This link expires in 1 hour. If you didn't request this, you can safely ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, resetLink, resetLink);
    }
}
