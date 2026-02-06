package com.theinside.partii.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for logging out a user by revoking their refresh token.
 */
public record LogoutRequest(
    @NotBlank(message = "Refresh token cannot be blank")
    String refreshToken
) {}
