package com.theinside.partii.dto;

import java.time.Instant;

public record AuthResponse(
        Long userId,
        String email,
        String displayName,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {}
