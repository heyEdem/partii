package com.theinside.partii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "partii.app")
public record AppProperties(
        String frontendUrl,
        String verificationPath,
        String passwordResetPath
) {
    public String getVerificationUrl() {
        return frontendUrl + verificationPath;
    }

    public String getPasswordResetUrl() {
        return frontendUrl + passwordResetPath;
    }
}
