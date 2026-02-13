package com.theinside.partii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resend")
public record ResendProperties(
        String apiKey,
        String fromEmail
) {}
