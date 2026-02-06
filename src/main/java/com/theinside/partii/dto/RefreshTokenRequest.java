package com.theinside.partii.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing an access token using a refresh token.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token cannot be blank")
    String refreshToken
) {}
