package com.theinside.partii.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO for completing profile after OAuth signup.
 * Used when essential information wasn't collected during OAuth flow.
 */
public record CompleteProfileRequest(
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dob,

        @Size(max = 150, message = "Legal name must not exceed 150 characters")
        String legalName,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String generalLocation
) {}