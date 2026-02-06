package com.theinside.partii.dto;

//import com.theinside.partii.utils.validators.ValidPhoneNumber;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO for partial profile updates.
 * All fields are optional - only non-null fields will be updated.
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
        String displayName,

        @Size(max = 150, message = "Legal name must not exceed 150 characters")
        String legalName,

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String generalLocation,

        @Size(max = 300, message = "Address must not exceed 300 characters")
        String primaryAddress,

//        @ValidPhoneNumber
        String phoneNumber,

        @Past(message = "Date of birth must be in the past")
        LocalDate dob,

        String profilePictureUrl
) {
}
