package com.theinside.partii.dto;

import com.theinside.partii.entity.AccountStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO for returning user profile information.
 * Excludes sensitive fields like password and internal flags.
 */
public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        String legalName,
        String bio,
        String generalLocation,
        String primaryAddress,
        String phoneNumber,
        LocalDate dob,
        int age,
        AccountStatus accountStatus,
        boolean isVerified,
        int totalRatings,
        int averageRating,
        int eventsAttended,
        int eventsOrganized,
        int activeEventsCount,
        String profilePictureUrl,
        Instant createdAt
) {
}
